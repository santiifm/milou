package com.santiifm.milou.data.local.dao

import androidx.room.*
import com.santiifm.milou.data.local.entity.ConsoleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConsoleDao {

    @Query("SELECT * FROM consoles ORDER BY name ASC")
    fun getAllConsoles(): Flow<List<ConsoleEntity>>

    @Query("SELECT * FROM consoles WHERE manufacturerId = :manufacturerId ORDER BY name ASC")
    fun getConsolesByManufacturer(manufacturerId: String): Flow<List<ConsoleEntity>>

    /** One-shot suspend version — used for cleanup before deletion. */
    @Query("SELECT * FROM consoles WHERE manufacturerId = :manufacturerId")
    suspend fun getConsolesByManufacturerOnce(manufacturerId: String): List<ConsoleEntity>

    @Query("SELECT * FROM consoles WHERE id = :id")
    suspend fun getConsoleById(id: String): ConsoleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsole(console: ConsoleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsoles(consoles: List<ConsoleEntity>)

    @Update
    suspend fun updateConsole(console: ConsoleEntity)

    @Delete
    suspend fun deleteConsole(console: ConsoleEntity)

    @Query("DELETE FROM consoles WHERE id = :id")
    suspend fun deleteConsoleById(id: String)

    @Query("DELETE FROM consoles WHERE manufacturerId = :manufacturerId")
    suspend fun deleteConsolesByManufacturer(manufacturerId: String)

    @Query("DELETE FROM consoles")
    suspend fun clearAll()
}
