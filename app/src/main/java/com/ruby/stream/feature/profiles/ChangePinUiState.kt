package com.ruby.stream.feature.profiles

/**
 * PASS 5 — ChangePin's UI state (Session 10, FULLY LOCKED, AD-013).
 * A dedicated multi-step WORKFLOW destination (not embedded in
 * SettingsProfile) -- consistent with Stream Selection already being a
 * real route, not a mode/dialog embedded in another screen's state.
 *
 * ONE UiState with an internal step enum, not one route per step -- a
 * single logical action should not fragment the back stack into N
 * entries.
 *
 * Loading exists for consistency with every other repository-backed
 * screen's lifecycle, even though reading one local ProfileEntity row
 * is fast enough to resolve within a frame -- the value here is
 * uniformity across screens, not performance.
 *
 * The initial step is computed ONCE by the ViewModel at entry (see
 * ChangePinArgs/PinAuthority below), never re-derived by the UI.
 *
 * NO COMPLETE/success terminal state exists -- successful completion
 * is simply the workflow ending (repository.setPin() succeeds ->
 * navigateBack()), explicitly NOT a UI state that exists only long
 * enough to disappear before the user can perceive it.
 */
sealed interface ChangePinUiState {
    data object Loading : ChangePinUiState
    data class Content(
        val profileName: String,
        val step: ChangePinStep,
        val canSubmit: Boolean,
        val error: PinError? = null,
    ) : ChangePinUiState
}

enum class ChangePinStep { ENTER_CURRENT, ENTER_NEW, CONFIRM_NEW }

/**
 * PinError is deliberately small -- format-only invalidity (wrong
 * length, non-numeric input) is NEVER a PinError; it is represented
 * purely by canSubmit=false, disabling the action before submission is
 * even attemptable. PinError is reserved for failures the user could
 * not have known before submitting.
 *
 * THE GENERAL WORKFLOW-ERROR INVARIANT this type follows (reusable
 * beyond PINs -- see profile-name uniqueness, recovery-phrase mismatch,
 * add-on manifest URL validation):
 *   1. A validation/verification failure never changes the current
 *      step -- the user stays exactly where the failure occurred.
 *   2. The ViewModel MAY normalize/reset invalid input as part of
 *      handling the failure (e.g. clearing only the confirmation
 *      buffer on a mismatch, keeping the already-entered new-PIN
 *      buffer intact).
 *   3. The resulting error is STICKY across that ViewModel-driven
 *      correction -- it does NOT clear merely because the ViewModel
 *      cleared/reset a buffer. Only the user's OWN next edit to the
 *      relevant input clears the error.
 *   4. canSubmit is computed from ONLY the current input's syntactic
 *      completeness/validity -- explicitly NOT a function of whether
 *      the last submission attempt failed. A just-failed, still-
 *      complete buffer may legitimately have canSubmit=true and
 *      error!=null simultaneously.
 */
sealed interface PinError {
    data object CurrentPinIncorrect : PinError
    data object PinsDoNotMatch : PinError
}

/**
 * PinAuthority parameterizes ChangePin for every PIN-changing scenario,
 * not a duplicate Owner-specific screen (Session 12 refinement). A raw
 * requiresCurrentPin: Boolean nav arg was considered and REJECTED --
 * that would push a DERIVED fact into nav-args, forcing every future
 * call site to re-derive the same precedence.
 *
 * ChangePinViewModel alone derives the initial step, OWNER_ADMIN
 * checked FIRST to make precedence explicit:
 *   val initialStep = when {
 *     authority == PinAuthority.OWNER_ADMIN -> ChangePinStep.ENTER_NEW
 *     profile.pinHash == null -> ChangePinStep.ENTER_NEW
 *     else -> ChangePinStep.ENTER_CURRENT
 *   }
 * Precedence: (1) admin authority overrides verification entirely,
 * even if a PIN already exists; (2) no PIN means nothing to verify;
 * (3) otherwise verify the existing PIN.
 *
 * authority is NEVER exposed to ChangePinUiState/the UI -- the
 * composable has no reason to know WHY the workflow started where it
 * did.
 */
data class ChangePinArgs(val profileId: Long, val authority: PinAuthority)

enum class PinAuthority { PROFILE_OWNER, OWNER_ADMIN }
