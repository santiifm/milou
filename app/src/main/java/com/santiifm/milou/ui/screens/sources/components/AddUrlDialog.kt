package com.santiifm.milou.ui.screens.sources.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.santiifm.milou.R
import com.santiifm.milou.data.model.ContentType

@Composable
fun AddUrlDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, ContentType) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var contentType by remember { mutableStateOf(ContentType.GAME) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                url = it.toString()
            }
        }
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Source") },
        text = {
            Column {
                Text("Enter a URL, magnet link, or upload a .torrent file:")
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("URL / Magnet") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text("https://... or magnet:?")
                        }
                    )

                    IconButton(
                        onClick = { filePicker.launch("application/x-bittorrent") }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_folder),
                            contentDescription = "Upload .torrent"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Content Type:", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))
                ContextTypeSelection(
                    selectedType = contentType,
                    onTypeSelected = { contentType = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        url.trim(),
                        contentType
                    )
                    onDismiss()
                },
                enabled = url.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContextTypeSelection(
    selectedType: ContentType,
    onTypeSelected: (ContentType) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ContentType.entries.forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                label = {
                    Text(
                        text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                        maxLines = 1
                    )
                }
            )
        }
    }
}
