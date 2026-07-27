package com.santiifm.milou.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchCriteriaTest {

    @Test
    fun `default SearchCriteria should have correct values`() {
        val criteria = SearchCriteria()
        assertEquals("", criteria.query)
        assertEquals(emptySet<String>(), criteria.consoles)
        assertEquals(emptySet<String>(), criteria.tags)
        assertEquals(FilterMode.OR, criteria.filterMode)
        assertEquals(SortOrder.ASC, criteria.sortOrder)
        assertEquals(false, criteria.includeHidden)
        assertEquals(false, criteria.favoritesOnly)
    }

    @Test
    fun `copying SearchCriteria should work correctly`() {
        val criteria = SearchCriteria(query = "original")
        val updated = criteria.copy(query = "updated", favoritesOnly = true)
        
        assertEquals("updated", updated.query)
        assertEquals(true, updated.favoritesOnly)
        assertEquals(FilterMode.OR, updated.filterMode)
    }
}
