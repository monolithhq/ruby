package com.ruby.stream.core.streams

import com.ruby.stream.core.addons.AddonExecutor
import com.ruby.stream.core.addons.model.MetaObject
import com.ruby.stream.core.addons.model.MetaPreviewObject
import com.ruby.stream.core.addons.model.StreamObject
import com.ruby.stream.data.database.dao.InstalledAddonDao
import com.ruby.stream.data.database.dao.InstalledCatalogDao
import com.ruby.stream.data.database.entity.AddonHealth
import com.ruby.stream.data.database.entity.InstalledAddonEntity
import com.ruby.stream.data.database.entity.InstalledCatalogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.channelFlow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PASS 3 — the single point of contact the rest of Ruby uses for
 * "give me streams for this content." Coordinates every installed
 * add-on's AddonExecutor call, merges results, and hands them through
 * the eligibility -> rank pipeline.
 *
 * Locked architecture decision (see SOT "Repository architecture"):
 * ONE AddonRepository, not one repository per add-on.
 *
 * Stable ordering (see SOT "Stable add-on order"): add-ons are
 * iterated by InstalledAddonEntity.id ascending, via
 * InstalledAddonDao.getAllOrderedById -- added this pass.
 *
 * Health model (revised this pass -- see SOT "Hybrid health model" /
 * "Unify health vocabulary"): this repository does NOT call
 * AddonHealthChecker before fetching streams. Instead:
 *   1. Persisted InstalledAddonEntity.health is read as the initial
 *      state, but the only thing that excludes an add-on from being
 *      attempted this request is enabled=false. Even an UNREACHABLE
 *      add-on is retried, since a stale prior failure shouldn't
 *      permanently exclude it without a fresh attempt.
 *   2. The actual stream fetch attempt IS the live health signal for
 *      this request.
 *   3. That outcome is used immediately for this request, AND written
 *      back to Room asynchronously (does not block the caller).
 *
 * getMeta() (added Session 5, PASS 5 scoping) deliberately does NOT
 * follow discoverStreams()'s fan-out-and-merge shape -- see SOT
 * AD-00P for the full reasoning. Metadata has one true answer per
 * title (one cast list, one description), unlike streams where every
 * add-on's result is an independently valid option worth showing.
 * Field-level merging across sources was considered and rejected:
 * there is no principled way to decide which add-on's cast list or
 * description is "more correct" when they conflict, and a
 * completeness-scoring heuristic would just be an invented guess
 * dressed up as logic. Instead: try installed, enabled add-ons in the
 * same stable order streams use, sequentially, and return the first
 * non-null result. Sequential and short-circuiting on purpose --
 * unlike streams, only one answer is needed here, so there is no
 * reason to query every add-on just to compare them.
 *
 * search() (added Session 5, during Search scoping) is a THIRD merge
 * policy, distinct from both of the above -- see SOT AD-00N. Unlike
 * getMeta(), multiple add-ons returning different-but-valid results
 * for the same query is normal and all of them should surface. Unlike
 * discoverStreams(), there is no ranking: search is discovery, not
 * playback, and there is no protocol-grounded notion of one result
 * being "better" than another. Fans out to every searchable catalog
 * (per InstalledCatalogDao, NOT a live manifest fetch -- see
 * InstalledCatalogEntity's doc for why manifest capability data is
 * install-time metadata, not runtime data) across every enabled
 * add-on, merges, and dedupes by (type, id) -- the only identity
 * Stremio's protocol actually grounds. First occurrence wins, walking
 * add-ons in the same stable order as everything else in this class.
 * Deliberately never does fuzzy title/poster matching for identity --
 * that is a heuristic system with no protocol grounding, same
 * category of thing already rejected for stream ranking's Stage 3.
 */
@Singleton
class AddonRepository @Inject constructor(
    private val installedAddonDao: InstalledAddonDao,
    private val installedCatalogDao: InstalledCatalogDao,
    private val addonExecutor: AddonExecutor,
    private val capabilityEvaluator: StreamCapabilityEvaluator,
    private val streamRanker: StreamRanker,
    private val repositoryScope: CoroutineScope,
) {

    /**
     * Progressive stream discovery (AD-012, replacing the prior
     * suspend fun getRankedStreams(): List<RankedStreamCandidate>).
     * Renamed rather than overloaded -- the old contract was "wait for
     * everything, then return once"; this contract is "emit as each
     * provider finishes," which is different enough to warrant a new
     * name rather than surprising callers written against the old
     * blocking semantics.
     *
     * Deterministic emission contract (locked invariant): emits
     * exactly ONE initial update (candidates = emptyList(),
     * completedAttempts = 0) before any provider coroutine launches,
     * then exactly ONE further update per attempted provider on that
     * provider's completion -- regardless of whether it returned
     * streams, returned an empty list, or failed. A discovery session
     * therefore always emits exactly (attemptedAddons.size + 1)
     * updates, in total. completedAttempts counts providers whose WORK
     * has finished, not providers that found streams -- an empty or
     * failed provider still advances it, or the UI's progress counter
     * would stall short of total whenever any provider comes back
     * empty/failed. No separate `finished: Boolean` -- completion is
     * (completedAttempts == totalProviders).
     *
     * Health updates remain exactly per-provider, on that provider's
     * own completion, unconditionally -- this preserves AD-00Q exactly
     * (a null/failed result is a real health signal the moment it's
     * known, never batched to the end of the whole discovery session).
     *
     * StreamRanker.rank() is unchanged and called fresh on the FULL
     * accumulated candidate list after every single completion, not
     * just the newest arrival. Because rank() is a pure function of
     * its input (verified: no internal mutable state), this is what
     * makes reordering correct-and-stable "for free": existing
     * candidates only change position relative to each other if
     * rank()'s own algorithm says so, never merely because of arrival
     * timing/order. A newly-completed provider's candidates can land
     * anywhere in the list, including above earlier arrivals -- this
     * is the intended, expected behavior of a live-ranked list, not
     * visual jank to be avoided.
     *
     * Every emitted StreamDiscoveryUpdate is an immutable snapshot
     * (allEligible.toList() at emission time) -- no emission may ever
     * expose the repository's own live mutable accumulator to a
     * collector.
     *
     * Concurrency: channelFlow is required (not a plain flow{} builder)
     * because multiple concurrent per-provider coroutines each need to
     * send() independently as they finish -- a sequential flow{}
     * builder cannot support multiple concurrent producers. The mutex
     * guards ONLY the repository-local allEligible/completed
     * accumulator -- constructing the next StreamDiscoveryUpdate is the
     * entire critical section. persistHealthUpdates() and send() both
     * stay OUTSIDE the lock: persistHealthUpdates() touches unrelated
     * shared state (installedAddonDao, not allEligible/completed) and
     * has no need of this mutex at all; send() is itself a suspending
     * call, and holding a mutex across a suspension point would let a
     * slow/backpressured collector block every other provider's
     * completion from updating the shared accumulator. channelFlow's
     * ProducerScope.send() is itself safe to call concurrently from
     * multiple child coroutines without additional synchronization --
     * this is precisely the use case channelFlow exists for.
     */
    fun discoverStreams(type: String, id: String): Flow<StreamDiscoveryUpdate> = channelFlow {
        val installedAddons = installedAddonDao.getAllOrderedById()
        val attemptedAddons = installedAddons.filter { it.enabled }
        val total = attemptedAddons.size

        val allEligible = mutableListOf<EligibleStream>()
        var completed = 0
        val mutex = Mutex()

        send(
            StreamDiscoveryUpdate(
                candidates = emptyList(),
                completedAttempts = 0,
                totalProviders = total,
            )
        )

        coroutineScope {
            attemptedAddons.forEach { addon ->
                launch {
                    val (observedHealth, streams) = fetchAndProbe(addon, type, id)

                    persistHealthUpdates(listOf(addon to observedHealth))

                    val eligible = streams.mapNotNull { stream ->
                        val ineligible = capabilityEvaluator.evaluate(
                            stream = stream,
                            addonEnabled = addon.enabled,
                            addonHealth = observedHealth,
                        )
                        if (ineligible != null) return@mapNotNull null
                        EligibleStream(
                            stream = stream,
                            addonId = addon.id,
                            addonHealthy = observedHealth == AddonHealth.HEALTHY,
                        )
                    }

                    val update = mutex.withLock {
                        allEligible.addAll(eligible)
                        completed++
                        StreamDiscoveryUpdate(
                            candidates = streamRanker.rank(allEligible.toList()),
                            completedAttempts = completed,
                            totalProviders = total,
                        )
                    }
                    send(update)
                }
            }
        }
    }

    /**
     * First-success-wins metadata lookup, in stable add-on order. See
     * class doc for why this is sequential/first-match rather than
     * fan-out-and-merge like discoverStreams(). Every add-on actually
     * attempted along the way (including ones tried before a winner is
     * found) gets its health observation written back -- a null result
     * is a real health signal even if a later add-on succeeds.
     */
    suspend fun getMeta(type: String, id: String): MetaObject? {
        val installedAddons = installedAddonDao.getAllOrderedById()
        val attemptedAddons = installedAddons.filter { it.enabled }

        val attempted = mutableListOf<Pair<InstalledAddonEntity, AddonHealth>>()
        var result: MetaObject? = null

        for (addon in attemptedAddons) {
            val meta = addonExecutor.getMeta(addon.manifestUrl, type, id)
            val observedHealth = if (meta == null) AddonHealth.UNREACHABLE else AddonHealth.HEALTHY
            attempted.add(addon to observedHealth)
            if (meta != null) {
                result = meta
                break
            }
        }

        persistHealthUpdates(attempted)

        return result
    }

    /**
     * query is never blank here -- the caller (SearchViewModel, PASS 6)
     * is responsible for not invoking this on an empty query at all
     * (see SearchUiState.Idle). type is optional: null searches every
     * type a given catalog declares; a specific type narrows to
     * catalogs of that type only.
     *
     * Individual add-on/catalog failures are swallowed, not
     * propagated -- one unreachable add-on should not fail an entire
     * search when others may still answer. This mirrors
     * discoverStreams()'s existing graceful-degradation posture, not
     * a new policy invented for this method.
     */
    suspend fun search(query: String, type: String? = null): List<MetaPreviewObject> = coroutineScope {
        val installedAddons = installedAddonDao.getAllOrderedById()
        val attemptedAddons = installedAddons.filter { it.enabled }

        val searchTasks = attemptedAddons.flatMap { addon ->
            val searchableCatalogs = getSearchableCatalogsFor(addon, type)
            searchableCatalogs.map { catalog ->
                async {
                    runCatching {
                        addonExecutor.getCatalog(
                            baseUrl = addon.manifestUrl,
                            type = catalog.type,
                            catalogId = catalog.catalogId,
                            extraPath = "search=$query",
                        )
                    }.getOrNull()
                }
            }
        }

        searchTasks
            .map { it.await() }
            .filterNotNull()
            .flatten()
            .distinctBy { it.type to it.id }
    }

    /**
     * Consults InstalledCatalogDao only -- never re-fetches or
     * re-parses a manifest here. If type is null, every searchable
     * catalog for this add-on is queried regardless of type; if a
     * type is given, only that type's searchable catalogs are used.
     */
    private suspend fun getSearchableCatalogsFor(
        addon: InstalledAddonEntity,
        type: String?,
    ): List<InstalledCatalogEntity> {
        val all = installedCatalogDao.getAllForAddon(addon.id)
        return all.filter { it.supportsSearch && (type == null || it.type == type) }
    }

    /**
     * Attempts the actual stream fetch and derives this request's live
     * health observation from its outcome. AddonExecutor.getStreams
     * returns null only on a whole-request failure (see PASS 2B); an
     * empty list means the add-on responded successfully but had
     * nothing for this title -- HEALTHY, not UNREACHABLE.
     */
    private suspend fun fetchAndProbe(
        addon: InstalledAddonEntity,
        type: String,
        id: String,
    ): Pair<AddonHealth, List<StreamObject>> {
        val streams = addonExecutor.getStreams(addon.manifestUrl, type, id)
        return if (streams == null) {
            AddonHealth.UNREACHABLE to emptyList()
        } else {
            AddonHealth.HEALTHY to streams
        }
    }

    /**
     * Fire-and-forget write-back using an application-lifetime
     * repositoryScope (DI binding deferred to PASS 8) rather than a
     * child of this function's own coroutineScope, which would still
     * be joined before the caller returns. Takes only what it actually
     * needs (addon + observed health) -- discoverStreams() and
     * getMeta() both project their own richer per-addon result down to
     * this shape at the call site, rather than this function accepting
     * an unused third field just to satisfy one caller's tuple shape.
     * search() does NOT call this -- add-on health is not touched by
     * search failures, since a missing/failed search result is not a
     * reachability signal the way a failed getStreams()/getMeta() call
     * is (an add-on can be perfectly healthy and simply have no
     * searchable catalogs, or no matches for a query).
     */
    private fun persistHealthUpdates(
        results: List<Pair<InstalledAddonEntity, AddonHealth>>,
    ) {
        repositoryScope.launch(Dispatchers.IO) {
            results.forEach { (addon, observedHealth) ->
                if (observedHealth != addon.health) {
                    installedAddonDao.update(addon.copy(health = observedHealth))
                }
            }
        }
    }
}
