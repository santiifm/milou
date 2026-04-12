package com.santiifm.milou.ui.screens.sources.components

import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.painterResource
import com.santiifm.milou.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.santiifm.milou.data.model.Manufacturer

@Composable
fun ManufacturerCard(
    manufacturer: Manufacturer,
    onAddConsole: () -> Unit,
    onEditManufacturer: () -> Unit,
    onDeleteManufacturer: () -> Unit,
    onAddUrl: (String) -> Unit,
    onEditConsole: (String) -> Unit,
    onDeleteConsole: (String) -> Unit,
    onDeleteUrl: (String, Int) -> Unit,
    onSetCustomDownloadPath: (String) -> Unit,
    onRefreshConsole: (String) -> Unit,
    getCustomDownloadPathForConsole: (String) -> String?
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = manufacturer.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy((-12).dp)
                ) {
                    IconButton(onClick = onEditManufacturer) {
                        Icon(painterResource(R.drawable.ic_edit), contentDescription = "Edit Manufacturer")
                    }
                    IconButton(onClick = onDeleteManufacturer) {
                        Icon(painterResource(R.drawable.ic_trash), contentDescription = "Delete Manufacturer")
                    }
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            painterResource(if (expanded) R.drawable.ic_arrow_up else R.drawable.ic_arrow_down),
                            contentDescription = if (expanded) "Collapse" else "Expand"
                        )
                    }
                }
            }

            Text(
                text = "${manufacturer.consoles.size} console${if (manufacturer.consoles.size != 1) "s" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onAddConsole,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Console", style = MaterialTheme.typography.bodySmall)
            }

            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    manufacturer.consoles.forEach { console ->
                        ConsoleCard(
                            console = console,
                            customDownloadPath = getCustomDownloadPathForConsole(console.id),
                            onAddUrl = { onAddUrl(console.id) },
                            onEditConsole = { onEditConsole(console.id) },
                            onDeleteConsole = { onDeleteConsole(console.id) },
                            onDeleteUrl = { urlIndex -> onDeleteUrl(console.id, urlIndex) },
                            onSetCustomDownloadPath = { onSetCustomDownloadPath(console.id) },
                            onRefreshConsole = { onRefreshConsole(console.id) }
                        )
                    }
                }
            }
        }
    }
}
