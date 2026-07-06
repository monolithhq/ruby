package com.ruby.stream.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ruby.stream.data.database.entity.InstalledCatalogEntity

@Dao
interface InstalledCatalogDao {

    /**
     * The one query AddonRepository.search() actually needs: for a
     * given addonId and content type, which catalogs declare search
     * support. Filtering supportsSearch in SQL rather than in Kotlin
     * after a full-table read is the whole reason this is a real
     * table instead of a serialized column.
     */
    @Query(
        "SELECT * FROM installed_catalogs WHERE addonId = :addonId AND type = :type AND supportsSearch = 1"
    )
    suspend fun getSearchableCatalogs(addonId: Long, type: String): List<InstalledCatalogEntity>

    @Query("SELECT * FROM installed_catalogs WHERE addonId = :addonId")
    suspend fun getAllForAddon(addonId: Long): List<InstalledCatalogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<InstalledCatalogEntity>)

    @Query("DELETE FROM installed_catalogs WHERE addonId = :addonId")
    suspend fun deleteAllForAddon(addonId: Long)

    /**
     * Called at install/update time (and by any future explicit
     * "refresh add-ons" action) -- never by routine search() calls or
     * AddonHealthChecker. Replaces this add-on's entire catalog
     * descriptor set atomically, since a manifest update may add,
     * remove, or change catalogs rather than just editing one in
     * place.
     */
    @Transaction
    suspend fun replaceForAddon(addonId: Long, entities: List<InstalledCatalogEntity>) {
        deleteAllForAddon(addonId)
        insertAll(entities)
    }
}
