package com.ruby.stream.feature.profiles

/** PASS 5 — user-intent events emitted by the ProfileEditor screen. */
sealed interface ProfileEditorUiEvent {
    data class NameChanged(val name: String) : ProfileEditorUiEvent
    data class AvatarSelected(val avatarId: String) : ProfileEditorUiEvent
    data class ProfileTypeChanged(val type: com.ruby.stream.data.database.entity.ProfileType) : ProfileEditorUiEvent
    data class ContentRatingLevelChanged(val level: String?) : ProfileEditorUiEvent
    data object SaveClicked : ProfileEditorUiEvent
    data object DeleteClicked : ProfileEditorUiEvent
    data object ResetPinClicked : ProfileEditorUiEvent
}
