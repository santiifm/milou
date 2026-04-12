package com.santiifm.milou.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.santiifm.milou.data.local.dao.ConsoleDao
import com.santiifm.milou.data.local.dao.DownloadableFileDao
import com.santiifm.milou.data.local.dao.ManufacturerDao
import com.santiifm.milou.data.local.entity.ConsoleEntity
import com.santiifm.milou.data.local.entity.DownloadableFileEntity
import com.santiifm.milou.data.local.entity.FileTagEntity
import com.santiifm.milou.data.local.entity.ManufacturerEntity
import com.santiifm.milou.data.local.queries.DownloadableFileFts

@Database(
    entities = [ManufacturerEntity::class, ConsoleEntity::class, DownloadableFileEntity::class, FileTagEntity::class, DownloadableFileFts::class],
    version = 2,
    exportSchema = false
)
abstract class MilouDatabase : RoomDatabase() {
    abstract fun downloadableFileDao(): DownloadableFileDao
    abstract fun consoleDao(): ConsoleDao
    abstract fun manufacturerDao(): ManufacturerDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE downloadable_files ADD COLUMN torrentFileIndex INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE downloadable_files ADD COLUMN torrentMagnet TEXT DEFAULT NULL")
            }
        }
    }
}
