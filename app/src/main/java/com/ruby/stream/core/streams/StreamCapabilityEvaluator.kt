package com.ruby.stream.core.streams

import com.ruby.stream.core.addons.model.StreamObject
import com.ruby.stream.data.database.entity.AddonHealth
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
 *
 * addonEnabled and addonHealth are two orthogonal, separately-passed
 * checks (revised in PASS 3, see SOT "Unify health vocabulary"):
 * addonEnabled answers "should Ruby attempt to use this add-on at
 * all" (InstalledAddonEntity.enabled); addonHealth answers "what
 * happened the last time Ruby interacted with it"
 * (InstalledAddonEntity.health / AddonHealth, which no longer has a
 * DISABLED value -- that concept lives entirely in addonEnabled now).
 */
@Singleton
class StreamCapabilityEvaluator @Inject constructor() {

    fun evaluate(
        stream: StreamObject,
        addonEnabled: Boolean,
        addonHealth: AddonHealth,
    ): IneligibilityReason? {
        if (!addonEnabled) {
            return IneligibilityReason.ADDON_DISABLED
        }
        if (addonHealth == AddonHealth.INVALID_MANIFEST) {
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
        addonHealth: AddonHealth,
    ): Boolean = evaluate(stream, addonEnabled, addonHealth) == null
}
