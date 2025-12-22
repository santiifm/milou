package com.santiifm.milou.ui.screens.sources

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.santiifm.milou.R
import com.santiifm.milou.ui.screens.sources.components.AddConsoleDialog
import com.santiifm.milou.ui.screens.sources.components.AddManufacturerDialog
import com.santiifm.milou.ui.screens.sources.components.AddUrlDialog
import com.santiifm.milou.ui.screens.sources.components.ManufacturerCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesScreen(
    viewModel: SourcesViewModel = hiltViewModel()
) {
    val manufacturers by viewModel.manufacturers.collectAsState(initial = emptyList())
    val isRescanning by viewModel.isRescanning.collectAsState()
    val consoleDownloadPaths by viewModel.consoleDownloadDirectories.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val directoryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            uri?.let {
                val contentResolver = context.contentResolver
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                viewModel.selectedConsoleId.value?.let {
                    viewModel.updateConsoleDownloadDirectory(it, uri.toString())
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        viewModel.loadDefaultSources(context)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!isLandscape) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Sources",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.showAddManufacturerDialog() }
                        ) {
                            Icon(painterResource(R.drawable.ic_add), contentDescription = "Add Manufacturer")
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isRescanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Rescanning...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Button(
                                    onClick = { viewModel.rescanAllSources() }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_retry),
                                        contentDescription = ""
                                    )
                                    Text(
                                        text = "Rescan Sources"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Landscape mode: Show only the action buttons without header text
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.showAddManufacturerDialog() }
                    ) {
                        Icon(painterResource(R.drawable.ic_add), contentDescription = "Add Manufacturer")
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isRescanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "Rescanning...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Button(
                                onClick = { viewModel.rescanAllSources() }
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_retry),
                                    contentDescription = ""
                                )
                                Text(
                                    text = "Rescan Sources"
                                )
                            }
                        }
                    }
                }
            }
        }
        if (manufacturers.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No Sources Configured",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add manufacturers and consoles to start discovering games",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(manufacturers) { manufacturer ->
                ManufacturerCard(
                    manufacturer = manufacturer,
                    onAddConsole = { viewModel.showAddConsoleDialog(manufacturer.id) },
                    onEditManufacturer = { viewModel.showEditManufacturerDialog(manufacturer.id) },
                    onDeleteManufacturer = { viewModel.deleteManufacturer(manufacturer.id) },
                    onAddUrl = { consoleId -> viewModel.showAddUrlDialog(consoleId) },
                    onEditConsole = { consoleId -> viewModel.showEditConsoleDialog(manufacturer.id, consoleId) },
                    onDeleteConsole = { consoleId -> viewModel.deleteConsole(consoleId) },
                    onDeleteUrl = { consoleId, urlIndex -> viewModel.deleteUrl(consoleId, urlIndex) },
                    onSetCustomDownloadPath = { consoleId ->
                        viewModel.setSelectedConsoleId(consoleId)
                        directoryPicker.launch(null)
                    },
                    getCustomDownloadPathForConsole = {
                        consoleDownloadPaths[it]
                    }
                )
            }
        }
    }

    val showAddManufacturerDialog by viewModel.showAddManufacturerDialog.collectAsState()
    val showEditManufacturerDialog by viewModel.showEditManufacturerDialog.collectAsState()
    val showAddConsoleDialog by viewModel.showAddConsoleDialog.collectAsState()
    val showEditConsoleDialog by viewModel.showEditConsoleDialog.collectAsState()
    val showAddUrlDialog by viewModel.showAddUrlDialog.collectAsState()
    val selectedManufacturerId by viewModel.selectedManufacturerId.collectAsState()
    val selectedConsoleId by viewModel.selectedConsoleId.collectAsState()

    if (showAddManufacturerDialog) {
        AddManufacturerDialog(
            onDismiss = { viewModel.hideAddManufacturerDialog() },
            onConfirm = { name -> viewModel.addManufacturer(name) }
        )
    }

    if (showEditManufacturerDialog) {
        selectedManufacturerId?.let { manufacturerId ->
            AddManufacturerDialog(
                manufacturerId = manufacturerId,
                onDismiss = { viewModel.hideEditManufacturerDialog() },
                onConfirm = { name -> viewModel.updateManufacturer(manufacturerId, name) }
            )
        }
    }

    if (showAddConsoleDialog) {
        selectedManufacturerId?.let { manufacturerId ->
            AddConsoleDialog(
                onDismiss = { viewModel.hideAddConsoleDialog() },
                onConfirm = { name -> viewModel.addConsole(manufacturerId, name) }
            )
        }
    }

    if (showEditConsoleDialog) {
        selectedConsoleId?.let { consoleId ->
            AddConsoleDialog(
                consoleId = consoleId,
                onDismiss = { viewModel.hideEditConsoleDialog() },
                onConfirm = { name -> viewModel.updateConsole(consoleId, name) }
            )
        }
    }

    if (showAddUrlDialog) {
        selectedConsoleId?.let { consoleId ->
            AddUrlDialog(
                onDismiss = { viewModel.hideAddUrlDialog() },
                onConfirm = { url, contentType -> viewModel.addUrl(consoleId, url, contentType) }
            )
        }
    }
}
