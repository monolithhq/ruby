package com.ruby.stream.feature.settings

/**
 * PASS 5 — user-intent events emitted by SettingsAddons (AD-018).
 *
 * ToggleEnabled/ToggleFamilyFriendly commit immediately, matching every
 * other preference-style screen. DeleteRequested triggers a ViewModel-
 * owned platform confirmation dialog, NOT a UiState field, per AD-018 --
 * actual deletion happens only after the ViewModel receives confirmation
 * from that dialog (not modeled here; a platform-layer concern, not a
 * UiEvent). InstallAddonClicked navigates to the deferred future
 * AddonInstall workflow (mode: INSTALL); this screen has no update
 * button of its own since that also belongs to AddonInstall (mode:
 * UPDATE), triggered per add-on once that workflow exists.
 */
sealed interface SettingsAddonsUiEvent {
    data class ToggleEnabled(val addonId: Long, val enabled: Boolean) : SettingsAddonsUiEvent
    data class ToggleFamilyFriendly(val addonId: Long, val familyFriendly: Boolean) : SettingsAddonsUiEvent
    data class DeleteRequested(val addonId: Long) : SettingsAddonsUiEvent
    data object InstallAddonClicked : SettingsAddonsUiEvent
}
