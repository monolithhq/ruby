package com.ruby.stream.core.addons.model

import kotlinx.serialization.Serializable

/**
 * PASS 2B — grounded in PASS 2A's schema doc, Sections 2 and 3
 * (Catalog Handler response / Meta Handler response).
 */

@Serializable
data class MetaPreviewObject(
    val id: String,
    val type: ContentType,
    val name: String,
    val poster: String,
    val posterShape: String? = null,
    val genres: List<String>? = null,
    val imdbRating: String? = null,
    val releaseInfo: String? = null,
    val director: List<String>? = null,
    val cast: List<String>? = null,
    val links: List<MetaLink>? = null,
    val description: String? = null,
    val trailers: List<Trailer>? = null,
)

@Serializable
data class MetaObject(
    val id: String,
    val type: ContentType,
    val name: String,
    val genres: List<String>? = null,
    val poster: String? = null,
    val posterShape: String? = null,
    val background: String? = null,
    val logo: String? = null,
    val description: String? = null,
    val releaseInfo: String? = null,
    val director: List<String>? = null,
    val cast: List<String>? = null,
    val imdbRating: String? = null,
    val released: String? = null,
    val trailers: List<Trailer>? = null,
    val links: List<MetaLink>? = null,
    val videos: List<VideoObject>? = null,
    val runtime: String? = null,
    val language: String? = null,
    val country: String? = null,
    val awards: String? = null,
    val website: String? = null,
    val behaviorHints: MetaBehaviorHints? = null,
)

/**
 * Video ID convention for series (confirmed via Cinemeta example in
 * PASS 2A): "{metaId}:{season}:{episode}", colon-joined.
 */
@Serializable
data class VideoObject(
    val id: String,
    val title: String,
    val released: String,
    val thumbnail: String? = null,
    val streams: List<StreamObject>? = null,
    val available: Boolean? = null,
    val episode: Int? = null,
    val season: Int? = null,
    val trailers: List<Trailer>? = null,
    val overview: String? = null,
)

@Serializable
data class MetaLink(
    val name: String,
    val category: String,
    val url: String,
)

@Serializable
data class Trailer(
    val source: String? = null,
    val type: String? = null,
)

@Serializable
data class MetaBehaviorHints(
    val defaultVideoId: String? = null,
)
