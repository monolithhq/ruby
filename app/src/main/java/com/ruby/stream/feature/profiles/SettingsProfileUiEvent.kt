package com.ruby.stream.feature.profiles

/**
 * PASS 5 — user-intent events emitted by the SettingsProfile screen.
 *
 * ChangePinClicked/ManageProfilesClicked are pure navigation triggers
 * -- SettingsProfile delegates PIN changing and other-profile
 * management to their own dedicated destinations rather than
 * implementing either inline (AD-013).
 */
sealed interface SettingsProfileUiEvent {
    data class NameChanged(val name: String) : SettingsProfileUiEvent
    data class AvatarSelected(val avatarId: String) : SettingsProfileUiEvent
    data class ContentRatingLevelChanged(val level: String?) : SettingsProfileUiEvent
    data object ChangePinClicked : SettingsProfileUiEvent
    data object ManageProfilesClicked : SettingsProfileUiEvent
}
