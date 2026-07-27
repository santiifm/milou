package com.santiifm.milou.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_metadata")
data class GameMetadataEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val releaseDate: Long?,
    val rating: Float?,
    val coverUrl: String?,
    val localCoverPath: String?,
    val developer: String?,
    val publisher: String?,
    val genres: String, // Comma-separated strings
    val source: String,
    val confidence: Float
)
