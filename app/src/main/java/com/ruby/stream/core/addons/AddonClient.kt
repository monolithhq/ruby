package com.ruby.stream.core.addons

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PASS 2B — thin HTTP layer over OkHttp for add-on protocol requests.
 *
 * No Retrofit: Ruby's add-on surface is a handful of predictable
 * REST-ish GET routes (manifest.json, /{resource}/{type}/{id}.json),
 * not a large typed API surface, so a raw OkHttp wrapper avoids adding
 * an unused abstraction layer. Every method returns a raw JSON string
 * (or null on failure) — parsing is ManifestParser/AddonExecutor's job,
 * kept separate so each piece is independently testable.
 */
@Singleton
class AddonClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {

    /**
     * Fetches manifest.json from a base add-on URL. Returns null on
     * any network failure or non-2xx response — callers (AddonExecutor,
     * AddonHealthChecker) decide what that means (Offline vs. Invalid
     * Manifest vs. Add-on Error), this layer only reports success/failure.
     */
    suspend fun fetchManifest(manifestUrl: String): String? = fetchRaw(manifestUrl)

    /**
     * Fetches a resource route, e.g.
     * fetchResource(base, "stream", "movie", "tt1254207") to hit
     * /stream/movie/tt1254207.json
     *
     * id and extraPath are now properly percent-encoded (fixed Session
     * 5, while adding search support) -- the prior version interpolated
     * both raw into the URL path. That never mattered while every
     * caller only ever passed simple, URL-safe content IDs with no
     * extraPath at all, but search is the first caller passing
     * free-text user input (via extraPath = "search=$query"), which can
     * contain spaces, &, ?, or Unicode that would otherwise corrupt the
     * path. extraPath's "=" separator (e.g. "search=inception") is
     * preserved unencoded on purpose -- only the value after it is
     * encoded, keeping the key=value structure Stremio's protocol
     * expects intact while still safely encoding the value.
     */
    suspend fun fetchResource(
        baseUrl: String,
        resource: String,
        type: String,
        id: String,
        extraPath: String? = null,
    ): String? {
        val trimmedBase = baseUrl.trimEnd('/')
        val encodedId = urlEncode(id)
        val path = if (extraPath != null) {
            "$trimmedBase/$resource/$type/$encodedId/${encodeExtraPath(extraPath)}.json"
        } else {
            "$trimmedBase/$resource/$type/$encodedId.json"
        }
        return fetchRaw(path)
    }

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    /**
     * extraPath arrives as "key=value" (e.g. "search=inception & sons")
     * -- encode only the value portion, leaving the key and "="
     * structure intact, since that structure is meaningful to the
     * add-on's route parsing, not user content.
     */
    private fun encodeExtraPath(extraPath: String): String {
        val separatorIndex = extraPath.indexOf('=')
        if (separatorIndex == -1) return urlEncode(extraPath)
        val key = extraPath.substring(0, separatorIndex)
        val value = extraPath.substring(separatorIndex + 1)
        return "$key=${urlEncode(value)}"
    }

    private suspend fun fetchRaw(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                response.body?.string()
            }
        } catch (e: IOException) {
            null
        }
    }
}
