package com.ruby.stream.feature.home

/**
 * PASS 5 — user-intent events emitted by the Home screen's Composables.
 *
 * Retry is deliberately TWO-SHAPED rather than one unified event:
 * RetrySection covers the three fixed, compile-time-known sections;
 * RetryCatalogRail(railId) covers the open, runtime-sized rail
 * collection. Conflating a closed enum with an open id-space would
 * reintroduce the exact "is this a rail id or a section name"
 * ambiguity SectionError was introduced to avoid elsewhere.
 *
 * CatalogRailItemClicked carries both titleId and railId (not just
 * titleId) since the same title can surface in multiple rails and a
 * handler/analytics event needs to know which rail surfaced a given
 * click.
 */
sealed interface HomeUiEvent {
    data class RetrySection(val section: HomeSection) : HomeUiEvent
    data class RetryCatalogRail(val railId: String) : HomeUiEvent

    data class HeroBannerClicked(val contentId: String) : HomeUiEvent
    data class ContinueWatchingItemClicked(val contentId: String, val episodeId: String?) : HomeUiEvent
    data class WatchlistItemClicked(val contentId: String) : HomeUiEvent
    data class CatalogRailItemClicked(val titleId: String, val railId: String) : HomeUiEvent
}

/** The three fixed, compile-time-known sections eligible for whole-section retry. */
enum class HomeSection {
    HERO_BANNER,
    CONTINUE_WATCHING,
    WATCHLIST,
}
