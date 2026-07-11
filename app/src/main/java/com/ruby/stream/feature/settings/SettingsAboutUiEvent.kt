package com.ruby.stream.feature.settings

/**
 * PASS 5 — user-intent events emitted by SettingsAbout (AD-020).
 *
 * Both are navigation/action entries only -- this screen owns no
 * persisted state and has no toggles to commit. GitHub/source-code
 * links are deliberately absent, per AD-020 (a deployment decision, not
 * an architectural one -- can be added later without changing this
 * shape).
 */
sealed interface SettingsAboutUiEvent {
    data object OpenSourceLicensesClicked : SettingsAboutUiEvent
    data object PrivacyPolicyClicked : SettingsAboutUiEvent
}
