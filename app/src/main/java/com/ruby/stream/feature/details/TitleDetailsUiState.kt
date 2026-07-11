package com.ruby.stream.feature.details

/**
 * PASS 5 — Title Details' top-level UI state.
 *
 * Single-purpose screen (one meta lookup via AddonRepository.getMeta()),
 * plain Loading/Content/Empty/Error, not SectionState<T> composition --
 * same category as Stream Selection, not Home/Library/Search.
 */
sealed interface TitleDetailsUiState {
    data object Loading : TitleDetailsUiState
    data class Content(
        val meta: TitleMetaUiModel,
        val libraryStatus: LibraryStatus,
    ) : TitleDetailsUiState
    data object Error : TitleDetailsUiState
}

/**
 * Modeled against the real MetaObject/VideoObject shapes (checked
 * directly, not assumed). Movies and series were initially drafted as
 * two separate sealed subclasses but collapsed into ONE model with
 * seasons: List<SeasonUiModel> (empty = movie, non-empty = series) --
 * the only real divergence the protocol exposes is episode presence;
 * nine-plus identical fields do not justify two types.
 *
 * runtime stays nullable and is simply always null for series, since
 * VideoObject carries no per-episode runtime field either -- noted
 * explicitly rather than left ambiguous.
 */
data class TitleMetaUiModel(
    val id: String,
    val name: String,
    val poster: String?,
    val background: String?,
    val description: String?,
    val genres: List<String>,
    val cast: List<String>,
    val director: List<String>,
    val releaseInfo: String?,
    val imdbRating: String?,
    val runtime: String?,
    val seasons: List<SeasonUiModel>,
)

data class SeasonUiModel(
    val seasonNumber: Int,
    val episodes: List<EpisodeUiModel>,
)

data class EpisodeUiModel(
    val videoId: String,
    val episodeNumber: Int,
    val title: String,
    val thumbnail: String?,
    val overview: String?,
    val released: String,
)

/**
 * Combines LibraryEntity data alongside MetaObject on Content, since
 * Title Details needs both to render correct action-button state
 * (Play vs. Resume, Add vs. Remove from Watchlist).
 *
 * Deliberately does NOT include a streamSelection: SectionState<Unit>
 * field -- a loading state that exists only for the duration of one
 * navigation frame before this screen is destroyed isn't useful state,
 * it's a race with the nav transition. Navigation boundary: Title
 * Details owns WHAT the user wants to play (decides a target
 * identifier); Stream Selection owns HOW to play it.
 */
data class LibraryStatus(
    val inWatchlist: Boolean,
    val continueWatchingPositionMs: Long?,
)
