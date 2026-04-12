package com.santiifm.milou.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.santiifm.milou.data.local.entity.DownloadableFileEntity
import com.santiifm.milou.data.local.entity.FileTagEntity

@Dao
interface DownloadableFileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<DownloadableFileEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<FileTagEntity>)

    @Query("""
        SELECT df.id, df.name, df.fileName, df.consoleId, df.downloadUrl, df.fileSize,
               df.fileExtension, df.torrentFileIndex, df.torrentMagnet,
               GROUP_CONCAT(t.tag, '|') as tags
        FROM downloadable_files df
        LEFT JOIN downloadable_file_tags t ON df.id = t.fileId
        JOIN consoles c ON df.consoleId = c.id
        JOIN manufacturers m ON c.manufacturerId = m.id
        WHERE (:query = '*' OR df.name LIKE '%' || :query || '%')
          AND (:manufacturer IS NULL OR m.name = :manufacturer)
          AND (:consoleIdsCount = 0 OR df.consoleId IN (:consoleIds))
          AND (:tagsCount = 0 OR df.id IN (
                SELECT t2.fileId
                FROM downloadable_file_tags t2
                WHERE t2.tag IN (:tags)
                GROUP BY t2.fileId
                HAVING (:matchAllTags = 0 AND COUNT(DISTINCT t2.tag) >= 1)
                   OR (:matchAllTags = 1 AND COUNT(DISTINCT t2.tag) = :tagsCount)
          ))
        GROUP BY df.id, df.name, df.fileName, df.consoleId, df.downloadUrl, df.fileSize,
                 df.fileExtension, df.torrentFileIndex, df.torrentMagnet
        ORDER BY
            CASE WHEN :sortAsc = 1 THEN df.name END ASC,
            CASE WHEN :sortAsc = 0 THEN df.name END DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun queryFilesWithTags(
        query: String,
        manufacturer: String?,
        consoleIds: List<String>,
        consoleIdsCount: Int,
        tags: List<String>,
        tagsCount: Int,
        matchAllTags: Boolean,
        sortAsc: Boolean,
        limit: Int = 100,
        offset: Int = 0
    ): List<DownloadableFileWithTagsResult>

    @Query("SELECT COUNT(*) FROM downloadable_files")
    suspend fun getFilesCount(): Int

    @Query("""
        SELECT DISTINCT t.tag
        FROM downloadable_file_tags t
        JOIN downloadable_files df ON t.fileId = df.id
        JOIN consoles c ON df.consoleId = c.id
        JOIN manufacturers m ON c.manufacturerId = m.id
        WHERE (:query = '*' OR df.name LIKE '%' || :query || '%')
          AND (:manufacturer IS NULL OR m.name = :manufacturer)
          AND (:consoleIdsCount = 0 OR df.consoleId IN (:consoleIds))
        ORDER BY t.tag ASC
    """)
    suspend fun getAvailableTags(
        query: String,
        manufacturer: String?,
        consoleIds: List<String>,
        consoleIdsCount: Int
    ): List<String>

    @Query("""
        SELECT DISTINCT c.id, c.name, c.manufacturerId, c.urls, COUNT(df.id) as fileCount
        FROM consoles c
        JOIN downloadable_files df ON c.id = df.consoleId
        JOIN manufacturers m ON c.manufacturerId = m.id
        WHERE (:query = '*' OR df.name LIKE '%' || :query || '%')
          AND (:manufacturer IS NULL OR m.name = :manufacturer)
        GROUP BY c.id, c.name, c.manufacturerId, c.urls
        HAVING fileCount > 0
        ORDER BY c.name ASC
    """)
    suspend fun getConsolesWithFiles(
        query: String,
        manufacturer: String?
    ): List<ConsoleWithFileCount>

    @Transaction
    suspend fun insertFilesWithTags(files: List<DownloadableFileEntity>, tags: List<FileTagEntity>) {
        val ids = insertAll(files)
        val tagsWithIds = tags.mapIndexed { index, tag ->
            tag.copy(fileId = ids.getOrNull(index) ?: 0L)
        }
        insertTags(tagsWithIds)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(files: List<DownloadableFileEntity>): List<Long>

    @Query("DELETE FROM downloadable_files")
    suspend fun clearAll()

    @Query("DELETE FROM downloadable_file_tags WHERE fileId IN (SELECT id FROM downloadable_files WHERE consoleId = :consoleId)")
    suspend fun deleteTagsByConsoleId(consoleId: String)

    @Query("DELETE FROM downloadable_files WHERE consoleId = :consoleId")
    suspend fun deleteFilesByConsoleIdInternal(consoleId: String)

    @Transaction
    suspend fun deleteFilesByConsoleId(consoleId: String) {
        deleteTagsByConsoleId(consoleId)
        deleteFilesByConsoleIdInternal(consoleId)
    }
}

data class DownloadableFileWithTagsResult(
    val id: Long,
    val name: String,
    val fileName: String,
    val consoleId: String,
    val downloadUrl: String,
    val fileSize: Long,
    val fileExtension: String,
    val torrentFileIndex: Int?,
    val torrentMagnet: String?,
    val tags: String?
)

data class ConsoleWithFileCount(
    val id: String,
    val name: String,
    val manufacturerId: String,
    val urls: String,
    val fileCount: Int
)
