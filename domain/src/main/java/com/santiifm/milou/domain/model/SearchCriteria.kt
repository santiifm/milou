package com.santiifm.milou.domain.model

data class SearchCriteria(
    val query: String = "",
    val consoles: Set<String> = emptySet(),
    val tags: Set<String> = emptySet(),
    val filterMode: FilterMode = FilterMode.OR,
    val sortOrder: SortOrder = SortOrder.ASC,
    val includeHidden: Boolean = false,
    val favoritesOnly: Boolean = false
)
