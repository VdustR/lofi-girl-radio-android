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
            streams.sortedByDescending { it.viewerCount }
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

    private var mediaController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var playStreamJob: Job? = null

    // --- Audio Quality ---

    enum class AudioQuality(val label: String, val maxBitrate: Int) {
        AUTO("Auto", Int.MAX_VALUE),
        HIGH("High", 320_000),
        NORMAL("Normal", 128_000),
        LOW("Low", 64_000)
    }

    private val _audioQuality = MutableStateFlow(AudioQuality.AUTO)
    val audioQuality: StateFlow<AudioQuality> = _audioQuality.asStateFlow()

    // --- Sleep Timer ---

    data class SleepTimerState(
        val isActive: Boolean = false,
        val remainingMillis: Long = 0L,
        val selectedPresetMinutes: Int? = null
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
                mediaController = future.get()
                mediaController?.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        _isPlaying.value = playing
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
                _streams.value = streams
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
            try {
                val hlsUrl = repository.getHlsUrl(stream.videoId)
                // Check if user switched to a different stream while we were loading
                if (_currentStream.value?.videoId != stream.videoId) return@launch
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
                    _uiState.value = UiState.Error(ErrorType.EXTRACTION_ERROR, "Playback service not ready")
                    return@launch
                }
                controller.setMediaItem(mediaItem)
                controller.prepare()
                controller.play()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = UiState.Error(ErrorType.STREAM_DOWN, e.message)
            }
        }
    }

    fun togglePlayPause() {
        mediaController?.let { controller ->
            if (controller.isPlaying) controller.pause()
            else controller.play()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setAudioQuality(quality: AudioQuality) {
        _audioQuality.value = quality
        mediaController?.let { controller ->
            val params = controller.trackSelectionParameters.buildUpon()
                .setMaxAudioBitrate(quality.maxBitrate)
                .build()
            controller.trackSelectionParameters = params
        }
    }

    // --- Sleep Timer ---

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        val durationMillis = minutes * 60_000L
        _sleepTimer.value = SleepTimerState(
            isActive = true,
            remainingMillis = durationMillis,
            selectedPresetMinutes = minutes
        )
        sleepTimerJob = viewModelScope.launch {
            val endTime = SystemClock.elapsedRealtime() + durationMillis
            while (true) {
                val remaining = endTime - SystemClock.elapsedRealtime()
                if (remaining <= 0) break
                _sleepTimer.value = _sleepTimer.value.copy(remainingMillis = remaining)
                delay(1000L)
            }
            // Timer expired — stop playback
            mediaController?.stop()
            _isPlaying.value = false
            _sleepTimer.value = SleepTimerState()
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
