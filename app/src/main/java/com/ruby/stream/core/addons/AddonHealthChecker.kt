package com.ruby.stream.core.addons

import com.ruby.stream.core.addons.model.Manifest
import com.ruby.stream.data.database.entity.AddonHealth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PASS 2B, REVISED IN PASS 3 -- determines an add-on's operational
 * health via a manifest check.
 *
 * Uses AddonHealth (data/database/entity/InstalledAddonEntity.kt) as
 * its result type, NOT a separate PASS-2B-local enum -- an earlier
 * version of this file defined its own AddonHealthState with
 * different values (ENABLED/DISABLED/TIMEOUT/INVALID_MANIFEST),
 * duplicating the entity layer's AddonHealth
 * (HEALTHY/TIMEOUT/UNREACHABLE/INVALID_MANIFEST). Unified onto one
 * enum during PASS 3 to avoid permanent drift between two vocabularies
 * describing the same thing. See SOT "Unify health vocabulary".
 *
 * DISABLED was deliberately removed from AddonHealth entirely (not
 * just from this file) -- InstalledAddonEntity.enabled already models
 * "should Ruby attempt to use this add-on", and health now answers a
 * strictly narrower, orthogonal question: "what happened the last time
 * Ruby interacted with it". This class no longer accepts a
 * userDisabled parameter -- callers that already know an add-on is
 * disabled should not call checkHealth for it at all, since there is
 * nothing left to check.
 *
 * IMPORTANT — call-site scope change (PASS 3): this class is NOT
 * called by AddonRepository on every stream request anymore. The
 * stream request itself now serves as the live health probe (see
 * AddonRepository) -- calling this beforehand would duplicate that
 * network round trip. checkHealth is reserved for: add-on
 * installation, user-triggered manual refresh (Settings -> Add-ons),
 * and periodic background validation.
 */
data class AddonHealthResult(
    val health: AddonHealth,
    val manifest: Manifest?,
)

@Singleton
class AddonHealthChecker @Inject constructor(
    private val addonExecutor: AddonExecutor,
) {

    suspend fun checkHealth(manifestUrl: String): AddonHealthResult {
        return when (val result = addonExecutor.getManifestResult(manifestUrl)) {
            is ManifestFetchResult.Unreachable ->
                AddonHealthResult(health = AddonHealth.UNREACHABLE, manifest = null)

            is ManifestFetchResult.Malformed ->
                AddonHealthResult(health = AddonHealth.INVALID_MANIFEST, manifest = null)

            is ManifestFetchResult.Success -> {
                val manifest = result.manifest
                val isValid = manifest.id.isNotBlank() &&
                    manifest.name.isNotBlank() &&
                    manifest.resources.isNotEmpty()

                if (isValid) {
                    AddonHealthResult(health = AddonHealth.HEALTHY, manifest = manifest)
                } else {
                    AddonHealthResult(health = AddonHealth.INVALID_MANIFEST, manifest = manifest)
                }
            }
        }
    }
}
