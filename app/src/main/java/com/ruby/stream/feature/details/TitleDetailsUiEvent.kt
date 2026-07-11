package com.ruby.stream.feature.details

/**
 * PASS 5 — user-intent events emitted by the Title Details screen's
 * Composables.
 *
 * PlayClicked carries a single playbackId: String (movie -> metaId,
 * series -> selected videoId), NOT two separate event types
 * (PlayClicked/EpisodePlayClicked) -- the ViewModel already has
 * TitleMetaUiModel.seasons.isEmpty() to disambiguate if it ever needs
 * to, and two event types differing only by which identifier they
 * carry is exactly the kind of type proliferation that would force a
 * third/fourth event the moment Ruby adds another playable kind (live
 * channel, special). Standing pattern: prefer one event carrying an
 * opaque identifier over N events differing only by payload type, when
 * the receiving layer can already disambiguate from data it holds.
 */
sealed interface TitleDetailsUiEvent {
    data class PlayClicked(val playbackId: String) : TitleDetailsUiEvent
    data object WatchlistToggled : TitleDetailsUiEvent
    data class EpisodeClicked(val videoId: String) : TitleDetailsUiEvent
    data object RetryClicked : TitleDetailsUiEvent
}
