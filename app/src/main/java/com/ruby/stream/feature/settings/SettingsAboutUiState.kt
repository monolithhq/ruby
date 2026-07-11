package com.ruby.stream.feature.settings

/**
 * PASS 5 — SettingsAbout's state (Session 15, AD-020). The last of the
 * eleven Settings sub-screens, and the one with genuinely no owned
 * state -- completes the Settings-section taxonomy.
 *
 * Confirmed against the actual build config before designing:
 * versionName/versionCode are real compile-time values ("1.0.0"/1),
 * available via BuildConfig with zero async read required. No existing
 * open-source-notices/license infrastructure exists in the repo (no
 * aggregation plugin, no bundled LICENSE files) -- a real gap, not an
 * oversight, since nothing has needed it until now.
 *
 * NO Loading, and NO sealed interface at all -- the first Settings
 * screen to reach this conclusion. Every field is synchronously
 * available at ViewModel construction (compile-time constants, static
 * assets, static links) -- unlike Appearance/Network (DataStore),
 * Storage (filesystem inspection), or Add-ons (Room Flow), there is no
 * suspending dependency gating initial render.
 *
 * Larger content gets its own destination, not embedded inline --
 * matching the established pattern (SettingsStorage -> Manage
 * Downloads, SettingsAddons -> AddonInstall). "Open Source Licenses"
 * and "Privacy Policy" are action/navigation entries, not inline
 * content; whether either currently opens a real screen or is stubbed
 * for later is an implementation detail, not an architectural one.
 *
 * GitHub/source-code links are explicitly NOT baked into the
 * architecture -- these are deployment decisions (public repo, private
 * repo, no repo), not something the UiState should assume either way.
 *
 * Scope is deliberately factual only: app version, build number, and
 * links to Privacy Policy / Open Source Licenses. No toggles, no
 * persistence, no repository, no business logic.
 */
data class SettingsAboutUiState(
    val appVersion: String,
    val buildNumber: Int,
)
