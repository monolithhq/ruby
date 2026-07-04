package com.ruby.stream.core.streams

import com.ruby.stream.core.addons.model.StreamObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PASS 3, Stage 2 + Stage 3. Input to this class is ALREADY eligible
 * (StreamCapabilityEvaluator has run) -- StreamRanker never re-checks
 * eligibility and never excludes anything; it only orders.
 *
 * Stage 2 (authoritative, deterministic): sort by add-on health, then
 * by stable add-on order. No playback-method comparison here -- see
 * SOT "Eligibility precedes ranking": every candidate is already
 * playable by the time it reaches this class, so there is nothing left
 * to prefer on that axis.
 *
 * Stage 3 (heuristic, advisory): within each Stage 2 group (same
 * health tier), reorder using StreamLabelAnalyzer's inferred quality/
 * HDR/codec signals. A Stage 3 reordering can never move a candidate
 * out of its Stage 2 group -- see SOT "Authoritative before heuristic".
 */
@Singleton
class StreamRanker @Inject constructor(
    private val labelAnalyzer: StreamLabelAnalyzer,
) {

    fun rank(candidates: List<EligibleStream>): List<StreamObject> {
        val analyzed = candidates.map { candidate ->
            RankedStreamCandidate(
                stream = candidate.stream,
                sourceAddonId = candidate.addonId,
                sourceAddonHealthy = candidate.addonHealthy,
                labelAnalysis = labelAnalyzer.analyze(candidate.stream),
            )
        }

        // Stage 2: group by health (healthy first), preserve incoming
        // (already addon-order-stable) order as the tie-breaker within
        // each health group -- this sortedByDescending is a STABLE
        // sort, so ties keep their original relative order.
        val stage2Ordered = analyzed.sortedByDescending { it.sourceAddonHealthy }

        // Stage 3: reorder only within contiguous same-health runs, so
        // a heuristic score can never cross a Stage 2 boundary.
        return stage2Ordered
            .groupConsecutiveBy { it.sourceAddonHealthy }
            .flatMap { group -> group.sortedWith(heuristicComparator) }
            .map { it.stream }
    }

    /**
     * Higher inferred quality first. Ties (including everything
     * UNKNOWN, i.e. no usable label at all) fall through to the
     * incoming stable order via sortedWith's stability -- an analysis
     * failure never demotes a stream below where Stage 2 already
     * placed it, it just doesn't get a boost.
     */
    private val heuristicComparator = compareByDescending<RankedStreamCandidate> {
        resolutionWeight(it.labelAnalysis)
    }.thenByDescending {
        hdrWeight(it.labelAnalysis)
    }

    private fun resolutionWeight(analysis: StreamLabelAnalysis): Int {
        if (analysis.resolutionConfidence == InferenceConfidence.NONE) return 0
        return when (analysis.resolution) {
            InferredResolution.UHD_2160P -> 6
            InferredResolution.QHD_1440P -> 5
            InferredResolution.FHD_1080P -> 4
            InferredResolution.HD_720P -> 3
            InferredResolution.SD_576P -> 2
            InferredResolution.SD_480P -> 2
            InferredResolution.SD_360P -> 1
            InferredResolution.SD_GENERIC -> 1
            InferredResolution.UNKNOWN -> 0
        }
    }

    private fun hdrWeight(analysis: StreamLabelAnalysis): Int {
        if (analysis.hdrConfidence == InferenceConfidence.NONE) return 0
        return when (analysis.hdr) {
            InferredHdr.DOLBY_VISION -> 3
            InferredHdr.HDR10_PLUS -> 3
            InferredHdr.HDR10 -> 2
            InferredHdr.HLG -> 1
            InferredHdr.NONE_DETECTED -> 0
            InferredHdr.UNKNOWN -> 0
        }
    }

    /**
     * Splits a list into contiguous runs of equal key, preserving
     * order. Used so Stage 3 only ever reorders within one Stage 2
     * health-tier run, never across the boundary between two runs.
     */
    private fun <T, K> List<T>.groupConsecutiveBy(key: (T) -> K): List<List<T>> {
        if (isEmpty()) return emptyList()
        val groups = mutableListOf<MutableList<T>>()
        var currentKey: K? = null
        for (item in this) {
            val itemKey = key(item)
            if (groups.isEmpty() || itemKey != currentKey) {
                groups.add(mutableListOf(item))
                currentKey = itemKey
            } else {
                groups.last().add(item)
            }
        }
        return groups
    }
}

/**
 * A stream already confirmed eligible by StreamCapabilityEvaluator,
 * carrying just enough addon context for Stage 2's ordering. Built by
 * AddonRepository, consumed by StreamRanker.
 */
data class EligibleStream(
    val stream: StreamObject,
    val addonId: Long,
    val addonHealthy: Boolean,
)
