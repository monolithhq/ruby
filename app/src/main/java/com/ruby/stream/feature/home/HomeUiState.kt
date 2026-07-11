package com.ruby.stream.feature.home

import com.ruby.stream.feature.SectionState

/**
 * PASS 5 — Home's top-level UI state.
 *
 * Home is a COMPOSITION of simultaneously visible sections, confirmed
 * against the real nav graph and locked v1 section list (top to
 * bottom): Hero Banner -> Continue Watching (conditional) -> Watchlist
 * (conditional) -> Catalog Rails (0..N, dynamic). This is the first
 * real consumer of SectionState<T>/AD-005/AD-006.
 *
 * Named/distinct-behavior sections get dedicated top-level properties;
 * the repeated/unbounded section (catalog rails) is a
 * List<SectionState<...>>, not one property per genre/rail -- keeps
 * this state independent of however many rails are configured, rather
 * than hardcoding an assumed count.
 *
 * "New and Hot" deliberately does NOT appear here -- it remains its
 * own separate nav-graph destination (PASS 1). Home stays the
 * "continue browsing" screen; New and Hot stays the discovery screen.
 */
data class HomeUiState(
    val heroBanner: SectionState<HeroBannerUiModel>,
    val continueWatching: SectionState<List<ContinueWatchingItem>>,
    val watchlist: SectionState<List<WatchlistItem>>,
    val catalogRails: List<SectionState<CatalogRailUiModel>>,
)

/** Single featured title shown at the top of Home. Rotation policy TBD. */
data class HeroBannerUiModel(
    val contentId: String,
    val contentType: String,
    val title: String,
    val backgroundUrl: String?,
    val description: String?,
)

/**
 * One row entry in the Continue Watching section, backed by
 * LibraryEntity(type = CONTINUE_WATCHING). episodeId stays nullable,
 * matching LibraryEntity's own convention (movies simply have no
 * episode), deliberately NOT normalized to PlaybackHistoryEntity's
 * NO_EPISODE sentinel convention used elsewhere.
 */
data class ContinueWatchingItem(
    val contentId: String,
    val contentType: String,
    val title: String,
    val posterUrl: String?,
    val episodeId: String?,
    val playbackPositionMs: Long,
    val durationMs: Long,
)

/** One row entry in the Watchlist section, backed by LibraryEntity(type = WATCHLIST). */
data class WatchlistItem(
    val contentId: String,
    val contentType: String,
    val title: String,
    val posterUrl: String?,
)

/**
 * One catalog rail's content. The rail's SOURCE is deliberately
 * abstracted -- today an installed add-on's catalog, could be
 * merged/curated/recommended later without a HomeUiState shape change.
 */
data class CatalogRailUiModel(
    val railId: String,
    val title: String,
    val items: List<CatalogRailItem>,
)

data class CatalogRailItem(
    val contentId: String,
    val contentType: String,
    val title: String,
    val posterUrl: String?,
)
