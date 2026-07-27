package com.santiifm.milou.domain.usecase

import com.santiifm.milou.domain.model.FilterMode
import com.santiifm.milou.domain.model.Game
import com.santiifm.milou.domain.model.SearchCriteria
import com.santiifm.milou.domain.repository.SearchRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchLibraryUseCaseTest {

    private class FakeSearchRepository : SearchRepository {
        var lastCriteria: SearchCriteria? = null
        var lastLimit: Int = -1
        var lastOffset: Int = -1

        override suspend fun search(criteria: SearchCriteria, limit: Int, offset: Int): List<Game> {
            lastCriteria = criteria
            lastLimit = limit
            lastOffset = offset
            return emptyList()
        }
    }

    private val repository = FakeSearchRepository()
    private val useCase = SearchLibraryUseCase(repository)

    @Test
    fun `invoke should normalize query`() = runBlocking {
        val criteria = SearchCriteria(query = "  ZELDA  ")
        useCase(criteria, page = 0, pageSize = 20)

        assertEquals("zelda", repository.lastCriteria?.query)
    }

    @Test
    fun `invoke should calculate offset correctly for page 0`() = runBlocking {
        val criteria = SearchCriteria()
        useCase(criteria, page = 0, pageSize = 20)

        assertEquals(0, repository.lastOffset)
        assertEquals(20, repository.lastLimit)
    }

    @Test
    fun `invoke should calculate offset correctly for page 1`() = runBlocking {
        val criteria = SearchCriteria()
        useCase(criteria, page = 1, pageSize = 20)

        assertEquals(20, repository.lastOffset)
    }

    @Test
    fun `invoke should calculate offset correctly for page 5`() = runBlocking {
        val criteria = SearchCriteria()
        useCase(criteria, page = 5, pageSize = 50)

        assertEquals(250, repository.lastOffset)
    }

    @Test
    fun `invoke should propagate filter mode`() = runBlocking {
        val criteria = SearchCriteria(filterMode = FilterMode.AND)
        useCase(criteria, page = 0, pageSize = 20)

        assertEquals(FilterMode.AND, repository.lastCriteria?.filterMode)
    }

    @Test
    fun `invoke should handle empty query`() = runBlocking {
        val criteria = SearchCriteria(query = "")
        useCase(criteria, page = 0, pageSize = 20)

        assertEquals("", repository.lastCriteria?.query)
    }

    @Test
    fun `invoke should handle multiple consoles`() = runBlocking {
        val consoles = setOf("nes", "snes")
        val criteria = SearchCriteria(consoles = consoles)
        useCase(criteria, page = 0, pageSize = 20)

        assertEquals(consoles, repository.lastCriteria?.consoles)
    }
}
