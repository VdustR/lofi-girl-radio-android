package com.vdustr.lofiradio.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vdustr.lofiradio.ui.theme.Accent
import com.vdustr.lofiradio.ui.theme.Background
import com.vdustr.lofiradio.ui.theme.Primary
import com.vdustr.lofiradio.ui.theme.Surface
import com.vdustr.lofiradio.ui.theme.TextMuted
import com.vdustr.lofiradio.ui.theme.TextPrimary
import com.vdustr.lofiradio.ui.theme.TextSecondary
import com.vdustr.lofiradio.ui.theme.NunitoSans
import com.vdustr.lofiradio.ui.theme.VarelaRound
import com.vdustr.lofiradio.viewmodel.RadioViewModel

@Composable
fun MainScreen(
    viewModel: RadioViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val filteredStreams by viewModel.filteredStreams.collectAsState()
    val currentStream by viewModel.currentStream.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sleepTimerState by viewModel.sleepTimer.collectAsState()
    val context = LocalContext.current

    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showAboutScreen by remember { mutableStateOf(false) }

    if (showAboutScreen) {
        AboutScreen(onBack = { showAboutScreen = false })
        return
    }

    val openYouTube = remember(context) { { viewModel.openInYouTube(context) } }

    Scaffold(
        containerColor = Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top Bar
            val onSettingsClick = remember { { showAboutScreen = true } }
            val onSleepTimerClick = remember { { showSleepTimerSheet = true } }
            TopBar(
                sleepTimerActive = sleepTimerState.isActive,
                onSleepTimerClick = onSleepTimerClick,
                onSettingsClick = onSettingsClick
            )

            // Main content
            when (val state = uiState) {
                is RadioViewModel.UiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Loading streams...", color = TextMuted)
                    }
                }

                is RadioViewModel.UiState.Empty -> {
                    ErrorScreen(
                        errorType = RadioViewModel.ErrorType.STREAM_DOWN,
                        onRetry = { viewModel.loadStreams() },
                        modifier = Modifier.weight(1f)
                    )
                }

                is RadioViewModel.UiState.Error -> {
                    ErrorScreen(
                        errorType = state.type,
                        onRetry = { viewModel.loadStreams() },
                        modifier = Modifier.weight(1f)
                    )
                }

                is RadioViewModel.UiState.Ready -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        // Search bar
                        item {
                            val onQueryChange = remember { { q: String -> viewModel.updateSearchQuery(q) } }
                            SearchBar(
                                query = searchQuery,
                                onQueryChange = onQueryChange
                            )
                        }

                        // Section header
                        item {
                            Text(
                                text = "All Live Streams",
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                            )
                        }

                        // Stream list
                        itemsIndexed(
                            items = filteredStreams,
                            key = { _, stream -> stream.videoId }
                        ) { index, stream ->
                            val onClick = remember(stream) { { viewModel.playStream(stream) } }
                            StreamListItem(
                                stream = stream,
                                isActive = stream.videoId == currentStream?.videoId,
                                index = index,
                                onClick = onClick
                            )
                        }

                        // Bottom spacing for player bar
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }
            }

            // Bottom Player Bar
            val onPlayPauseClick = remember { { viewModel.togglePlayPause() } }
            PlayerBar(
                currentStream = currentStream,
                isPlaying = isPlaying,
                isBuffering = isBuffering,
                onPlayPauseClick = onPlayPauseClick,
                onOpenYouTubeClick = if (currentStream != null) openYouTube else null,
                sleepTimerRemainingMillis = if (sleepTimerState.isActive) sleepTimerState.remainingMillis else null,
            )
        }

        // Sleep Timer Bottom Sheet
        if (showSleepTimerSheet) {
            SleepTimerSheet(
                sleepTimerState = sleepTimerState,
                onStart = { durationMillis -> viewModel.startSleepTimer(durationMillis) },
                onCancel = { viewModel.cancelSleepTimer() },
                onDismiss = { showSleepTimerSheet = false }
            )
        }
    }
}

@Composable
private fun TopBar(
    sleepTimerActive: Boolean,
    onSleepTimerClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Lofi Girl Radio",
                style = TextStyle(
                    fontFamily = VarelaRound,
                    fontSize = 20.sp,
                    brush = remember { Brush.linearGradient(colors = listOf(Primary, Accent)) }
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "unofficial",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Surface)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Row {
            IconButton(onClick = onSleepTimerClick) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Sleep Timer",
                    tint = if (sleepTimerActive) Primary else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "About",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(16.dp)
        )
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = TextStyle(
                color = TextPrimary,
                fontSize = 13.sp,
                fontFamily = NunitoSans
            ),
            cursorBrush = SolidColor(Primary),
            singleLine = true,
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        "Filter streams...",
                        style = TextStyle(color = TextMuted, fontSize = 13.sp)
                    )
                }
                innerTextField()
            }
        )
    }
}
