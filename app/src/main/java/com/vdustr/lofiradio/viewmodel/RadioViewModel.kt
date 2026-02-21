package com.vdustr.lofiradio.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.vdustr.lofiradio.LofiRadioApp
import com.vdustr.lofiradio.data.LofiStream
import com.vdustr.lofiradio.playback.PlaybackService
import com.vdustr.lofiradio.util.fuzzyMatchScore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RadioViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as LofiRadioApp
    private val repository = app.streamRepository

    // --- UI State ---

    sealed interface UiState {
        data object Loading : UiState
        data object Empty : UiState
        data class Ready(val streams: List<LofiStream>) : UiState
        data class Error(val type: ErrorType, val message: String? = null) : UiState
    }

    enum class ErrorType { OFFLINE, STREAM_DOWN, EXTRACTION_ERROR }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // --- Streams ---

    private val _streams = MutableStateFlow<List<LofiStream>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredStreams: StateFlow<List<LofiStream>> = combine(
        _streams,
        _searchQuery
    ) { streams, query ->
        if (query.isBlank()) {
            streams
        } else {
            streams.mapNotNull { stream ->
                val score = fuzzyMatchScore(query, stream.title)
                if (score > 0) stream to score else null
            }.sortedByDescending { it.second }.map { it.first }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Playback ---

    private val _currentStream = MutableStateFlow<LofiStream?>(null)
    val currentStream: StateFlow<LofiStream?> = _currentStream.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    @Volatile private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var playStreamJob: Job? = null

    // --- Sleep Timer ---

    data class SleepTimerState(
        val isActive: Boolean = false,
        val remainingMillis: Long = 0L
    )

    private val _sleepTimer = MutableStateFlow(SleepTimerState())
    val sleepTimer: StateFlow<SleepTimerState> = _sleepTimer.asStateFlow()

    private var sleepTimerJob: Job? = null

    // --- Init ---

    init {
        connectToPlaybackService()
        loadStreams()
    }

    private fun connectToPlaybackService() {
        val sessionToken = SessionToken(
            getApplication(),
            ComponentName(getApplication(), PlaybackService::class.java)
        )
        val future = MediaController.Builder(getApplication(), sessionToken).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                val controller = future.get()
                mediaController = controller

                // Sync initial state before adding listener to avoid race condition
                _isPlaying.value = controller.isPlaying
                _isBuffering.value = (controller.playbackState == Player.STATE_BUFFERING)

                controller.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        _isPlaying.value = playing
                    }
                    override fun onPlaybackStateChanged(state: Int) {
                        _isBuffering.value = (state == Player.STATE_BUFFERING)
                    }
                })
            } catch (_: Exception) {
                _uiState.value = UiState.Error(ErrorType.EXTRACTION_ERROR, "Failed to connect to playback service")
            }
        }, ContextCompat.getMainExecutor(getApplication()))
    }

    // --- Actions ---

    fun loadStreams() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val streams = repository.fetchLiveStreams()
                _streams.value = streams.sortedByDescending { it.viewerCount }
                _uiState.value = if (streams.isEmpty()) UiState.Empty
                else UiState.Ready(streams)
            } catch (e: java.io.IOException) {
                _uiState.value = UiState.Error(ErrorType.OFFLINE)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(ErrorType.EXTRACTION_ERROR, e.message)
            }
        }
    }

    fun playStream(stream: LofiStream) {
        playStreamJob?.cancel()
        playStreamJob = viewModelScope.launch {
            _currentStream.value = stream
            _isBuffering.value = true
            try {
                val hlsUrl = repository.getHlsUrl(stream.videoId)
                // Check if user switched to a different stream while we were loading
                if (_currentStream.value?.videoId != stream.videoId) {
                    _isBuffering.value = false
                    return@launch
                }
                val metadataBuilder = MediaMetadata.Builder()
                    .setTitle(stream.title)
                    .setArtist("Lofi Girl")
                if (stream.thumbnailUrl != null) {
                    metadataBuilder.setArtworkUri(Uri.parse(stream.thumbnailUrl))
                }
                val mediaItem = MediaItem.Builder()
                    .setUri(hlsUrl)
                    .setMimeType(MimeTypes.APPLICATION_M3U8)
                    .setMediaMetadata(metadataBuilder.build())
                    .build()
                val controller = mediaController
                if (controller == null) {
                    _isBuffering.value = false
                    _uiState.value = UiState.Error(ErrorType.EXTRACTION_ERROR, "Playback service not ready")
                    return@launch
                }
                controller.setMediaItem(mediaItem)
                controller.prepare()
                controller.play()
                // ExoPlayer listener takes over _isBuffering from here
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _isBuffering.value = false
                _uiState.value = UiState.Error(ErrorType.STREAM_DOWN, e.message)
            }
        }
    }

    fun togglePlayPause() {
        mediaController?.let { controller ->
            if (controller.playWhenReady) {
                controller.pause()
                _isBuffering.value = false
            } else {
                controller.play()
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- Sleep Timer ---

    fun startSleepTimer(durationMillis: Long) {
        if (durationMillis < 60_000L) return
        sleepTimerJob?.cancel()
        _sleepTimer.value = SleepTimerState(
            isActive = true,
            remainingMillis = durationMillis
        )
        sleepTimerJob = viewModelScope.launch {
            val endTime = SystemClock.elapsedRealtime() + durationMillis
            while (true) {
                delay(1000L)
                ensureActive()
                val remaining = (endTime - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
                if (remaining == 0L) break
                _sleepTimer.value = _sleepTimer.value.copy(remainingMillis = remaining)
            }
            // Timer expired — reset timer state first, then pause
            _sleepTimer.value = SleepTimerState()
            mediaController?.pause()
            _isBuffering.value = false
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimer.value = SleepTimerState()
    }

    // --- External ---

    fun openInYouTube(context: Context) {
        val stream = _currentStream.value ?: return
        val uri = Uri.Builder()
            .scheme("https")
            .authority("www.youtube.com")
            .path("watch")
            .appendQueryParameter("v", stream.videoId)
            .build()
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    // --- Cleanup ---

    override fun onCleared() {
        super.onCleared()
        playStreamJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        sleepTimerJob?.cancel()
    }
}
