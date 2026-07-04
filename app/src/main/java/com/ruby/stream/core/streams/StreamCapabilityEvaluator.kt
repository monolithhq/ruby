package com.ruby.stream.core.streams

import com.ruby.stream.core.addons.AddonHealthState
import com.ruby.stream.core.addons.model.StreamObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PASS 3, Stage 1 (Eligibility). Binary gate only -- no scoring, no
 * label parsing, no ranking. A stream either passes every check below
 * or it is excluded before StreamRanker ever sees it.
 *
 * Locked architecture decision (see SOT "Eligibility precedes
 * ranking"): playback capability is an eligibility criterion, not a
 * ranking criterion. Every stream that reaches StreamRanker is already
 * known to be playable -- there is no "prefer HTTP over YouTube" step,
 * because by the time ranking runs, only playable streams remain.
 */
@Singleton
class StreamCapabilityEvaluator @Inject constructor() {

    /**
     * Returns null if the stream is eligible, or the specific reason
     * it was excluded if not. Returning the reason (not just a
     * Boolean) costs nothing here and pays off the moment Ruby wants
     * to surface why a stream was hidden -- without this, that would
     * require re-deriving the same checks a second time later.
     */
    fun evaluate(
        stream: StreamObject,
        addonEnabled: Boolean,
        addonHealth: AddonHealthState,
    ): IneligibilityReason? {
        if (!addonEnabled || addonHealth == AddonHealthState.DISABLED) {
            return IneligibilityReason.ADDON_DISABLED
        }
        if (addonHealth == AddonHealthState.INVALID_MANIFEST) {
            return IneligibilityReason.ADDON_MANIFEST_INVALID
        }
        if (!stream.isPlayableByRuby()) {
            return IneligibilityReason.UNSUPPORTED_SOURCE_TYPE
        }
        if (stream.url != null && stream.url.isBlank()) {
            return IneligibilityReason.MISSING_REQUIRED_FIELD
        }
        if (stream.ytId != null && stream.ytId.isBlank()) {
            return IneligibilityReason.MISSING_REQUIRED_FIELD
        }
        if (stream.externalUrl != null && stream.externalUrl.isBlank()) {
            return IneligibilityReason.MISSING_REQUIRED_FIELD
        }
        return null
    }

    fun isEligible(
        stream: StreamObject,
        addonEnabled: Boolean,
        addonHealth: AddonHealthState,
    ): Boolean = evaluate(stream, addonEnabled, addonHealth) == null
}
