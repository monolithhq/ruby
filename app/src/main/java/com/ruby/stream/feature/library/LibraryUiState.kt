package com.ruby.stream.feature.library

/**
 * PASS 5 — Library's shell state.
 *
 * Library is ONE navigation destination containing FOUR TABS
 * (Watchlist/Continue Watching/Downloads/History), not four separate
 * nav-graph screens and not one Home-style stacked-section screen --
 * confirmed against the real nav graph and against all four backing
 * sources (LibraryEntity split by type, DownloadEntity,
 * PlaybackHistoryEntity), all pre-existing PASS 0B tables.
 *
 * Key distinction from Home: Home is a COMPOSITION of simultaneously
 * visible sections (SectionState<T>/AD-005-006 applies). Library is a
 * CONTAINER for mutually exclusive views, only one visible at a time,
 * each backed by a genuinely different query -- so it does NOT use
 * SectionState<T> here. The shell holds only which tab is selected;
 * each tab owns its own independent Loading/Content/Empty/Error state,
 * loaded lazily on first visit and cached thereafter, not all four
 * eagerly on screen entry.
 */
data class LibraryUiState(
    val selectedTab: LibraryTab,
)

enum class LibraryTab {
    WATCHLIST,
    CONTINUE_WATCHING,
    DOWNLOADS,
    HISTORY,
}

/** Backed by LibraryEntity(type = WATCHLIST). */
sealed interface WatchlistUiState {
    data object Loading : WatchlistUiState
    data class Content(val items: List<WatchlistItem>) : WatchlistUiState
    data object Empty : WatchlistUiState
    data object Error : WatchlistUiState
}

data class WatchlistItem(
    val contentId: String,
    val contentType: String,
    val title: String,
    val posterUrl: String?,
)

/** Backed by LibraryEntity(type = CONTINUE_WATCHING). */
sealed interface ContinueWatchingUiState {
    data object Loading : ContinueWatchingUiState
    data class Content(val items: List<ContinueWatchingItemUiModel>) : ContinueWatchingUiState
    data object Empty : ContinueWatchingUiState
    data object Error : ContinueWatchingUiState
}

/**
 * episodeId stays NULLABLE here, matching LibraryEntity's own
 * convention (movies simply have no episode) -- deliberately NOT
 * normalized to PlaybackHistoryEntity's NO_EPISODE sentinel used by
 * HistoryItemUiModel below. Two different null-conventions preserved
 * on purpose rather than unified to one style, since normalizing would
 * make a UI model silently disagree with its own backing entity's
 * documented invariant.
 */
data class ContinueWatchingItemUiModel(
    val contentId: String,
    val contentType: String,
    val title: String,
    val posterUrl: String?,
    val episodeId: String?,
    val playbackPositionMs: Long,
    val durationMs: Long,
)

/** Backed directly by DownloadDao.observeAll(profileId)'s reactive Flow. */
sealed interface DownloadsUiState {
    data object Loading : DownloadsUiState
    data class Content(val items: List<DownloadItemUiModel>) : DownloadsUiState
    data object Empty : DownloadsUiState
    data object Error : DownloadsUiState
}

/**
 * status reuses DownloadEntity's own DownloadStatus enum directly
 * rather than wrapping it in a UI-layer equivalent -- deliberate,
 * since it is already a clean presentation-appropriate set with no
 * Room-specific baggage, unlike PlaybackException which genuinely
 * needed translation.
 */
data class DownloadItemUiModel(
    val id: Long,
    val contentId: String,
    val contentType: String,
    val title: String,
    val posterUrl: String?,
    val episodeId: String?,
    val status: com.ruby.stream.data.database.entity.DownloadStatus,
    val bytesDownloaded: Long,
    val totalBytes: Long,
)

/** Backed by PlaybackHistoryEntity. */
sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data class Content(val items: List<HistoryItemUiModel>) : HistoryUiState
    data object Empty : HistoryUiState
    data object Error : HistoryUiState
}

/**
 * episodeId stays NON-NULL here, carrying PlaybackHistoryEntity's own
 * NO_EPISODE sentinel through -- the deliberate opposite convention
 * from ContinueWatchingItemUiModel above, preserved because each
 * mirrors its own backing entity's actual invariant rather than being
 * normalized to a single UI-wide style.
 */
data class HistoryItemUiModel(
    val contentId: String,
    val contentType: String,
    val title: String,
    val posterUrl: String?,
    val episodeId: String,
    val positionMs: Long,
    val durationMs: Long,
    val playedAt: Long,
)
