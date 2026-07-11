package com.ruby.stream.feature.profiles

import com.ruby.stream.data.database.entity.ProfileType

/**
 * PASS 5 — ProfileEditor's UI state (Session 12, FULLY LOCKED).
 *
 * Edits a specific OTHER profile (not necessarily the active one),
 * launched from ManageProfiles. Loading earned (reads the target
 * profile from Room at entry, per Session 11's "Loading is
 * conditional" rule).
 *
 * profileName, not "name" -- matches ChangePinUiState.Content
 * deliberately, since both screens can operate on a profile that is
 * NOT the active one.
 *
 * isOwner is a DOMAIN FACT, not derived policy (canDelete was proposed
 * and REJECTED in favor of this, Session 12) -- deletion policy will
 * likely grow more conditions over time (active-profile-cannot-
 * delete-itself, last-profile-protection), and one opaque boolean
 * would hide WHY from both the composable and future maintainers.
 *
 * canSave, not canSubmit -- deliberate per-screen verb naming
 * (Create/Continue/Save), not an inconsistency.
 */
sealed interface ProfileEditorUiState {
    data object Loading : ProfileEditorUiState
    data class Content(
        val profileId: Long,
        val profileName: String,
        val avatar: AvatarOption,
        val availableAvatars: List<AvatarOption>,
        val profileType: ProfileType,
        val contentRatingLevel: String?,
        val hasPin: Boolean,
        val isOwner: Boolean,
        val canSave: Boolean,
        val error: ProfileEditorError? = null,
    ) : ProfileEditorUiState
}

/**
 * DuplicateName-on-edit has one real implementation difference owed to
 * the repository's update path, not a new architectural decision: the
 * uniqueness check must exclude the row being edited (WHERE name =
 * :name AND id != :profileId), or a no-op save would self-collide
 * against the unique index (AD-013/Session 11).
 */
sealed interface ProfileEditorError {
    data object DuplicateName : ProfileEditorError
}
