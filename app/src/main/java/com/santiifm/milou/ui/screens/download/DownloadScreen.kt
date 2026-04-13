package com.santiifm.milou.ui.screens.download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.santiifm.milou.R

@Composable
fun DownloadScreen(
    navController: NavController,
    viewModel: DownloadViewModel = hiltViewModel()
) {
    val downloads by viewModel.downloads.collectAsState()
    val showDeleteConfirmation by viewModel.showDeleteConfirmation.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isLandscape) {
            Text(
                text = "Downloads",
                style = MaterialTheme.typography.titleLarge
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 64.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(downloads) { item ->
                DownloadItem(item = item, viewModel =  viewModel)
            }
        }
    }
    
    showDeleteConfirmation?.let { fileName ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDeleteConfirmation() },
            title = {
                Text(text = stringResource(R.string.delete_download_title))
            },
            text = {
                Text(text = stringResource(R.string.delete_download_message))
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDeleteRemoveFile(fileName) }
                ) {
                    Text(stringResource(R.string.delete_file))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.confirmDeleteKeepFile(fileName) }
                ) {
                    Text(stringResource(R.string.keep_file))
                }
            }
        )
    }
}
