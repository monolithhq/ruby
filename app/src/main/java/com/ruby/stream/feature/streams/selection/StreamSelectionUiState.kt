package com.ruby.stream.feature.streams.selection

import com.ruby.stream.core.streams.RankedStreamCandidate

/**
 * PASS 5 — Stream Selection's top-level UI state.
 * ...
 *
 * AMENDMENT (Session 21, supports AD-022/PlaybackPolicy): Content gains
 * pendingDialog, a small piece of transient interaction state -- not a
 * new top-level UiState case -- representing a confirmation the screen
 * cannot proceed past without resolving. This differs from
 * SettingsAddons' delete-confirmation (AD-018), which stays an
 * imperative platform dialog: that action is secondary/administrative
 * and doesn't block the screen's own primary purpose either way, while
 * this dialog directly blocks Stream Selection's one job (getting the
 * user to playback). pendingDialog only ever needs to exist on Content,
 * never Empty/Error/Loading -- StreamSelected (the event that triggers
 * PlaybackPolicy.evaluate()) can only fire while Content is already
 * showing candidates.
 */
sealed interface StreamSelectionUiState {
    data object Loading : StreamSelectionUiState

    data class Content(
        val candidates: List<RankedStreamCandidate>,
        val completedAttempts: Int,
        val totalProviders: Int,
        val pendingDialog: StreamSelectionDialog? = null,
    ) : StreamSelectionUiState {
        val discoveryComplete: Boolean
            get() = completedAttempts >= totalProviders
    }

    data object Empty : StreamSelectionUiState
    data object Error : StreamSelectionUiState
}

/**
 * A confirmation dialog blocking Stream Selection's primary workflow.
 * Currently exactly one case; kept as a sealed interface (not a plain
 * Boolean flag) so a second blocking dialog can be added later without
 * changing pendingDialog's type -- same reasoning already applied
 * elsewhere in this codebase to preferring enums/sealed types over
 * Booleans (e.g. ProfileType over an isKids Boolean).
 *
 * CellularWarning carries the RankedStreamCandidate the user originally
 * selected, so CellularWarningConfirmed can hand it straight to
 * playback without the ViewModel needing to re-derive or re-store
 * "which candidate was pending" separately.
 */
sealed interface StreamSelectionDialog {
    data class CellularWarning(val candidate: RankedStreamCandidate) : StreamSelectionDialog
}
