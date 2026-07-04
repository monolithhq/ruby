package com.ruby.stream.core.addons

import com.ruby.stream.core.addons.model.Manifest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * PASS 2B — parses a raw manifest.json response body into a Manifest.
 *
 * Deliberately separate from AddonClient (network) so parsing can be
 * unit-tested against fixed JSON strings without any network dependency.
 */
class ManifestParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Returns null (not a thrown exception) if the manifest is
     * malformed. Per the PASS 2B decision on graceful degradation: a
     * single bad manifest should not crash the caller, it should be
     * treated as "this add-on is currently unusable" — surfaced by
     * AddonHealthChecker as an Invalid Manifest state, not a crash.
     */
    fun parse(rawJson: String): Manifest? {
        return try {
            json.decodeFromString(Manifest.serializer(), rawJson)
        } catch (e: SerializationException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
