package com.santiifm.milou.domain.repository

import com.santiifm.milou.domain.model.Game
import com.santiifm.milou.domain.model.SearchCriteria

interface SearchRepository {
    suspend fun search(
        criteria: SearchCriteria,
        limit: Int,
        offset: Int
    ): List<Game>
}
