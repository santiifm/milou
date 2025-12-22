package com.santiifm.milou.ui.screens.sources.components

import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.painterResource
import com.santiifm.milou.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.santiifm.milou.data.model.Console
import com.santiifm.milou.util.FileParsingUtils

@Composable
fun ConsoleCard(
    console: Console,
    customDownloadPath: String?,
    onAddUrl: () -> Unit,
    onEditConsole: () -> Unit,
    onDeleteConsole: () -> Unit,
    onDeleteUrl: (Int) -> Unit,
    onSetCustomDownloadPath: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = console.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy((-12).dp)
                ) {
                    IconButton(onClick = onAddUrl) {
                        Icon(painterResource(R.drawable.ic_add), contentDescription = "Add URL")
                    }
                    IconButton(onClick = onSetCustomDownloadPath) {
                        Icon(painterResource(R.drawable.ic_folder), contentDescription = "Set Custom Download Path")
                    }
                    IconButton(onClick = onEditConsole) {
                        Icon(painterResource(R.drawable.ic_edit), contentDescription = "Edit Console")
                    }
                    IconButton(onClick = onDeleteConsole) {
                        Icon(painterResource(R.drawable.ic_trash), contentDescription = "Delete Console")
                    }
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            painterResource(if (expanded) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down),
                            contentDescription = if (expanded) "Collapse" else "Expand"
                        )
                    }
                }
            }

            if (customDownloadPath != null) {
                Text(
                    text = "Download Path: ${FileParsingUtils.toUserReadablePath(customDownloadPath)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    console.urls.forEachIndexed { index, urlEntry ->
                        UrlItem(
                            urlEntry = urlEntry,
                            onDelete = { onDeleteUrl(index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UrlItem(
    urlEntry: com.santiifm.milou.data.model.UrlEntry,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = urlEntry.url,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Type: ${urlEntry.contentType.name.lowercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    painterResource(R.drawable.ic_trash),
                    contentDescription = "Delete URL",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
