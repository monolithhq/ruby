package com.ruby.stream.feature.profiles

/** PASS 5 — user-intent events emitted by the CreateProfile workflow. */
sealed interface CreateProfileUiEvent {
    data class NameChanged(val name: String) : CreateProfileUiEvent
    data class AvatarSelected(val avatarId: String) : CreateProfileUiEvent
    data class ProfileTypeChanged(val type: com.ruby.stream.data.database.entity.ProfileType) : CreateProfileUiEvent
    data object SubmitClicked : CreateProfileUiEvent
}
