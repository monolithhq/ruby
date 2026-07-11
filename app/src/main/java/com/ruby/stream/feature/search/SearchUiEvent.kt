package com.ruby.stream.feature.search

/**
 * PASS 5 — user-intent events emitted by the Search screen's
 * Composables.
 *
 * QueryChanged carries the RAW, un-debounced query string on every
 * keystroke -- debounce is locked as a ViewModel-owned architecture
 * decision (AD-00M: 300ms, via debounce()+distinctUntilChanged() on a
 * MutableStateFlow<String>), never Compose-owned. Compose emits user
 * intent only; the ViewModel owns search POLICY (debounce,
 * cancellation, blank-query handling). Blank queries resolve directly
 * to Idle inside the ViewModel and are never passed to
 * AddonRepository.search() at all.
 */
sealed interface SearchUiEvent {
    data class QueryChanged(val query: String) : SearchUiEvent
    data class ResultClicked(val contentId: String) : SearchUiEvent
    data object RetryClicked : SearchUiEvent
}
