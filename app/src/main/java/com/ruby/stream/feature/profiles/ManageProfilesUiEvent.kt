package com.ruby.stream.feature.profiles

/**
 * PASS 5 — user-intent events emitted by the ManageProfiles screen.
 *
 * ResetPinClicked triggers ChangePin parameterized with
 * PinAuthority.OWNER_ADMIN for the target profile (AD-013 Session 12
 * refinement) -- the Owner overrides current-PIN verification entirely
 * even if the target profile already has a PIN.
 */
sealed interface ManageProfilesUiEvent {
    data object CreateProfileClicked : ManageProfilesUiEvent
    data class ProfileClicked(val profileId: Long) : ManageProfilesUiEvent
    data class DeleteProfileClicked(val profileId: Long) : ManageProfilesUiEvent
    data class ResetPinClicked(val profileId: Long) : ManageProfilesUiEvent
}
