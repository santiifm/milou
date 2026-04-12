package com.santiifm.milou.ui.screens.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.santiifm.milou.R
import com.santiifm.milou.data.model.TagCategory
import com.santiifm.milou.ui.screens.home.HomeViewModel

@Composable
fun LandscapeFilterOverlay(
    viewModel: HomeViewModel,
    selectedConsoles: Set<String>,
    selectedTags: Set<String>,
    onConsoleToggle: (String) -> Unit,
    onTagToggle: (String) -> Unit,
    onClearConsoleFilters: () -> Unit,
    onClearAllFilters: () -> Unit,
    onGoBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val consolesWithFiles by viewModel.consolesWithFiles.collectAsState()
    val categorizedTags by viewModel.categorizedTags.collectAsState()
    val tagFilterMode by viewModel.tagFilterMode.collectAsState()
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Header with go back button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onGoBack,
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_left),
                        contentDescription = "Go back"
                    )
                }
                
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                FilterModeToggle(
                    currentMode = tagFilterMode,
                    onToggle = { viewModel.toggleTagFilterMode() },
                    isConsoleFilter = false,
                    modifier = Modifier.padding(end = 8.dp)
                )
                
                Button(
                    onClick = onClearAllFilters,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text("Clear All")
                }
            }
            
            // Two-column layout for filters
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Console Filter Column
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.filter_by_console),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Clear all option
                            item {
                                CompactConsoleItem(
                                    consoleName = stringResource(R.string.all_consoles),
                                    isSelected = selectedConsoles.isEmpty(),
                                    fileCount = null,
                                    onClick = onClearConsoleFilters
                                )
                            }
                            
                            if (consolesWithFiles.isNotEmpty()) {
                                items(consolesWithFiles) { console ->
                                    val consoleName = com.santiifm.milou.util.ConsoleFormatter.getConsoleDisplayName(console.id)
                                    val isSelected = selectedConsoles.contains(console.id)
                                    
                                    CompactConsoleItem(
                                        consoleName = consoleName,
                                        isSelected = isSelected,
                                        fileCount = console.fileCount,
                                        onClick = { onConsoleToggle(console.id) }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Tag Filter Column
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.filter_by_tags),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            categorizedTags?.let { tags ->
                                // Regions
                                if (tags.regions.tags.isNotEmpty()) {
                                    item {
                                        CollapsibleTagCategorySection(
                                            category = tags.regions,
                                            activeTags = selectedTags,
                                            onTagToggle = onTagToggle
                                        )
                                    }
                                }
                                
                                // Video Standards
                                if (tags.videoStandards.tags.isNotEmpty()) {
                                    item {
                                        CollapsibleTagCategorySection(
                                            category = tags.videoStandards,
                                            activeTags = selectedTags,
                                            onTagToggle = onTagToggle
                                        )
                                    }
                                }
                                
                                // Content Types
                                if (tags.contentTypes.tags.isNotEmpty()) {
                                    item {
                                        CollapsibleTagCategorySection(
                                            category = tags.contentTypes,
                                            activeTags = selectedTags,
                                            onTagToggle = onTagToggle
                                        )
                                    }
                                }
                                
                                // Languages
                                if (tags.languages.tags.isNotEmpty()) {
                                    item {
                                        CollapsibleTagCategorySection(
                                            category = tags.languages,
                                            activeTags = selectedTags,
                                            onTagToggle = onTagToggle
                                        )
                                    }
                                }
                                
                                // File Types
                                if (tags.fileTypes.tags.isNotEmpty()) {
                                    item {
                                        CollapsibleTagCategorySection(
                                            category = tags.fileTypes,
                                            activeTags = selectedTags,
                                            onTagToggle = onTagToggle
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactConsoleItem(
    consoleName: String,
    isSelected: Boolean,
    fileCount: Int?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onClick() },
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = consoleName,
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        fileCount?.let { count ->
            Text(
                text = "($count)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CollapsibleTagCategorySection(
    category: TagCategory,
    activeTags: Set<String>,
    onTagToggle: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_down),
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                modifier = Modifier
                    .rotate(if (isExpanded) 180f else 0f)
                    .padding(end = 4.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )
            
            // Show count of active tags in this category
            val activeCount = category.tags.count { it in activeTags }
            if (activeCount > 0) {
                Text(
                    text = "($activeCount)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }
        
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(300)),
            exit = shrinkVertically(animationSpec = tween(300))
        ) {
            Column {
                category.tags.forEach { tag ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = tag in activeTags,
                            onCheckedChange = { onTagToggle(tag) },
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = tag,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 6.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 2.dp),
            thickness = DividerDefaults.Thickness,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    }
}
