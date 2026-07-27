package com.santiifm.milou.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.santiifm.milou.domain.model.FilterMode
import com.santiifm.milou.ui.theme.LightningBlue
import com.santiifm.milou.ui.theme.NeonRed
import com.santiifm.milou.ui.theme.WarmOrange

@Composable
fun SelectedFiltersPills(
    selectedConsoles: Set<String>,
    selectedTags: Set<String>,
    tagFilterMode: FilterMode,
    getConsoleName: (String) -> String,
    onRemoveConsole: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val allFilters = buildList {
        selectedConsoles.forEach { consoleId ->
            add(FilterPillData(
                id = consoleId,
                label = getConsoleName(consoleId),
                type = FilterType.CONSOLE,
                onRemove = { onRemoveConsole(consoleId) }
            ))
        }
        
        if (selectedTags.size > 1) {
            add(FilterPillData(
                id = "tag_mode",
                label = "Tags: ${tagFilterMode.name}",
                type = FilterType.MODE_INDICATOR,
                onRemove = { }
            ))
        }
        
        selectedTags.forEach { tag ->
            add(FilterPillData(
                id = tag,
                label = tag,
                type = FilterType.TAG,
                onRemove = { onRemoveTag(tag) }
            ))
        }
    }
    
    if (allFilters.isNotEmpty()) {
        LazyRow(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(allFilters) { filterData ->
                FilterPill(
                    filterData = filterData,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun FilterPill(
    filterData: FilterPillData,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (filterData.type) {
        FilterType.CONSOLE -> WarmOrange
        FilterType.TAG -> LightningBlue
        FilterType.MODE_INDICATOR -> NeonRed.copy(alpha = 0.6f)
    }
    
    val textColor = MaterialTheme.colorScheme.onSurface
    
    Box(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = filterData.label,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 2.sp,
                modifier = modifier.height(14.dp)
            )

            if (filterData.type != FilterType.MODE_INDICATOR) {
                Text(
                    text = "×",
                    color = textColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 2.sp,
                    modifier = Modifier
                        .clickable { filterData.onRemove() }
                        .padding(2.dp)
                        .height(14.dp)
                )
            }
        }
    }
}

private data class FilterPillData(
    val id: String,
    val label: String,
    val type: FilterType,
    val onRemove: () -> Unit
)

private enum class FilterType {
    CONSOLE, TAG, MODE_INDICATOR
}
