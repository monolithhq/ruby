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
 *
 * visibleToKids (added AD-011, Session 7) is the SECOND of two gates
 * for KIDS-profile catalog visibility, and reuses this table rather
 * than a new one because it operates at exactly the granularity the
 * protocol itself exposes -- a single add-on can expose both a "Disney
 * Kids" and a "Disney Marvel" catalog, and only the catalog layer can
 * distinguish them (per Stremio's own real-world profile model, which
 * configures catalog visibility per profile, not per add-on). The
 * FIRST gate is InstalledAddonEntity.familyFriendly -- a KIDS profile
 * must clear that Owner-level trust gate before this per-catalog flag
 * is even consulted; an untrusted add-on's catalogs are hidden from
 * KIDS profiles regardless of this column's value. Defaults false
 * (opt-in): even on a newly trusted add-on, no catalog is shown to
 * KIDS profiles until the Owner explicitly picks which ones -- the
 * safer failure mode, matching familyFriendly's own default. Ordinary
 * (non-KIDS) profiles ignore this column entirely.
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
    val visibleToKids: Boolean = false,
)
