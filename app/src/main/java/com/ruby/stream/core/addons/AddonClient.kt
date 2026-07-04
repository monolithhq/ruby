package com.ruby.stream.core.addons

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
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
     */
    suspend fun fetchResource(
        baseUrl: String,
        resource: String,
        type: String,
        id: String,
        extraPath: String? = null,
    ): String? {
        val trimmedBase = baseUrl.trimEnd('/')
        val encodedId = id
        val path = if (extraPath != null) {
            "$trimmedBase/$resource/$type/$encodedId/$extraPath.json"
        } else {
            "$trimmedBase/$resource/$type/$encodedId.json"
        }
        return fetchRaw(path)
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
