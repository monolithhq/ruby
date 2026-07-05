package com.ruby.stream.core.streams

import com.ruby.stream.core.addons.AddonExecutor
import com.ruby.stream.core.addons.model.MetaObject
import com.ruby.stream.core.addons.model.StreamObject
import com.ruby.stream.data.database.dao.InstalledAddonDao
import com.ruby.stream.data.database.entity.AddonHealth
import com.ruby.stream.data.database.entity.InstalledAddonEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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
 * follow getRankedStreams()'s fan-out-and-merge shape -- see SOT
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
 */
@Singleton
class AddonRepository @Inject constructor(
    private val installedAddonDao: InstalledAddonDao,
    private val addonExecutor: AddonExecutor,
    private val capabilityEvaluator: StreamCapabilityEvaluator,
    private val streamRanker: StreamRanker,
    private val repositoryScope: CoroutineScope,
) {

    /**
     * Returns List<RankedStreamCandidate>, not List<StreamObject>
     * (fixed Session 5, PASS 5 scoping) -- see StreamRanker.rank() for
     * the full reasoning. Stream Selection is the only current
     * consumer and needs sourceAddonHealthy/labelAnalysis to render a
     * meaningful choice; a plain StreamObject list would silently
     * discard exactly the information that screen exists to show.
     */
    suspend fun getRankedStreams(type: String, id: String): List<RankedStreamCandidate> = coroutineScope {
        val installedAddons = installedAddonDao.getAllOrderedById()
        val attemptedAddons = installedAddons.filter { it.enabled }

        val perAddonResults = attemptedAddons.map { addon ->
            async {
                val (observedHealth, streams) = fetchAndProbe(addon, type, id)
                Triple(addon, observedHealth, streams)
            }
        }.map { it.await() }

        persistHealthUpdates(perAddonResults.map { (addon, health, _) -> addon to health })

        val eligible = perAddonResults.flatMap { (addon, observedHealth, streams) ->
            streams.mapNotNull { stream ->
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
        }

        streamRanker.rank(eligible)
    }

    /**
     * First-success-wins metadata lookup, in stable add-on order. See
     * class doc for why this is sequential/first-match rather than
     * fan-out-and-merge like getRankedStreams(). Every add-on actually
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
     * needs (addon + observed health) -- getRankedStreams() and
     * getMeta() both project their own richer per-addon result down to
     * this shape at the call site, rather than this function accepting
     * an unused third field just to satisfy one caller's tuple shape.
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
