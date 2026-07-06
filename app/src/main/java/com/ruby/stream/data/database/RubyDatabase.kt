package com.ruby.stream.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ruby.stream.data.database.converters.RubyTypeConverters
import com.ruby.stream.data.database.dao.DownloadDao
import com.ruby.stream.data.database.dao.InstalledAddonDao
import com.ruby.stream.data.database.dao.InstalledCatalogDao
import com.ruby.stream.data.database.dao.LibraryDao
import com.ruby.stream.data.database.dao.PlaybackHistoryDao
import com.ruby.stream.data.database.dao.ProfileDao
import com.ruby.stream.data.database.entity.DownloadEntity
import com.ruby.stream.data.database.entity.InstalledAddonEntity
import com.ruby.stream.data.database.entity.InstalledCatalogEntity
import com.ruby.stream.data.database.entity.LibraryEntity
import com.ruby.stream.data.database.entity.PlaybackHistoryEntity
import com.ruby.stream.data.database.entity.ProfileEntity

@Database(
    entities = [
        LibraryEntity::class,
        PlaybackHistoryEntity::class,
        DownloadEntity::class,
        ProfileEntity::class,
        InstalledAddonEntity::class,
        InstalledCatalogEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(RubyTypeConverters::class)
abstract class RubyDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun downloadDao(): DownloadDao
    abstract fun profileDao(): ProfileDao
    abstract fun installedAddonDao(): InstalledAddonDao
    abstract fun installedCatalogDao(): InstalledCatalogDao
}
