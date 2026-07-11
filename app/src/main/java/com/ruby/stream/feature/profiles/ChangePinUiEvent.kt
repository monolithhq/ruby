package com.ruby.stream.feature.profiles

/**
 * PASS 5 — user-intent events emitted by the ChangePin workflow.
 *
 * DigitEntered/BackspaceClicked model a numeric-keypad-style input
 * rather than a raw text field, matching PIN entry's typical mobile
 * UX; the ViewModel accumulates digits into whichever buffer the
 * current step targets (current/new/confirm).
 */
sealed interface ChangePinUiEvent {
    data class DigitEntered(val digit: Char) : ChangePinUiEvent
    data object BackspaceClicked : ChangePinUiEvent
    data object SubmitClicked : ChangePinUiEvent
}
