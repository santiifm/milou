package com.santiifm.milou.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.santiifm.milou.R
import com.santiifm.milou.data.model.DownloadableFileWithTags
import com.santiifm.milou.ui.theme.PurpleAccent
import com.santiifm.milou.ui.theme.WarmOrange

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var unitIndex = 0
    
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    
    return if (unitIndex == 0) {
        "${size.toInt()} ${units[unitIndex]}"
    } else {
String.format("%.1f %s", size, units[unitIndex])
    }
}

private fun cleanGameName(name: String): String {
    return name
        .trim() // Remove leading and trailing whitespace
        .let { trimmedName ->
            // Find all dots and check which one is likely a file extension
            val dotIndices = trimmedName.mapIndexedNotNull { index, char -> 
                if (char == '.') index else null 
            }
            
            // Look for the last dot that's followed by a short extension (1-4 characters)
            var lastValidDotIndex = -1
            for (dotIndex in dotIndices.reversed()) {
                val afterDot = trimmedName.substring(dotIndex + 1)
                // Check if it's a short extension (1-4 chars, no spaces, mostly alphanumeric)
                if (afterDot.length in 1..4 && 
                    afterDot.all { it.isLetterOrDigit() } && 
                    !afterDot.contains(' ')) {
                    lastValidDotIndex = dotIndex
                    break
                }
            }
            
            if (lastValidDotIndex > 0) {
                trimmedName.substring(0, lastValidDotIndex)
            } else {
                trimmedName
            }
        }
        .trim()
}

@Composable
fun RomList(
    library: List<DownloadableFileWithTags>,
    getConsoleName: (String) -> String = { it },
    onFileClick: (DownloadableFileWithTags) -> Unit = {},
    hasMoreResults: Boolean = false,
    isLoadingMore: Boolean = false,
    onLoadMore: () -> Unit = {}
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
        items(library) { romWithTags ->
            val rom = romWithTags.file
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp, end = 16.dp, start = 16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = PurpleAccent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onFileClick(romWithTags) }
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = cleanGameName(rom.name),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.weight(1f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Column(
                        modifier = Modifier.padding(start = 8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = getConsoleName(rom.consoleId),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            lineHeight = 16.sp
                        )
                        if (rom.fileSize > 0) {
                            Text(
                                text = formatFileSize(rom.fileSize),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
                
                val hasTags = romWithTags.tags.isNotEmpty() || rom.fileExtension.isNotEmpty()
                if (hasTags) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        val allTags = buildList {
                            addAll(romWithTags.tags)
                            if (rom.fileExtension.isNotEmpty()) {
                                add(rom.fileExtension.uppercase())
                            }
                        }
                        
                        if (allTags.isNotEmpty()) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                allTags.forEach { tag ->
                                    Text(
                                        text = if (tag == "game" || tag == "miscellaneous") {
                                            tag.replaceFirstChar { it.uppercase() }
                                        } else tag,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 10.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .background(
                                                color = WarmOrange.copy(alpha = 0.8f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (hasMoreResults) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isLoadingMore) {
                        CircularProgressIndicator()
                    } else {
                        Button(
                            onClick = onLoadMore,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.load_more))
                        }
                    }
                }
            }
        }
    }
}
