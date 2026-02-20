package com.vdustr.lofiradio.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.vdustr.lofiradio.ui.theme.Border
import com.vdustr.lofiradio.ui.theme.Primary
import com.vdustr.lofiradio.ui.theme.Surface
import com.vdustr.lofiradio.ui.theme.TextMuted
import com.vdustr.lofiradio.ui.theme.TextPrimary
import com.vdustr.lofiradio.ui.theme.TextSecondary
import com.vdustr.lofiradio.ui.theme.VarelaRound
import com.vdustr.lofiradio.viewmodel.RadioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: RadioViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val filteredStreams by viewModel.filteredStreams.collectAsState()
    val currentStream by viewModel.currentStream.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sleepTimer by viewModel.sleepTimer.collectAsState()
    val context = LocalContext.current

    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showAboutScreen by remember { mutableStateOf(false) }

    if (showAboutScreen) {
        AboutScreen(onBack = { showAboutScreen = false })
        return
    }

    Scaffold(
        containerColor = Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Top Bar
            TopBar(
                onCastClick = { /* Cast handled via CastButtonFactory */ },
                onSettingsClick = { showAboutScreen = true }
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
                        // Stream info
                        currentStream?.let { stream ->
                            item {
                                StreamInfoSection(stream = stream)
                            }
                        }

                        // Action chips
                        item {
                            ActionChips(
                                sleepTimerActive = sleepTimer.isActive,
                                sleepTimerRemaining = sleepTimer.remainingMillis,
                                onSleepTimerClick = { showSleepTimerSheet = true },
                                onOpenYouTubeClick = { viewModel.openInYouTube(context) }
                            )
                        }

                        // Search bar
                        item {
                            SearchBar(
                                query = searchQuery,
                                onQueryChange = { viewModel.updateSearchQuery(it) }
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
                            StreamListItem(
                                stream = stream,
                                isActive = stream.videoId == currentStream?.videoId,
                                index = index,
                                onClick = { viewModel.playStream(stream) }
                            )
                        }

                        // Bottom spacing for player bar
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }
            }

            // Bottom Player Bar
            PlayerBar(
                currentStream = currentStream,
                isPlaying = isPlaying,
                onPlayPauseClick = { viewModel.togglePlayPause() },
                onCastClick = { /* Cast handled via CastButtonFactory */ },
            )
        }

        // Sleep Timer Bottom Sheet
        if (showSleepTimerSheet) {
            SleepTimerSheet(
                sleepTimerState = sleepTimer,
                onSelectPreset = { minutes ->
                    viewModel.startSleepTimer(minutes)
                    showSleepTimerSheet = false
                },
                onCancel = {
                    viewModel.cancelSleepTimer()
                    showSleepTimerSheet = false
                },
                onDismiss = { showSleepTimerSheet = false }
            )
        }
    }
}

@Composable
private fun TopBar(
    onCastClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Lofi Girl Radio",
                style = TextStyle(
                    fontFamily = VarelaRound,
                    fontSize = 20.sp,
                    brush = Brush.linearGradient(colors = listOf(Primary, Accent))
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
            IconButton(onClick = onCastClick) {
                // Cast icon placeholder
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
private fun StreamInfoSection(
    stream: com.vdustr.lofiradio.data.LofiStream,
) {
    Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
        Text(
            text = stream.title,
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Lofi Girl",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Text(" · ", color = TextMuted, style = MaterialTheme.typography.bodySmall)
            Text(
                text = "Streaming now",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun ActionChips(
    sleepTimerActive: Boolean,
    sleepTimerRemaining: Long,
    onSleepTimerClick: () -> Unit,
    onOpenYouTubeClick: () -> Unit
) {
    Row(
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionChip(
            icon = { Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(14.dp)) },
            label = if (sleepTimerActive) formatTimer(sleepTimerRemaining) else "Sleep Timer",
            isActive = sleepTimerActive,
            onClick = onSleepTimerClick
        )
        ActionChip(
            icon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp)) },
            label = "Open in YouTube",
            onClick = onOpenYouTubeClick
        )
    }
}

@Composable
private fun ActionChip(
    icon: @Composable () -> Unit,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isActive) com.vdustr.lofiradio.ui.theme.PrimarySubtle
                else Surface
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        icon()
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isActive) Primary else TextSecondary
        )
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
                fontFamily = com.vdustr.lofiradio.ui.theme.NunitoSans
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

private fun formatTimer(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    else String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
}
