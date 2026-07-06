package com.ruby.stream.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// DISABLED removed from this enum in PASS 3 -- InstalledAddonEntity.enabled
// (below) already models "should Ruby attempt to use this add-on at all".
// AddonHealth now answers a strictly narrower, orthogonal question: "what
// happened the last time Ruby interacted with it". See SOT "Unify health
// vocabulary" for the full reasoning.
enum class AddonHealth {
    HEALTHY,
    TIMEOUT,
    INVALID_MANIFEST,
    UNREACHABLE
}

// Unique index on manifestUrl (the add-on's natural identity) so
// @Insert(onConflict = REPLACE) in InstalledAddonDao.upsert behaves as a
// real upsert instead of allowing the same add-on to be installed twice.
@Entity(
    tableName = "installed_addons",
    indices = [Index(value = ["manifestUrl"], unique = true)]
)
data class InstalledAddonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val manifestUrl: String,
    val name: String,
    val version: String,
    val health: AddonHealth,
    val enabled: Boolean = true,

    // Owner-set trust judgment about this SOURCE, independent of
    // whether the add-on's own catalog labeling can be trusted -- see
    // AD-011. Defaults false (opt-in): a newly installed add-on shows
    // NOTHING to KIDS profiles until the Owner explicitly marks it
    // trusted, regardless of how the add-on labels its own catalogs.
    // This is the FIRST of two gates for KIDS-profile catalog
    // visibility; InstalledCatalogEntity.visibleToKids is the second,
    // finer-grained gate applied only within add-ons that already pass
    // this one. Never read from add-on-reported metadata -- add-ons are
    // third-party and cannot be trusted to self-report this honestly.
    val familyFriendly: Boolean = false,
    val lastSuccessfulValidation: Long? = null,
    val lastFailureReason: String? = null,
    val lastCheckedAt: Long? = null
)
