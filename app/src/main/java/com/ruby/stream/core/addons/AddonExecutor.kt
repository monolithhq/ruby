package com.ruby.stream.core.addons

import com.ruby.stream.core.addons.model.Manifest
import com.ruby.stream.core.addons.model.MetaObject
import com.ruby.stream.core.addons.model.MetaPreviewObject
import com.ruby.stream.core.addons.model.StreamObject
import com.ruby.stream.core.addons.model.SubtitleObject
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Distinguishes why getManifestResult couldn't return a Manifest, so
 * callers can tell "add-on is unreachable" apart from "add-on responded
 * but sent something we can't parse" -- these map to different health
 * states (TIMEOUT vs. INVALID_MANIFEST) and different UI error states.
 */
sealed class ManifestFetchResult {
    data class Success(val manifest: Manifest) : ManifestFetchResult()
    data object Unreachable : ManifestFetchResult()
    data object Malformed : ManifestFetchResult()
}

/**
 * PASS 2B — orchestrates real catalog/meta/stream/subtitles requests
 * against a single add-on and normalizes the results.
 *
 * Error-handling policy (locked during PASS 2B scoping): degrade
 * gracefully. A response that fails to parse AT ALL (bad JSON, wrong
 * top-level shape) yields an empty result for that call. A response
 * that parses but contains individual malformed items (e.g. one
 * Subtitle Object missing "lang") drops just that item and keeps the
 * rest — one bad item from one add-on should never blank out an
 * otherwise-good response. This matches PASS 2A's grounding note that
 * real add-ons may not strictly conform to the spec.
 *
 * Whole-request failure (network down, non-2xx, totally unparseable)
 * is surfaced as null so callers (PASS 3's repository layer) can
 * distinguish "this add-on returned nothing" from "this add-on is
 * unreachable" — feeding Ruby's three distinct error states (No
 * Streams / Add-on Error / Offline).
 */
@Singleton
class AddonExecutor @Inject constructor(
    private val addonClient: AddonClient,
    private val manifestParser: ManifestParser,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Distinguishes "couldn't reach the add-on" from "reached it, but
     * the manifest was garbage" -- AddonHealthChecker needs this split
     * to report TIMEOUT vs. INVALID_MANIFEST correctly instead of
     * collapsing both into one null.
     */
    suspend fun getManifestResult(manifestUrl: String): ManifestFetchResult {
        val raw = addonClient.fetchManifest(manifestUrl)
            ?: return ManifestFetchResult.Unreachable
        val manifest = manifestParser.parse(raw)
            ?: return ManifestFetchResult.Malformed
        return ManifestFetchResult.Success(manifest)
    }

    /**
     * Convenience wrapper over getManifestResult for callers (e.g.
     * PASS 3's repository layer) that only care whether a manifest was
     * obtained, not why it failed if not.
     */
    suspend fun getManifest(manifestUrl: String): Manifest? =
        (getManifestResult(manifestUrl) as? ManifestFetchResult.Success)?.manifest

    suspend fun getCatalog(
        baseUrl: String,
        type: String,
        catalogId: String,
        extraPath: String? = null,
    ): List<MetaPreviewObject>? {
        val raw = addonClient.fetchResource(baseUrl, "catalog", type, catalogId, extraPath)
            ?: return null
        return parseListField(raw, "metas") { element ->
            json.decodeFromJsonElement(MetaPreviewObject.serializer(), element)
        }
    }

    suspend fun getMeta(baseUrl: String, type: String, id: String): MetaObject? {
        val raw = addonClient.fetchResource(baseUrl, "meta", type, id) ?: return null
        return try {
            val root = json.parseToJsonElement(raw).jsonObject
            val metaElement = root["meta"] ?: return null
            json.decodeFromJsonElement(MetaObject.serializer(), metaElement)
        } catch (e: SerializationException) {
            null
        } catch (e: IllegalStateException) {
            null
        }
    }

    suspend fun getStreams(baseUrl: String, type: String, id: String): List<StreamObject>? {
        val raw = addonClient.fetchResource(baseUrl, "stream", type, id) ?: return null
        return parseListField(raw, "streams") { element ->
            json.decodeFromJsonElement(StreamObject.serializer(), element)
        }
    }

    suspend fun getSubtitles(baseUrl: String, type: String, id: String): List<SubtitleObject>? {
        val raw = addonClient.fetchResource(baseUrl, "subtitles", type, id) ?: return null
        return parseListField(raw, "subtitles") { element ->
            json.decodeFromJsonElement(SubtitleObject.serializer(), element)
        }
    }

    /**
     * Parses a top-level JSON object's array field, dropping individual
     * elements that fail to decode rather than failing the whole list.
     * Returns null only if the response isn't valid JSON or the named
     * field is missing/not an array at all (a structurally broken
     * response, not just a dirty item).
     */
    private inline fun <T> parseListField(
        raw: String,
        fieldName: String,
        decodeItem: (kotlinx.serialization.json.JsonElement) -> T,
    ): List<T>? {
        val root: JsonObject = try {
            json.parseToJsonElement(raw).jsonObject
        } catch (e: SerializationException) {
            return null
        } catch (e: IllegalStateException) {
            return null
        }

        val arrayElement = root[fieldName] ?: return null
        val items = try {
            arrayElement.jsonArray
        } catch (e: IllegalArgumentException) {
            return null
        }

        return items.mapNotNull { element ->
            try {
                decodeItem(element)
            } catch (e: SerializationException) {
                null
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}
