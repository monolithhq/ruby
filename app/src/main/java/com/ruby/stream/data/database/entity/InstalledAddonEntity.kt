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
    val lastSuccessfulValidation: Long? = null,
    val lastFailureReason: String? = null,
    val lastCheckedAt: Long? = null
)
