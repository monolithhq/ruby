package com.ruby.stream.core.addons

import com.ruby.stream.core.addons.model.Manifest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PASS 2B — determines an installed add-on's health state.
 *
 * Feeds Settings -> Add-ons -> Health (Enabled/Disabled/Timeout/
 * Invalid Manifest), per Section 5 of the sitemap. Deliberately
 * separate from AddonExecutor: this class answers "can I use this
 * add-on at all", while AddonExecutor answers "what did this specific
 * request return". Keeping them apart means health checks can run on
 * their own schedule (e.g. periodic re-validation) without coupling to
 * whatever screen happens to be requesting streams right now.
 */
enum class AddonHealthState {
    ENABLED,
    DISABLED,
    TIMEOUT,
    INVALID_MANIFEST,
}

data class AddonHealthResult(
    val state: AddonHealthState,
    val manifest: Manifest?,
)

@Singleton
class AddonHealthChecker @Inject constructor(
    private val addonExecutor: AddonExecutor,
) {

    /**
     * userDisabled is passed in rather than looked up here -- health
     * checking shouldn't own the "is this add-on toggled off" concern,
     * that's InstalledAddonEntity's job (PASS 0B). This function only
     * answers whether the add-on, if enabled, is actually reachable
     * and valid.
     */
    suspend fun checkHealth(manifestUrl: String, userDisabled: Boolean): AddonHealthResult {
        if (userDisabled) {
            return AddonHealthResult(state = AddonHealthState.DISABLED, manifest = null)
        }

        return when (val result = addonExecutor.getManifestResult(manifestUrl)) {
            is ManifestFetchResult.Unreachable ->
                AddonHealthResult(state = AddonHealthState.TIMEOUT, manifest = null)

            is ManifestFetchResult.Malformed ->
                AddonHealthResult(state = AddonHealthState.INVALID_MANIFEST, manifest = null)

            is ManifestFetchResult.Success -> {
                val manifest = result.manifest
                val isValid = manifest.id.isNotBlank() &&
                    manifest.name.isNotBlank() &&
                    manifest.resources.isNotEmpty()

                if (isValid) {
                    AddonHealthResult(state = AddonHealthState.ENABLED, manifest = manifest)
                } else {
                    AddonHealthResult(state = AddonHealthState.INVALID_MANIFEST, manifest = manifest)
                }
            }
        }
    }
}
