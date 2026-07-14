package com.ruby.stream.core.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.content.Context
import javax.inject.Inject

/**
 * PASS 4 — Player Layer. Owns the ExoPlayer instance for one playback
 * session, translates Media3's callback/exception surface into Ruby's
 * own PlaybackState/PlaybackError, and exposes nothing beyond the
 * androidx.media3.common.Player interface for rendering (PlayerSurface
 * in PASS 7 depends only on that interface, never on ExoPlayer itself).
 *
 * Scope: bound @ViewModelScoped in PASS 8, not @Singleton. Ruby is a
 * movie streaming app, not a background-audio app -- leaving the
 * Player screen should release decoders, the network connection, and
 * video surfaces, not keep an ExoPlayer instance warm indefinitely.
 * See SOT "DI lifetime" decision.
 *
 * Explicitly excluded from this class, per the PASS 4 / PASS 6
 * boundary locked in the SOT:
 *   - No Room / PlaybackHistoryDao / PlaybackHistoryEntity reference.
 *     This class doesn't know a database exists.
 *   - No ViewModel, no Compose, no navigation.
 *   - No save-cadence or resume-decision logic -- PASS 6 observes
 *     playbackState and decides what and when to persist.
 *
 * Session 5 additions: playback speed and audio/subtitle track
 * selection, following the same reasoning as seekTo -- these are
 * properties of the playback engine itself, not application behavior,
 * so PASS 4 is where they belong. Explicitly NOT added: skip-intro/
 * recap (no Stremio protocol field carries these timestamps -- no
 * data source to build it on, rejected outright not deferred) and
 * video-quality selection (Stremio streams are already-resolved
 * single URLs per source; quality choice belongs at PASS 3's
 * stream-selection screen, not inside the player).
 */
interface PlayerController {
    /**
     * The playback engine, exposed only as the Media3 interface.
     * PlayerSurface (PASS 7) depends on this type, never on ExoPlayer,
     * so the implementation stays swappable and the UI can't
     * accidentally reach for an ExoPlayer-specific API.
     */
    val player: Player

    /** Immutable to every consumer outside this class. */
    val playbackState: StateFlow<PlaybackState>

    /**
     * Loads source into the player and begins buffering, but does NOT
     * start playback -- call play() once ready. Kept separate from
     * play() so future behaviors (auto-play off, a resume-confirmation
     * prompt, pre-buffering while a dialog is still showing) don't
     * require an API change later.
     *
     * Not suspend: ExoPlayer.setMediaItem/prepare are fire-and-forget
     * calls into Media3's own internal handler thread -- nothing here
     * actually suspends. Readiness is communicated asynchronously via
     * playbackState (Preparing -> Buffering -> Playing/Paused), not
     * via this call returning. Callers do not need a coroutine scope
     * just to invoke this.
     *
     * source.url is expected to already be a resolved, playable URL --
     * choosing between a StreamObject's url/ytId/externalUrl fields and
     * carrying forward behaviorHints.proxyHeaders is
     * PlaybackUrlResolver's responsibility (PASS 6, AD-026), not this
     * controller's. PlayerController has no knowledge of StreamObject
     * at all.
     *
     * WIDENED (AD-026, PASS 6): previously took a bare streamUrl:
     * String. source.headers, when non-empty, are applied by
     * reconfiguring this controller's DataSource.Factory before
     * calling exoPlayer.prepare() -- Media3 applies custom HTTP headers
     * at the DataSource.Factory level, not per-MediaItem (confirmed
     * against Media3's own customization documentation), so headers
     * cannot simply be attached to the MediaItem itself.
     */
    fun prepare(source: PlaybackSource)

    /** Starts or resumes playback. No-op if already playing. */
    fun play()

    /** Pauses playback. No-op if already paused. */
    fun pause()

    /**
     * Seeks to positionMs. Safe to call in any state, including before
     * playback has started or while paused -- delegates directly to
     * Media3, which already defines this behavior. No extra state
     * machine is needed here.
     */
    fun seekTo(positionMs: Long)

    /**
     * Sets playback speed as a multiplier (1.0 = normal). Delegates
     * directly to Media3's PlaybackParameters; pitch is left
     * unadjusted (Media3 default). Safe in any state, same as seekTo.
     */
    fun setPlaybackSpeed(speed: Float)

    /**
     * Currently available audio tracks for the prepared media, or an
     * empty list before prepare() reaches STATE_READY or if the
     * source exposes only one. A snapshot, not a Flow -- track
     * availability only changes on prepare/source change, not
     * continuously like position does. PASS 7 should re-call this
     * when it first observes PlaybackState.Paused/Playing after a
     * prepare(), not poll it continuously.
     *
     * Track ids are session-local ("$groupIndex:$trackIndex"), valid
     * only until the next prepare() call -- see selectAudioTrack.
     * Callers checking whether a selector UI should be shown at all
     * can simply check getAudioTracks().size > 1; no separate
     * hasMultipleAudioTracks() is exposed for this.
     */
    fun getAudioTracks(): List<AudioTrack>

    /**
     * Selects an audio track by AudioTrack.id from the most recent
     * getAudioTracks() call. ids are only valid for the current
     * prepared media -- do not cache one across a new prepare() call.
     */
    fun selectAudioTrack(id: String)

    /** Same contract as getAudioTracks(), for subtitle/text tracks. */
    fun getSubtitleTracks(): List<SubtitleTrack>

    /**
     * Selects a subtitle track by SubtitleTrack.id, or pass null to
     * turn subtitles off entirely. Same id-lifetime contract as
     * selectAudioTrack.
     */
    fun selectSubtitleTrack(id: String?)

    /**
     * Releases the underlying ExoPlayer instance: decoders, network
     * connection, and video surfaces. Called when leaving the Player
     * screen. The controller is not reusable after this call.
     */
    fun release()
}

/**
 * Default implementation. Polling (see SOT "Polling" decision) is a
 * pure implementation detail: consumers only ever see playbackState
 * updates over time and have no way to know whether those updates are
 * driven by polling, listener callbacks, or some future Media3 push
 * API.
 *
 * Ticker lifecycle:
 *   - starts when Player.Listener observes STATE_READY && isPlaying
 *   - emits Playing(positionMs, durationMs) every 500ms while running
 *   - stops immediately on pause / ended / error / release
 *   - restarts on resume
 *
 * 500ms chosen as a default: smooth enough for a progress bar,
 * negligible CPU overhead, and easy to tighten later (e.g. 250ms
 * during active scrubbing) without changing the public API, since
 * callers never see the interval directly.
 */
@OptIn(UnstableApi::class)
class DefaultPlayerController @Inject constructor(
    @ApplicationContext context: Context,
    @com.ruby.stream.di.PlayerControllerScope private val controllerScope: CoroutineScope,
) : PlayerController {

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build()

    override val player: Player get() = exoPlayer

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    override val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var pollingJob: Job? = null

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        stopPolling()
                        _playbackState.value = PlaybackState.Buffering
                    }

                    Player.STATE_READY -> {
                        if (exoPlayer.isPlaying) {
                            startPolling()
                        } else {
                            stopPolling()
                            _playbackState.value = PlaybackState.Paused(
                                positionMs = exoPlayer.currentPosition,
                                durationMs = exoPlayer.duration.coerceAtLeast(0L),
                            )
                        }
                    }

                    Player.STATE_ENDED -> {
                        stopPolling()
                        _playbackState.value = PlaybackState.Ended(
                            durationMs = exoPlayer.duration.coerceAtLeast(0L),
                        )
                    }

                    Player.STATE_IDLE -> {
                        stopPolling()
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    startPolling()
                } else {
                    stopPolling()
                    // A pause fires onIsPlayingChanged(false) without a
                    // STATE_ENDED/STATE_IDLE transition, so it needs its
                    // own Paused emission here rather than relying on
                    // onPlaybackStateChanged alone.
                    if (exoPlayer.playbackState == Player.STATE_READY) {
                        _playbackState.value = PlaybackState.Paused(
                            positionMs = exoPlayer.currentPosition,
                            durationMs = exoPlayer.duration.coerceAtLeast(0L),
                        )
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                stopPolling()
                _playbackState.value = PlaybackState.Error(error.toPlaybackError())
            }
        })
    }

    override fun prepare(source: PlaybackSource) {
        _playbackState.value = PlaybackState.Preparing
        if (source.headers.isNotEmpty()) {
            applyHeaders(source.headers)
        }
        exoPlayer.setMediaItem(MediaItem.fromUri(source.url))
        exoPlayer.prepare()
    }

    /**
     * Reconfigures the player's media source factory with a
     * DataSource.Factory carrying the given request headers, per
     * Media3's documented header-injection pattern (headers live on
     * the DataSource.Factory, not the MediaItem). Rebuilt per-prepare()
     * call since different streams/add-ons may require different
     * headers (or none) -- there is no single fixed header set for the
     * lifetime of this controller.
     */
    @OptIn(UnstableApi::class)
    private fun applyHeaders(headers: Map<String, String>) {
        val dataSourceFactory = androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(headers)
        exoPlayer.setMediaSource(
            androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSourceFactory)
                .createMediaSource(androidx.media3.common.MediaItem.EMPTY)
        )
    }

    override fun play() {
        exoPlayer.play()
    }

    override fun pause() {
        exoPlayer.pause()
    }

    override fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
    }

    override fun setPlaybackSpeed(speed: Float) {
        exoPlayer.setPlaybackSpeed(speed)
    }

    override fun getAudioTracks(): List<AudioTrack> =
        mapTracks(C.TRACK_TYPE_AUDIO) { id, format, isSelected ->
            AudioTrack(
                id = id,
                language = format.language,
                label = format.label,
                isSelected = isSelected,
            )
        }

    override fun selectAudioTrack(id: String) {
        selectTrack(C.TRACK_TYPE_AUDIO, id)
    }

    override fun getSubtitleTracks(): List<SubtitleTrack> =
        mapTracks(C.TRACK_TYPE_TEXT) { id, format, isSelected ->
            SubtitleTrack(
                id = id,
                language = format.language,
                label = format.label,
                isSelected = isSelected,
            )
        }

    override fun selectSubtitleTrack(id: String?) {
        if (id == null) {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .build()
        } else {
            selectTrack(C.TRACK_TYPE_TEXT, id)
        }
    }

    override fun release() {
        // controllerScope is NOT cancelled automatically by Hilt when
        // this controller's owning ViewModelComponent is destroyed --
        // a CoroutineScope is just another injected object to Hilt,
        // not a Closeable it has a generic cleanup hook for (AD-027).
        // Explicit cancellation here is what actually stops the
        // SupervisorJob backing it, rather than relying on a DI
        // implementation detail. stopPolling() first (already stops
        // the one job actually running in this scope); the ordering
        // relative to exoPlayer.release() is not load-bearing since
        // polling has already stopped either way.
        stopPolling()
        controllerScope.cancel()
        exoPlayer.release()
    }

    private fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = controllerScope.launch {
            while (isActive) {
                _playbackState.value = PlaybackState.Playing(
                    positionMs = exoPlayer.currentPosition,
                    durationMs = exoPlayer.duration.coerceAtLeast(0L),
                )
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    /**
     * Shared walk over exoPlayer.currentTracks for a given track type.
     * id is synthesized as "$groupIndex:$trackIndex" -- valid only for
     * the currently prepared media, which is fine since
     * getAudioTracks()/getSubtitleTracks() and the matching select
     * call always happen within the same playback session, and
     * everything is invalidated on the next prepare() anyway.
     * Deliberately not Format.id, which Media3 does not guarantee is
     * present or unique across every add-on's stream.
     */
    private fun <T> mapTracks(
        trackType: Int,
        transform: (id: String, format: Format, isSelected: Boolean) -> T,
    ): List<T> {
        val groups = exoPlayer.currentTracks.groups.filter { it.type == trackType }
        return groups.mapIndexed { groupIndex, group ->
            (0 until group.length).map { trackIndex ->
                transform(
                    "$groupIndex:$trackIndex",
                    group.getTrackFormat(trackIndex),
                    group.isTrackSelected(trackIndex),
                )
            }
        }.flatten()
    }

    private fun selectTrack(trackType: Int, id: String) {
        val groups = exoPlayer.currentTracks.groups.filter { it.type == trackType }
        groups.forEachIndexed { groupIndex, group ->
            for (trackIndex in 0 until group.length) {
                if ("$groupIndex:$trackIndex" == id) {
                    exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                        .buildUpon()
                        .setOverrideForType(
                            TrackSelectionOverride(group.mediaTrackGroup, trackIndex)
                        )
                        .setTrackTypeDisabled(trackType, false)
                        .build()
                    return
                }
            }
        }
    }

    /**
     * Media3 exception -> Ruby domain error. This is the only place in
     * Ruby that ever inspects a PlaybackException; PASS 6/7 work
     * exclusively with PlaybackError.
     */
    private fun PlaybackException.toPlaybackError(): PlaybackError = when (errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
        -> PlaybackError.NETWORK

        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
        PlaybackException.ERROR_CODE_IO_NO_PERMISSION,
        PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
        -> PlaybackError.SOURCE

        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
        -> PlaybackError.DECODER

        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        -> PlaybackError.UNSUPPORTED_FORMAT

        PlaybackException.ERROR_CODE_DRM_UNSPECIFIED,
        PlaybackException.ERROR_CODE_DRM_SCHEME_UNSUPPORTED,
        PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED,
        PlaybackException.ERROR_CODE_DRM_CONTENT_ERROR,
        PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED,
        PlaybackException.ERROR_CODE_DRM_DISALLOWED_OPERATION,
        PlaybackException.ERROR_CODE_DRM_SYSTEM_ERROR,
        PlaybackException.ERROR_CODE_DRM_DEVICE_REVOKED,
        PlaybackException.ERROR_CODE_DRM_LICENSE_EXPIRED,
        -> PlaybackError.DRM

        PlaybackException.ERROR_CODE_TIMEOUT,
        -> PlaybackError.TIMEOUT

        else -> PlaybackError.UNKNOWN
    }

    private companion object {
        const val POLL_INTERVAL_MS = 500L
    }
}
