package com.ruby.stream.feature.search

/**
 * PASS 5 — Search's top-level UI state.
 *
 * Idle / Loading / Content(results) / Empty(reason) / Error -- NOT
 * SectionState<T>, same single-purpose-screen reasoning as Stream
 * Selection (one primary result list, no independently loading
 * sections).
 *
 * Idle is kept genuinely distinct from Empty (no query typed yet vs. a
 * completed search with zero matches) -- collapsing them would lose a
 * real, useful UX distinction ("search for movies..." vs. "no results
 * for X").
 *
 * Error is reserved STRICTLY for the search pipeline itself
 * malfunctioning (DB unavailable, repository throwing unexpectedly).
 * It does NOT mean "no add-ons installed" or "installed add-ons have
 * no searchable catalogs" -- those are valid, expected domain outcomes
 * (the same infrastructure-vs-content distinction as AD-005, one level
 * down), not failures of the search system; they surface via
 * Empty(SearchUnavailable) instead.
 */
sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Content(val results: List<SearchResultItem>) : SearchUiState
    data class Empty(val reason: SearchEmptyReason) : SearchUiState
    data object Error : SearchUiState
}

data class SearchResultItem(
    val contentId: String,
    val contentType: String,
    val title: String,
    val posterUrl: String?,
)

/**
 * The "why is it empty" distinction is carried as data on Empty, not
 * as additional top-level state variants. NoResults and
 * SearchUnavailable were initially drafted with SearchUnavailable
 * split into two separate reasons ("no add-ons installed" / "no
 * searchable catalogs"), then collapsed after applying a rule worth
 * reusing going forward: expose a distinction in UI state only when it
 * changes user-facing behavior or an available action, not merely
 * because the implementation is able to detect it. Neither installation-
 * related sub-case gives the user a different available action in v1,
 * so they collapse to one.
 */
enum class SearchEmptyReason {
    NO_RESULTS,
    SEARCH_UNAVAILABLE,
}
