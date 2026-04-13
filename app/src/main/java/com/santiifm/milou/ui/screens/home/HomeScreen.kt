package com.santiifm.milou.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Job
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.santiifm.milou.R
import com.santiifm.milou.ui.screens.home.components.RomList
import com.santiifm.milou.ui.screens.home.components.SearchSection
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val results by viewModel.results.collectAsState()
    val hasMoreResults by viewModel.hasMoreResults.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarJob = remember { mutableStateOf<Job?>(null) }
    val context = LocalContext.current

    val getConsoleName = remember {
        { consoleId: String ->
            viewModel.getConsoleName(consoleId)
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                snackbarHostState,
                modifier = Modifier.padding(bottom = 70.dp)
            )
        },
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Image(
                painter = painterResource(R.drawable.milou),
                contentDescription = null,
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.Center),
                colorFilter = ColorFilter.tint(
                    Color.Gray.copy(alpha = 0.1f)
                )
            )
            
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                SearchSection(viewModel = viewModel, modifier = Modifier)

                RomList(
                    library = results, 
                    getConsoleName = getConsoleName,
                    onFileClick = { fileWithTags ->
                        scope.launch { viewModel.startDownload(fileWithTags, context) }
                        snackbarJob.value?.cancel()
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarJob.value = scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Download of \"${fileWithTags.file.name}\" has been started",
                                duration = androidx.compose.material3.SnackbarDuration.Short
                            )
                        }
                    },
                    hasMoreResults = hasMoreResults,
                    isLoadingMore = isLoadingMore,
                    onLoadMore = {
                        scope.launch {
                            viewModel.loadMore()
                        }
                    }
                )
            }
        }
    }
}