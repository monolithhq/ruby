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
 * playable by the time it reaches this class.
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

        val stage2Ordered = analyzed.sortedByDescending { it.sourceAddonHealthy }

        return stage2Ordered
            .groupConsecutiveBy { it.sourceAddonHealthy }
            .flatMap { group -> group.sortedWith(heuristicComparator) }
            .map { it.stream }
    }

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

data class EligibleStream(
    val stream: StreamObject,
    val addonId: Long,
    val addonHealthy: Boolean,
)
