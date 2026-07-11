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
 */
sealed interface StreamSelectionUiEvent {
    data class StreamSelected(val candidate: RankedStreamCandidate) : StreamSelectionUiEvent
    data object RetryClicked : StreamSelectionUiEvent
}
