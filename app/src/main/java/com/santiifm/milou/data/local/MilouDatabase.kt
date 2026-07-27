package com.santiifm.milou.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.santiifm.milou.data.local.dao.ConsoleDao
import com.santiifm.milou.data.local.dao.DownloadableFileDao
import com.santiifm.milou.data.local.dao.JobDao
import com.santiifm.milou.data.local.dao.ManufacturerDao
import com.santiifm.milou.data.local.entity.ConsoleEntity
import com.santiifm.milou.data.local.entity.DownloadableFileEntity
import com.santiifm.milou.data.local.entity.FileTagEntity
import com.santiifm.milou.data.local.entity.JobEntity
import com.santiifm.milou.data.local.entity.ManufacturerEntity
import com.santiifm.milou.data.local.entity.GameMetadataEntity
import com.santiifm.milou.data.local.entity.GameFileEntity
import com.santiifm.milou.data.local.queries.DownloadableFileFts

@Database(
    entities = [
        ManufacturerEntity::class, 
        ConsoleEntity::class, 
        DownloadableFileEntity::class, 
        FileTagEntity::class, 
        DownloadableFileFts::class, 
        JobEntity::class,
        GameMetadataEntity::class,
        GameFileEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class MilouDatabase : RoomDatabase() {
    abstract fun downloadableFileDao(): DownloadableFileDao
    abstract fun consoleDao(): ConsoleDao
    abstract fun manufacturerDao(): ManufacturerDao
    abstract fun jobDao(): JobDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE downloadable_files ADD COLUMN torrentFileIndex INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE downloadable_files ADD COLUMN torrentMagnet TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS jobs (
                        id TEXT PRIMARY KEY NOT NULL,
                        type TEXT NOT NULL,
                        status TEXT NOT NULL,
                        progress REAL NOT NULL,
                        payload TEXT NOT NULL,
                        error TEXT,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS game_metadata (
                        id TEXT PRIMARY KEY NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT,
                        releaseDate INTEGER,
                        rating REAL,
                        coverUrl TEXT,
                        localCoverPath TEXT,
                        developer TEXT,
                        publisher TEXT,
                        genres TEXT NOT NULL,
                        source TEXT NOT NULL,
                        confidence REAL NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS game_files (
                        id INTEGER PRIMARY KEY AUTO_INCREMENT NOT NULL,
                        fileId INTEGER NOT NULL,
                        localPath TEXT NOT NULL,
                        hash TEXT,
                        region TEXT,
                        version TEXT,
                        FOREIGN KEY(fileId) REFERENCES downloadable_files(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                
                db.execSQL("CREATE INDEX IF NOT EXISTS index_game_files_fileId ON game_files(fileId)")
                
                db.execSQL("ALTER TABLE downloadable_files ADD COLUMN metadataId TEXT")
            }
        }
    }
}
