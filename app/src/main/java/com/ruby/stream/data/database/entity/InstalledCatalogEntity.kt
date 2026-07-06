package com.ruby.stream.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * PASS 3/5 (added Session 5, during Search scoping) -- persists ONLY
 * the manifest metadata AddonRepository.search() actually needs:
 * which catalogs an add-on has, their type/id, and whether each
 * declares the "search" extra. Deliberately NOT a cache of the full
 * parsed Manifest -- resources, idPrefixes, behaviorHints, logo, etc.
 * are irrelevant once installation has completed, and caching all of
 * it would couple this table to every future Manifest model change
 * for no runtime benefit.
 *
 * Populated at install/update time only (ManifestParser parses the
 * full manifest, installation extracts these descriptors from
 * Manifest.catalogs and persists them here) or by a future explicit
 * "refresh add-ons" action. Deliberately NOT refreshed by
 * AddonHealthChecker -- health checks answer "is this add-on
 * reachable", manifest/capability refresh answers "have this add-on's
 * capabilities changed", and those stay separate operations. Routine
 * search() calls never touch the network for this data, only this
 * table -- see AddonRepository.search() for the consuming query.
 *
 * One row per (addon, catalog) -- an add-on can have zero, one, or
 * several catalogs, so this is a real one-to-many table rather than a
 * serialized column on InstalledAddonEntity, letting search() filter
 * with SQL (WHERE supportsSearch = 1) instead of deserializing every
 * add-on's full catalog list just to check one flag.
 */
@Entity(
    tableName = "installed_catalogs",
    foreignKeys = [
        ForeignKey(
            entity = InstalledAddonEntity::class,
            parentColumns = ["id"],
            childColumns = ["addonId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["addonId"])],
    primaryKeys = ["addonId", "type", "catalogId"],
)
data class InstalledCatalogEntity(
    val addonId: Long,
    val type: String,
    val catalogId: String,
    val name: String,
    val supportsSearch: Boolean,
)
