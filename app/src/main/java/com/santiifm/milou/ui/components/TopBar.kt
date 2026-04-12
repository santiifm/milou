package com.santiifm.milou.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.santiifm.milou.di.RescanStateEntryPoint
import dagger.hilt.android.EntryPointAccessors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MilouTopBar() {
    val context = LocalContext.current
    val rescanStateHolder = EntryPointAccessors.fromApplication(
        context.applicationContext,
        RescanStateEntryPoint::class.java
    ).rescanStateHolder()

    val isRescanning by rescanStateHolder.isRescanning.collectAsState()
    val progressMessage by rescanStateHolder.progressMessage.collectAsState()
    val torrentFetchProgress by rescanStateHolder.torrentFetchProgress.collectAsState()

    if (!isRescanning) return

    val displayMessage = when {
        torrentFetchProgress.isNotEmpty() -> torrentFetchProgress
        progressMessage.isNotEmpty() -> progressMessage
        else -> "Rescanning sources..."
    }

    TopAppBar(
        title = {},
        actions = {
            Row(
                modifier = Modifier.padding(end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = displayMessage,
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        modifier = Modifier
            .statusBarsPadding()
            .height(56.dp),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}
