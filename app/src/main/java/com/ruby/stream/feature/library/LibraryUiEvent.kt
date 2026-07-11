package com.ruby.stream.feature.library

/**
 * PASS 5 — user-intent events emitted by the Library screen's
 * Composables. Tab-switching is shell-level; each tab's own
 * item-click/retry/action events are scoped separately below since
 * each tab is backed by a genuinely different query and item shape.
 */
sealed interface LibraryUiEvent {
    data class TabSelected(val tab: LibraryTab) : LibraryUiEvent
}

sealed interface WatchlistUiEvent {
    data class ItemClicked(val contentId: String) : WatchlistUiEvent
    data class RemoveClicked(val contentId: String) : WatchlistUiEvent
    data object RetryClicked : WatchlistUiEvent
}

sealed interface ContinueWatchingUiEvent {
    data class ItemClicked(val contentId: String, val episodeId: String?) : ContinueWatchingUiEvent
    data class RemoveClicked(val contentId: String, val episodeId: String?) : ContinueWatchingUiEvent
    data object RetryClicked : ContinueWatchingUiEvent
}

sealed interface DownloadsUiEvent {
    data class ItemClicked(val downloadId: Long) : DownloadsUiEvent
    data class PauseClicked(val downloadId: Long) : DownloadsUiEvent
    data class ResumeClicked(val downloadId: Long) : DownloadsUiEvent
    data class RetryDownloadClicked(val downloadId: Long) : DownloadsUiEvent
    data class DeleteClicked(val downloadId: Long) : DownloadsUiEvent
    data object RetryClicked : DownloadsUiEvent
}

sealed interface HistoryUiEvent {
    data class ItemClicked(val contentId: String, val episodeId: String) : HistoryUiEvent
    data class RemoveClicked(val contentId: String, val episodeId: String) : HistoryUiEvent
    data object RetryClicked : HistoryUiEvent
}
