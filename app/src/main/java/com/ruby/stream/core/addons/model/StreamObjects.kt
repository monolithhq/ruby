package com.ruby.stream.core.addons.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * PASS 2B — grounded in PASS 2A's schema doc, Section 4 (Stream
 * Handler response).
 *
 * StreamObject models every official source type defined by the
 * protocol. Ruby v1 currently resolves only HTTP/HTTPS (url), YouTube
 * (ytId), and externally-delegated (externalUrl) streams — see
 * AddonExecutor/PASS 3 for playback support. Unsupported source types
 * (infoHash, nzbUrl, rarUrls/zipUrls/etc.) are still parsed here so a
 * response containing them doesn't fail to parse; they are simply
 * treated as unavailable at the point streams are ranked/played.
 * Parsing a source type is not the same as Ruby being able to play it.
 */
@Serializable
data class StreamObject(
    val url: String? = null,
    val ytId: String? = null,
    val infoHash: String? = null,
    val fileIdx: Int? = null,
    val fileMustInclude: List<String>? = null,
    val nzbUrl: String? = null,
    val rarUrls: List<StreamSource>? = null,
    val zipUrls: List<StreamSource>? = null,
    @SerialName("7zipUrls") val sevenZipUrls: List<StreamSource>? = null,
    val tgzUrls: List<StreamSource>? = null,
    val tarUrls: List<StreamSource>? = null,
    val externalUrl: String? = null,
    val name: String? = null,
    val title: String? = null,
    val description: String? = null,
    val subtitles: List<SubtitleObject>? = null,
    val sources: List<String>? = null,
    val behaviorHints: StreamBehaviorHints? = null,
) {
    /**
     * True if this StreamObject uses a source type Ruby v1 can
     * actually hand to Media3 for playback. See class doc — parsing
     * happens for all source types, but playback support is narrower.
     */
    fun isPlayableByRuby(): Boolean =
        url != null || ytId != null || externalUrl != null
}

@Serializable
data class StreamSource(
    val url: String,
    val bytes: Long? = null,
)

@Serializable
data class StreamBehaviorHints(
    val countryWhitelist: List<String>? = null,
    val notWebReady: Boolean? = null,
    val bingeGroup: String? = null,
    val proxyHeaders: StreamProxyHeaders? = null,
    val videoHash: String? = null,
    val videoSize: Long? = null,
    val filename: String? = null,
)

@Serializable
data class StreamProxyHeaders(
    val request: Map<String, String>? = null,
    val response: Map<String, String>? = null,
)

/**
 * PASS 2A, Section 6. All three fields are required per spec — a
 * Subtitle Object with a missing field is malformed and should be
 * dropped by AddonExecutor, not defaulted.
 */
@Serializable
data class SubtitleObject(
    val id: String,
    val url: String,
    val lang: String,
)
