package com.ruby.stream.feature.profiles

import com.ruby.stream.data.database.entity.ProfileType

/**
 * PASS 5 — ManageProfiles' UI state (Session 10, EDITOR/DISPLAY screen
 * category). Owner-only. Lists all profiles; entry point for
 * create/delete/reset-another-profile's-PIN/change-another-profile's-
 * type.
 *
 * Loading earned legitimately per AD-013's "Loading is conditional"
 * rule (Session 11 refinement): this screen genuinely reads all
 * ProfileEntity rows from Room at entry, so Loading reflects a real
 * suspending dependency here, not consistency applied for its own
 * sake.
 */
sealed interface ManageProfilesUiState {
    data object Loading : ManageProfilesUiState
    data class Content(
        val profiles: List<ProfileListItem>,
    ) : ManageProfilesUiState
}

data class ProfileListItem(
    val id: Long,
    val name: String,
    val avatarUrl: String?,
    val profileType: ProfileType,
    val isOwner: Boolean,
    val hasPin: Boolean,
)
