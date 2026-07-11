package com.ruby.stream.feature.streams.selection

import com.ruby.stream.core.streams.RankedStreamCandidate

/**
 * PASS 5 — Stream Selection's top-level UI state.
 * ...
 */
sealed interface StreamSelectionUiState {
    data object Loading : StreamSelectionUiState

    data class Content(
        val candidates: List<RankedStreamCandidate>,
        val completedAttempts: Int,
        val totalProviders: Int,
    ) : StreamSelectionUiState {
        val discoveryComplete: Boolean
            get() = completedAttempts >= totalProviders
    }

    data object Empty : StreamSelectionUiState
    data object Error : StreamSelectionUiState
}
