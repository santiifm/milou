package com.santiifm.milou.domain.usecase

import com.santiifm.milou.domain.model.Game
import com.santiifm.milou.domain.model.SearchCriteria
import com.santiifm.milou.domain.repository.SearchRepository

class SearchLibraryUseCase(
    private val repository: SearchRepository
) {
    suspend operator fun invoke(
        criteria: SearchCriteria,
        page: Int,
        pageSize: Int
    ): List<Game> {
        val normalizedQuery = criteria.query.trim().lowercase()
        val normalizedCriteria = criteria.copy(query = normalizedQuery)
        
        val offset = page * pageSize
        
        return repository.search(
            criteria = normalizedCriteria,
            limit = pageSize,
            offset = offset
        )
    }
}
