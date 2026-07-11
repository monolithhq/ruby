package com.ruby.stream.feature.player

import com.ruby.stream.core.player.AudioTrack
import com.ruby.stream.core.player.PlaybackState
import com.ruby.stream.core.player.SubtitleTrack

/**
 * PASS 5 — Player's top-level UI state.
 *
 * Player is the deliberate exception to AD-005/AD-006's SectionState
 * composition: it has exactly one "section" (the playback engine), and
 * PlaybackState (PASS 4) already IS that section's state machine, so
 * wrapping it in another Loading/Content/Error layer would duplicate
 * PASS 4 rather than complement it. `playback` is a direct pass-through
 * of PASS 4's PlaybackState, never redefined here (per AD-00T: UI
 * depends on Player, not on ExoPlayer directly, and never re-derives
 * what PlayerController already reports).
 *
 * resumePrompt is new at THIS layer, not part of PASS 4's own contract
 * -- "should we offer to resume?" is a PASS 6 policy decision driven by
 * Room (PlaybackHistoryEntity), not something the playback engine
 * itself knows or should know.
 */
data class PlayerUiState(
    val playback: PlaybackState,
    val overlay: PlayerOverlay,
    val availableAudioTracks: List<AudioTrack>,
    val availableSubtitleTracks: List<SubtitleTrack>,
    val playbackSpeed: Float,
    val resumePrompt: ResumePrompt?,
)

/**
 * Which overlay (if any) is currently presented above the video surface.
 *
 * Deliberately has NO PlaybackErrorSheet case. A playback failure is
 * already fully represented by PlaybackState.Error (see PlaybackModels.kt)
 * -- adding a parallel overlay case for the same fact would allow
 * contradictory states like overlay = Settings while playback = Error,
 * which this model makes structurally impossible rather than merely
 * discouraged by convention.
 *
 * EpisodeList carries its own selectedEpisodeId payload rather than a
 * parallel top-level field on PlayerUiState -- overlays carry whatever
 * data they need, instead of accumulating unrelated fields on the
 * parent state over time.
 */
sealed interface PlayerOverlay {
    data object None : PlayerOverlay
    data object Controls : PlayerOverlay
    data object Settings : PlayerOverlay
    data class EpisodeList(val selectedEpisodeId: String?) : PlayerOverlay
}

/**
 * Offered when PASS 6 determines (from PlaybackHistoryEntity) that this
 * title has a meaningful prior position to resume from. Not part of
 * PASS 4's PlaybackState -- resuming is a policy decision, not an
 * engine concern.
 */
data class ResumePrompt(
    val positionMs: Long,
    val durationMs: Long,
)
