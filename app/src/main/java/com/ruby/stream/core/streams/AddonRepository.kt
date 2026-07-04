package com.ruby.stream.core.streams

import com.ruby.stream.core.addons.AddonExecutor
import com.ruby.stream.core.addons.AddonHealthChecker
import com.ruby.stream.core.addons.AddonHealthState
import com.ruby.stream.core.addons.model.StreamObject
import com.ruby.stream.data.database.dao.InstalledAddonDao
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PASS 3 — the single point of contact the rest of Ruby uses for
 * "give me streams for this content." Coordinates every installed
 * add-on's AddonExecutor call, merges results, and hands them through
 * the eligibility -> rank pipeline.
 *
 * Locked architecture decision (see SOT "Repository architecture"):
 * ONE AddonRepository, not one repository per add-on. Individual
 * add-ons are represented as rows in InstalledAddonDao + calls through
 * the shared AddonExecutor/AddonClient -- there is no per-add-on
 * repository class (e.g. no TorrentioRepository). The rest of Ruby is
 * completely unaware of how many add-ons are installed.
 *
 * Stable ordering (see SOT "Stable add-on order"): add-ons are
 * iterated by InstalledAddonEntity.id ascending (Room's own
 * autoincrement insertion order), NOT by network response arrival
 * order, which is nondeterministic under parallel fetch. This is a
 * deliberate substitute for a dedicated position/installOrder column,
 * which does not exist in PASS 0B's schema and was not added
 * speculatively for this pass -- see SOT Deferred Decisions.
 */
@Singleton
class AddonRepository @Inject constructor(
    private val installedAddonDao: InstalledAddonDao,
    private val addonExecutor: AddonExecutor,
    private val addonHealthChecker: AddonHealthChecker,
    private val capabilityEvaluator: StreamCapabilityEvaluator,
    private val streamRanker: StreamRanker,
) {

    /**
     * Fetches streams for a piece of content from every installed
     * add-on in parallel, filters to only what Ruby can actually play
     * right now, and returns them in final display order.
     *
     * type/id follow the protocol's own request shape (PASS 2A) --
     * id is the Video ID, colon-joined for series episodes
     * ("{metaId}:{season}:{episode}"), same as a bare content id for
     * movies.
     */
    suspend fun getRankedStreams(type: String, id: String): List<StreamObject> = coroutineScope {
        val installedAddons = installedAddonDao.getAllOrderedById()

        val perAddonResults = installedAddons.map { addon ->
            async {
                val health = addonHealthChecker.checkHealth(
                    manifestUrl = addon.manifestUrl,
                    userDisabled = !addon.enabled,
                ).state

                val rawStreams = if (health == AddonHealthState.ENABLED || health == AddonHealthState.TIMEOUT) {
                    addonExecutor.getStreams(addon.manifestUrl, type, id).orEmpty()
                } else {
                    emptyList()
                }

                addon.id to (health to rawStreams)
            }
        }.map { it.await() }

        // Ordering here follows installedAddons' own order (by id
        // ascending, from the DAO query below), NOT the order the
        // parallel awaits happen to complete in -- .map above preserves
        // list index order regardless of which coroutine finishes first.
        val eligible = perAddonResults.flatMap { (addonId, healthAndStreams) ->
            val (health, streams) = healthAndStreams
            val addon = installedAddons.first { it.id == addonId }
            streams.mapNotNull { stream ->
                val ineligible = capabilityEvaluator.evaluate(
                    stream = stream,
                    addonEnabled = addon.enabled,
                    addonHealth = health,
                )
                if (ineligible != null) return@mapNotNull null
                EligibleStream(
                    stream = stream,
                    addonId = addonId,
                    addonHealthy = health == AddonHealthState.ENABLED,
                )
            }
        }

        streamRanker.rank(eligible)
    }
}
