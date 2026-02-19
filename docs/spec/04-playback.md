# 04 — Playback

> **Status:** FINAL

## Overview

Audio playback uses Media3 ExoPlayer with HLS (HTTP Live Streaming) for live stream audio. Background playback is handled by a `MediaSessionService`.

## Components

### PlaybackService (MediaSessionService)

```kotlin
class PlaybackService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    // Hide prev/next buttons in notification and system UI
                    val availableCommands = MediaSession.ConnectionResult
                        .DEFAULT_SESSION_COMMANDS
                    val availablePlayerCommands = Player.Commands.Builder()
                        .addAllCommands()
                        .removeCommand(Player.COMMAND_SEEK_TO_NEXT)
                        .removeCommand(Player.COMMAND_SEEK_TO_PREVIOUS)
                        .build()
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(availableCommands)
                        .setAvailablePlayerCommands(availablePlayerCommands)
                        .build()
                }
            })
            .setSessionActivity(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .build()
    }
}
```

### HLS Playback

```kotlin
fun play(hlsUrl: String, metadata: LofiStream) {
    val mediaItem = MediaItem.Builder()
        .setUri(hlsUrl)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(metadata.title)
                .setArtist("Lofi Girl")
                .setArtworkUri(metadata.thumbnailUrl?.let { Uri.parse(it) })
                .build()
        )
        .build()

    player.setMediaItem(mediaItem)
    player.prepare()
    player.play()
}
```

### Notification

- `startForeground()` is called immediately when the service starts
- Notification shows: stream title, "Lofi Girl", play/pause button
- Artwork is loaded from `artworkUri` set in `MediaMetadata` (stream thumbnail)
- Previous and next buttons are hidden via `MediaSession.Callback.onConnect()` (only play/pause is shown)
- Tapping the notification opens the app via the `sessionActivity` PendingIntent
- Uses Media3's built-in `MediaNotification` system

### Audio Focus

- Request audio focus on play
- Duck or pause on transient focus loss
- Resume on focus regain
- ExoPlayer handles this automatically with `AudioAttributes`:

```kotlin
val audioAttributes = AudioAttributes.Builder()
    .setUsage(C.USAGE_MEDIA)
    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
    .build()
player.setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
```

## Audio Quality

### AudioQuality Enum

```kotlin
enum class AudioQuality(val maxBitrate: Int) {
    AUTO(Int.MAX_VALUE),
    HIGH(256_000),
    NORMAL(128_000),
    LOW(64_000),
}
```

### Track Selection

Audio quality is applied via `MediaController.trackSelectionParameters`:

```kotlin
mediaController.trackSelectionParameters = mediaController.trackSelectionParameters
    .buildUpon()
    .setMaxAudioBitrate(audioQuality.maxBitrate)
    .build()
```

### State Management

- `RadioViewModel` exposes `audioQuality: StateFlow<AudioQuality>` (default: `AUTO`)
- Quality selection is persisted in `DataStore Preferences`
- Changing quality takes effect immediately on the current stream (no re-extraction needed)

### UI

- The PlayerBar includes a Tune icon button that opens a `DropdownMenu`
- Menu lists all `AudioQuality` entries; the active entry is highlighted
- Selecting a quality updates the ViewModel, which applies it to the player

## Error Recovery

When ExoPlayer reports a playback error:

1. Log the error
2. Re-extract HLS URL via `StreamRepository.getHlsUrl()`
3. Retry playback with new URL
4. If retry fails, show error UI and stop playback
5. Maximum 3 automatic retries

## Lifecycle

- Service starts when user taps a channel
- Service runs as foreground service with notification
- Service stops when:
  - User presses stop
  - Sleep timer expires
  - All retries exhausted
  - User removes notification
