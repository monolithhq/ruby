package com.ruby.stream.core.streams

import com.ruby.stream.core.addons.AddonExecutor
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
 */
@Singleton
class AddonRepository @Inject constructor(
    private val installedAddonDao: InstalledAddonDao,
    private val addonExecutor: AddonExecutor,
    private val capabilityEvaluator: StreamCapabilityEvaluator,
    private val streamRanker: StreamRanker,
    private val repositoryScope: CoroutineScope,
) {

    suspend fun getRankedStreams(type: String, id: String): List<StreamObject> = coroutineScope {
        val installedAddons = installedAddonDao.getAllOrderedById()
        val attemptedAddons = installedAddons.filter { it.enabled }

        val perAddonResults = attemptedAddons.map { addon ->
            async {
                val (observedHealth, streams) = fetchAndProbe(addon, type, id)
                Triple(addon, observedHealth, streams)
            }
        }.map { it.await() }

        persistHealthUpdates(perAddonResults)

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
     * be joined before getRankedStreams returns.
     */
    private fun persistHealthUpdates(
        results: List<Triple<InstalledAddonEntity, AddonHealth, List<StreamObject>>>,
    ) {
        repositoryScope.launch(Dispatchers.IO) {
            results.forEach { (addon, observedHealth, _) ->
                if (observedHealth != addon.health) {
                    installedAddonDao.update(addon.copy(health = observedHealth))
                }
            }
        }
    }
}
