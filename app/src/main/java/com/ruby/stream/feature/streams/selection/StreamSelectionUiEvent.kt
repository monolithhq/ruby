package com.ruby.stream.feature.streams.selection

import com.ruby.stream.core.streams.RankedStreamCandidate

/**
 * PASS 5 — user-intent events emitted by the Stream Selection screen's
 * Composables.
 *
 * StreamSelected carries the full RankedStreamCandidate, not a bare
 * id, since the ViewModel already holds the complete list from
 * StreamSelectionUiState.Content and there is no separate lookup step
 * needed -- passing an id back would force the ViewModel to re-search
 * a list it already handed to the UI in full.
 *
 * AMENDMENT (Session 21, supports AD-022/PlaybackPolicy):
 * CellularWarningConfirmed/CellularWarningDismissed resolve the
 * StreamSelectionDialog.CellularWarning case that StreamSelected can
 * trigger when PlaybackPolicy.evaluate() returns Warn. Neither event
 * carries a candidate of its own -- the ViewModel already has it,
 * stored on the pendingDialog it's resolving (StreamSelectionUiState.
 * Content.pendingDialog.candidate), the same "don't make the caller
 * hand back what the ViewModel already holds" reasoning already applied
 * to StreamSelected above.
 */
sealed interface StreamSelectionUiEvent {
    data class StreamSelected(val candidate: RankedStreamCandidate) : StreamSelectionUiEvent
    data object RetryClicked : StreamSelectionUiEvent
    data object CellularWarningConfirmed : StreamSelectionUiEvent
    data object CellularWarningDismissed : StreamSelectionUiEvent
}
