package com.ruby.stream.feature.profiles

import com.ruby.stream.data.database.entity.ProfileType

/**
 * PASS 5 — SettingsProfile's UI state (Session 10, EDITOR/DISPLAY
 * screen category).
 *
 * Edits ONLY the currently-active profile: name, avatar, "has PIN"
 * display (pinHash != null check), profileType (ADULT/KIDS)
 * display/switching, contentRatingLevel selection. Does NOT implement
 * PIN changing itself (delegates to ChangePin) and does NOT manage
 * other profiles (delegates to ManageProfiles, Owner-only entry
 * point).
 *
 * hasPin is a DOMAIN FACT (pinHash != null), not derived policy --
 * UiState exposes the fact, the Composable decides what to render
 * from it ("Set PIN" vs "Change PIN").
 */
sealed interface SettingsProfileUiState {
    data object Loading : SettingsProfileUiState
    data class Content(
        val profileId: Long,
        val profileName: String,
        val avatarUrl: String?,
        val hasPin: Boolean,
        val profileType: ProfileType,
        val contentRatingLevel: String?,
        val isOwner: Boolean,
    ) : SettingsProfileUiState
}
