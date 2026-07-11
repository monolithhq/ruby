package com.ruby.stream.feature.profiles

import com.ruby.stream.data.database.entity.ProfileType

/**
 * PASS 5 — CreateProfile's UI state (Session 11, FULLY LOCKED, AD-013).
 * The first AD-013 workflow that is deliberately NOT staged.
 *
 * A PLAIN data class, not a sealed interface with a lone Content
 * member -- deliberately reconsidered and simplified once Loading was
 * removed: a sealed hierarchy is earned by genuinely having more than
 * one observable state, not kept preemptively for a hypothetical
 * future state.
 *
 * No Loading -- constructing this screen's initial state requires no
 * suspending operation (static bundled avatars + deterministic
 * defaults), per Session 11's "Loading is conditional" rule.
 *
 * A SINGLE FLAT FORM, not a step-based wizard, unlike ChangePin --
 * staging is only warranted when fields have a genuine SEQUENTIAL
 * DEPENDENCY (ChangePin's confirm-step cannot happen before its
 * enter-step). CreateProfile's fields (name, avatar, profileType) have
 * no such dependency.
 *
 * PIN is explicitly NOT collected at creation time -- a newly created
 * profile is PIN-less; setting one afterward reuses the existing
 * ChangePin flow unchanged (which already starts at ENTER_NEW when
 * pinHash is null).
 *
 * NO success/COMPLETE state -- matches ChangePin's own precedent:
 * repository.createProfile() succeeds -> navigateBack().
 */
data class CreateProfileUiState(
    val name: String,
    val selectedAvatar: AvatarOption,
    val availableAvatars: List<AvatarOption>,
    val profileType: ProfileType,
    val canSubmit: Boolean,
    val error: CreateProfileError? = null,
)

sealed interface CreateProfileError {
    data object DuplicateName : CreateProfileError
}

/**
 * A CURATED LOCAL PICKER -- explicitly a PRESENTATION-LAYER constraint
 * only. availableAvatars is a fixed local asset manifest.
 * ProfileEntity.avatarUrl remains a generic String? at the schema
 * level; CreateProfile simply happens to populate it with a bundled-
 * asset identifier today. Shared with ProfileEditorUiState, which also
 * offers avatar selection against the same fixed set.
 */
data class AvatarOption(val id: String, val assetPath: String)
