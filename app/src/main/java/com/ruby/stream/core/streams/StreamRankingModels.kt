package com.ruby.stream.core.streams

import com.ruby.stream.core.addons.model.StreamObject

/**
 * PASS 3 — models for Ruby-owned playback intelligence, kept separate
 * from core/addons/model/ (PASS 2B). Protocol models stay protocol-
 * pure; nothing here is part of the Stremio wire format. See SOT
 * Architecture Decisions: "Authoritative before heuristic".
 */

enum class InferenceConfidence {
    NONE,
    LOW,
    MEDIUM,
    HIGH,
}

enum class InferredResolution {
    UHD_2160P,
    QHD_1440P,
    FHD_1080P,
    HD_720P,
    SD_576P,
    SD_480P,
    SD_360P,
    SD_GENERIC,
    UNKNOWN,
}

enum class InferredHdr {
    DOLBY_VISION,
    HDR10_PLUS,
    HDR10,
    HLG,
    NONE_DETECTED,
    UNKNOWN,
}

enum class InferredCodec {
    AV1,
    HEVC,
    H264,
    UNKNOWN,
}

/**
 * Output of StreamLabelAnalyzer for one StreamObject. Every field here
 * is ADVISORY. Nothing in this class may be used to determine whether
 * a stream is eligible for playback -- only to help order otherwise-
 * equivalent eligible streams. See StreamRanker.
 */
data class StreamLabelAnalysis(
    val resolution: InferredResolution,
    val resolutionConfidence: InferenceConfidence,
    val hdr: InferredHdr,
    val hdrConfidence: InferenceConfidence,
    val codec: InferredCodec,
    val codecConfidence: InferenceConfidence,
)

/**
 * Why a stream was excluded before ranking. Surfaced for debugging/
 * diagnostics (e.g. a future "why wasn't this stream shown" screen) --
 * NOT surfaced to end users as-is, that's a PASS 7 UI concern.
 */
enum class IneligibilityReason {
    ADDON_DISABLED,
    ADDON_MANIFEST_INVALID,
    UNSUPPORTED_SOURCE_TYPE,
    MISSING_REQUIRED_FIELD,
}

/**
 * A stream that survived Stage 1 (eligibility) and carries everything
 * StreamRanker needs for Stage 2/3, without re-deriving it repeatedly.
 */
data class RankedStreamCandidate(
    val stream: StreamObject,
    val sourceAddonId: Long,
    val sourceAddonHealthy: Boolean,
    val labelAnalysis: StreamLabelAnalysis,
)

/**
 * One emission of AddonRepository.discoverStreams() (AD-012). Kept
 * deliberately separate from any ViewModel/UI-layer state type
 * (StreamSelectionUiState) -- this type's job is to carry repository-
 * level provider-progress accounting; the ViewModel maps it into
 * whatever the UI actually needs, so repository concerns never dictate
 * the UI contract.
 *
 * candidates is an IMMUTABLE SNAPSHOT at emission time (an explicit
 * architectural guarantee, not just an implementation detail of how
 * it happens to be built) -- no emission may ever expose the
 * repository's own live, still-mutating accumulator to a collector.
 *
 * completedAttempts counts providers whose WORK has finished --
 * succeeded with streams, succeeded with zero streams, or failed --
 * NOT providers that specifically found streams. An empty or failed
 * provider still advances this count and still triggers an emission;
 * see discoverStreams()'s own doc for the full deterministic emission
 * contract (exactly totalProviders + 1 emissions per discovery
 * session). No separate `finished: Boolean` field -- completion is
 * (completedAttempts == totalProviders), and a derived boolean here
 * would just be a second, independently-mutable fact that could drift
 * out of sync with the two ints it's derived from.
 */
data class StreamDiscoveryUpdate(
    val candidates: List<RankedStreamCandidate>,
    val completedAttempts: Int,
    val totalProviders: Int,
)
