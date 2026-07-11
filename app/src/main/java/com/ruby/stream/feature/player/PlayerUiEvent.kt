package com.ruby.stream.feature.player

/**
 * PASS 5 — user-intent events emitted by the Player screen's Composables,
 * consumed by PASS 6's PlayerViewModel.
 *
 * Kept as thin intent signals only -- no policy, no derived decisions.
 * SeekTo carries an absolute target position rather than a relative
 * delta, since PlayerController's own seek contract (PASS 4) is
 * absolute-position-based; translating a relative scrub gesture into an
 * absolute target is a PASS 7 Composable concern, not this event's.
 */
sealed interface PlayerUiEvent {
    data object PlayPauseClicked : PlayerUiEvent
    data class SeekTo(val positionMs: Long) : PlayerUiEvent
    data object SkipForwardClicked : PlayerUiEvent
    data object SkipBackwardClicked : PlayerUiEvent

    data class AudioTrackSelected(val trackId: String) : PlayerUiEvent
    data class SubtitleTrackSelected(val trackId: String?) : PlayerUiEvent
    data class PlaybackSpeedChanged(val speed: Float) : PlayerUiEvent

    data class OverlayRequested(val overlay: PlayerOverlay) : PlayerUiEvent
    data object OverlayDismissed : PlayerUiEvent

    data class EpisodeSelected(val episodeId: String) : PlayerUiEvent

    data object ResumeConfirmed : PlayerUiEvent
    data object ResumeDismissed : PlayerUiEvent

    data object RetryClicked : PlayerUiEvent
}
