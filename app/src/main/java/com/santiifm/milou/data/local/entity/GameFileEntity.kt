package com.santiifm.milou.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "game_files",
    foreignKeys = [
        ForeignKey(
            entity = DownloadableFileEntity::class,
            parentColumns = ["id"],
            childColumns = ["fileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("fileId")]
)
data class GameFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileId: Long,
    val localPath: String,
    val hash: String?,
    val region: String?,
    val version: String?
)
