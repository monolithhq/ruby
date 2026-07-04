package com.ruby.stream.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ruby.stream.data.database.entity.InstalledAddonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InstalledAddonDao {
    @Query("SELECT * FROM installed_addons ORDER BY name ASC")
    fun observeAll(): Flow<List<InstalledAddonEntity>>

    @Query("SELECT * FROM installed_addons WHERE enabled = 1 ORDER BY name ASC")
    suspend fun findEnabled(): List<InstalledAddonEntity>

    @Query("SELECT * FROM installed_addons WHERE manifestUrl = :manifestUrl LIMIT 1")
    suspend fun findByManifestUrl(manifestUrl: String): InstalledAddonEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: InstalledAddonEntity): Long

    @Update
    suspend fun update(entity: InstalledAddonEntity)

    @Delete
    suspend fun delete(entity: InstalledAddonEntity)

    // PASS 3 addition -- AddonRepository needs a one-shot, stably-
    // ordered snapshot of every installed add-on (enabled or not; it
    // does its own enabled/health filtering per-item) to iterate for
    // parallel stream fetches. Ordered by id (Room's own autoincrement
    // insertion order) rather than name, since name order is a UI
    // display concern (Settings -> Add-ons list) unrelated to the
    // deterministic-ranking-tiebreaker concern this method serves.
    @Query("SELECT * FROM installed_addons ORDER BY id ASC")
    suspend fun getAllOrderedById(): List<InstalledAddonEntity>

}
