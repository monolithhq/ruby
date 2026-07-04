package com.ruby.stream.core.streams

import com.ruby.stream.core.addons.model.StreamObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PASS 3, Stage 3 input. Parses free-text stream labels (name/title/
 * description/filename) for advisory quality/HDR/codec signals.
 *
 * This is explicitly heuristic, never authoritative -- see SOT
 * "Authoritative before heuristic". No parsed value here may affect
 * eligibility (StreamCapabilityEvaluator already ran) or override
 * Stage 2's add-on-health-based ordering. It may only reorder within
 * a Stage 2 group.
 *
 * Modeled as an interface so StreamRanker never depends on the
 * specific parsing strategy -- matches the SOT's existing "replaceable
 * heuristic layer" principle, even with only one implementation today.
 */
interface StreamLabelAnalyzer {
    fun analyze(stream: StreamObject): StreamLabelAnalysis
}

/**
 * Keyword/regex-based implementation. Vocabulary locked during PASS 3
 * scoping -- see SOT for the full rationale. Rules:
 *   - Case-insensitive matching.
 *   - Punctuation normalized before matching (so "HDR10+" and
 *     "HDR10 Plus" both match the same pattern).
 *   - No value is ever inferred from another (e.g. HEVC never implies
 *     HDR) -- each attribute is detected independently or left
 *     UNKNOWN/NONE_DETECTED.
 *   - Vocabulary is intentionally seeded now rather than deferred;
 *     extend the pattern lists below as real add-on output is
 *     observed -- doing so never changes this class's public API.
 */
@Singleton
class RegexStreamLabelAnalyzer @Inject constructor() : StreamLabelAnalyzer {

    override fun analyze(stream: StreamObject): StreamLabelAnalysis {
        val haystack = listOfNotNull(stream.name, stream.title, stream.description, stream.behaviorHints?.filename)
            .joinToString(separator = " ")
            .let(::normalize)

        val (resolution, resolutionConfidence) = detectResolution(haystack)
        val (hdr, hdrConfidence) = detectHdr(haystack)
        val (codec, codecConfidence) = detectCodec(haystack)

        return StreamLabelAnalysis(
            resolution = resolution,
            resolutionConfidence = resolutionConfidence,
            hdr = hdr,
            hdrConfidence = hdrConfidence,
            codec = codec,
            codecConfidence = codecConfidence,
        )
    }

    private fun normalize(raw: String): String {
        return raw
            .uppercase()
            .replace("HDR10+", "HDR10PLUS")
            .replace("HDR10 PLUS", "HDR10PLUS")
            .replace("H.265", "H265")
            .replace("H.264", "H264")
            .replace(Regex("[._\\-]"), " ")
    }

    private fun detectResolution(text: String): Pair<InferredResolution, InferenceConfidence> {
        return when {
            Regex("\\b(2160P|4K|UHD)\\b").containsMatchIn(text) ->
                InferredResolution.UHD_2160P to InferenceConfidence.HIGH
            Regex("\\b1440P\\b").containsMatchIn(text) ->
                InferredResolution.QHD_1440P to InferenceConfidence.HIGH
            Regex("\\b1080P\\b").containsMatchIn(text) ->
                InferredResolution.FHD_1080P to InferenceConfidence.HIGH
            Regex("\\b720P\\b").containsMatchIn(text) ->
                InferredResolution.HD_720P to InferenceConfidence.HIGH
            Regex("\\b576P\\b").containsMatchIn(text) ->
                InferredResolution.SD_576P to InferenceConfidence.MEDIUM
            Regex("\\b480P\\b").containsMatchIn(text) ->
                InferredResolution.SD_480P to InferenceConfidence.MEDIUM
            Regex("\\b360P\\b").containsMatchIn(text) ->
                InferredResolution.SD_360P to InferenceConfidence.MEDIUM
            Regex("\\bSD\\b").containsMatchIn(text) ->
                InferredResolution.SD_GENERIC to InferenceConfidence.LOW
            else ->
                InferredResolution.UNKNOWN to InferenceConfidence.NONE
        }
    }

    private fun detectHdr(text: String): Pair<InferredHdr, InferenceConfidence> {
        return when {
            Regex("\\b(DOLBY VISION|DV)\\b").containsMatchIn(text) ->
                InferredHdr.DOLBY_VISION to InferenceConfidence.HIGH
            Regex("\\bHDR10PLUS\\b").containsMatchIn(text) ->
                InferredHdr.HDR10_PLUS to InferenceConfidence.HIGH
            Regex("\\bHDR10\\b").containsMatchIn(text) ->
                InferredHdr.HDR10 to InferenceConfidence.HIGH
            Regex("\\bHLG\\b").containsMatchIn(text) ->
                InferredHdr.HLG to InferenceConfidence.MEDIUM
            Regex("\\bHDR\\b").containsMatchIn(text) ->
                InferredHdr.HDR10 to InferenceConfidence.LOW
            else ->
                InferredHdr.UNKNOWN to InferenceConfidence.NONE
        }
    }

    private fun detectCodec(text: String): Pair<InferredCodec, InferenceConfidence> {
        return when {
            Regex("\\bAV1\\b").containsMatchIn(text) ->
                InferredCodec.AV1 to InferenceConfidence.HIGH
            Regex("\\b(HEVC|H265|X265)\\b").containsMatchIn(text) ->
                InferredCodec.HEVC to InferenceConfidence.HIGH
            Regex("\\b(H264|X264)\\b").containsMatchIn(text) ->
                InferredCodec.H264 to InferenceConfidence.HIGH
            else ->
                InferredCodec.UNKNOWN to InferenceConfidence.NONE
        }
    }
}
