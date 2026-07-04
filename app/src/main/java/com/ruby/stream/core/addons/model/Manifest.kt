package com.ruby.stream.core.addons.model

import kotlinx.serialization.Serializable

/**
 * PASS 2B — grounded in PASS 2A's schema doc, Section 1 (Manifest).
 *
 * ContentType is deliberately NOT a closed enum. Stremio's type
 * vocabulary is open (movie/series/channel/tv confirmed, but add-ons
 * are free to introduce others). Modeling it as a string typealias
 * means an unfamiliar type value parses fine instead of crashing or
 * requiring a Ruby update.
 */
typealias ContentType = String

@Serializable
data class Manifest(
    val id: String,
    val version: String,
    val name: String,
    val description: String,
    val resources: List<ManifestResource> = emptyList(),
    val types: List<ContentType> = emptyList(),
    val catalogs: List<CatalogDefinition> = emptyList(),
    val idPrefixes: List<String>? = null,
    val addonCatalogs: List<CatalogDefinition>? = null,
    val background: String? = null,
    val logo: String? = null,
    val contactEmail: String? = null,
    val behaviorHints: ManifestBehaviorHints? = null,
    val config: List<ManifestConfig>? = null,
)

/**
 * resources can be plain strings (e.g. "stream") or objects with
 * per-resource overrides. Modeled as a sealed type so ManifestParser
 * can normalize both JSON shapes into one thing callers deal with.
 */
@Serializable
sealed class ManifestResource {
    @Serializable
    data class Simple(val name: String) : ManifestResource()

    @Serializable
    data class Detailed(
        val name: String,
        val types: List<ContentType>? = null,
        val idPrefixes: List<String>? = null,
    ) : ManifestResource()
}

@Serializable
data class CatalogDefinition(
    val type: ContentType,
    val id: String,
    val name: String,
    val extra: List<CatalogExtra>? = null,
)

@Serializable
data class CatalogExtra(
    val name: String,
    val isRequired: Boolean? = null,
    val options: List<String>? = null,
    val optionsLimit: Int? = null,
)

@Serializable
data class ManifestBehaviorHints(
    val adult: Boolean? = null,
    val p2p: Boolean? = null,
    val configurable: Boolean? = null,
    val configurationRequired: Boolean? = null,
)

@Serializable
data class ManifestConfig(
    val key: String,
    val type: String,
    val default: String? = null,
    val title: String? = null,
    val options: List<String>? = null,
    val required: Boolean? = null,
)
