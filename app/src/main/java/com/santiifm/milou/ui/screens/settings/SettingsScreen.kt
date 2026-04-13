package com.santiifm.milou.ui.screens.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.santiifm.milou.R

@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            uri?.let {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                viewModel.onDownloadDirChanged(context, it.toString())
            }
        }
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 80.dp
            )
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = uiState.downloadDirectory,
            onValueChange = { },
            label = { Text(stringResource(R.string.settings_download_directory)) },
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { launcher.launch(null) }) {
                    Icon(painterResource(R.drawable.ic_folder), contentDescription = "Select Directory")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(stringResource(R.string.settings_concurrent_downloads, uiState.concurrentDownloads), style = MaterialTheme.typography.titleMedium)
        
        Slider(
            value = uiState.concurrentDownloads.toFloat(),
            onValueChange = { viewModel.onConcurrentDownloadsChanged(context, it.toInt()) },
            valueRange = 1f..10f,
            steps = 8,
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(stringResource(R.string.settings_limit_speed, if (uiState.limitSpeed == Float.POSITIVE_INFINITY) stringResource(R.string.settings_unrestricted) else "${uiState.limitSpeed.toInt()} KB/s"), style = MaterialTheme.typography.titleMedium)
        
        val sliderValue = if (uiState.limitSpeed == Float.POSITIVE_INFINITY) 0f else uiState.limitSpeed.coerceIn(1f, 1000f)
        
        Slider(
            value = sliderValue,
            onValueChange = { sliderVal ->
                val actualSpeed = if (sliderVal == 0f) Float.POSITIVE_INFINITY else sliderVal
                viewModel.onLimitSpeedChanged(context, actualSpeed)
            },
            valueRange = 0f..1000f,
            steps = 10,
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.settings_auto_unzip), style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = uiState.autoUnzip,
                onCheckedChange = { viewModel.onAutoUnzipChanged(context, it) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.settings_separate_by_console), style = MaterialTheme.typography.titleMedium)
            Switch(
                checked = uiState.separateByConsole,
                onCheckedChange = { viewModel.onSeparateByConsoleChanged(context, it) }
            )
        }
    }
}
