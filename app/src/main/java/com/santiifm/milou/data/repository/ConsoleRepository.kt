package com.santiifm.milou.data.repository

import com.santiifm.milou.data.local.dao.ConsoleDao
import com.santiifm.milou.data.local.entity.ConsoleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConsoleRepository @Inject constructor(
    private val consoleDao: ConsoleDao
) {
    suspend fun clearAll() {
        consoleDao.clearAll()
    }

    fun getAllConsoles(): Flow<List<ConsoleEntity>> {
        return consoleDao.getAllConsoles()
    }

    suspend fun isDatabaseEmpty(): Boolean {
        return consoleDao.getAllConsoles().first().isEmpty()
    }

    suspend fun getConsoleById(id: String): ConsoleEntity? = consoleDao.getConsoleById(id)
}
