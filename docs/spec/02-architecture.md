# 02 — Architecture

> **Status:** FINAL

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Kotlin | 2.0+ |
| UI | Jetpack Compose + Material 3 | BOM 2024.x |
| Stream extraction | NewPipe Extractor | 0.25.2 (JitPack) |
| Audio playback | Media3 ExoPlayer + HLS | 1.9.x |
| Background playback | Media3 Session | 1.9.x |
| Image loading | Coil 3 | 3.3.0 |
| Architecture | MVVM | — |
| DI | Manual (Application class) | — |
| HTTP | OkHttp | 4.12.x |
| Build | Gradle Kotlin DSL + Version Catalogs | 8.x |

## Architecture Pattern: MVVM

```
View (Compose)  ←→  ViewModel  ←→  Repository  ←→  NewPipe Extractor
                                                ←→  PlaybackService
```

### Layers

1. **UI Layer** — Jetpack Compose screens observe `StateFlow` from ViewModel
2. **ViewModel** — `RadioViewModel` holds all UI state, delegates to repository and service
3. **Data Layer** — `StreamRepository` wraps NewPipe Extractor calls, manages caching
4. **Playback Layer** — `PlaybackService` (MediaSessionService) manages ExoPlayer

## Dependency Injection

Manual DI via `LofiRadioApp` (Application class):

```kotlin
class LofiRadioApp : Application(), SingletonImageLoader.Factory {
    lateinit var streamRepository: StreamRepository
    lateinit var okHttpClient: OkHttpClient
    // ... created in onCreate()

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .okHttpClient(okHttpClient)  // Reuse existing OkHttpClient
            .build()
    }
}
```

ViewModels access dependencies through the Application instance. This avoids adding Hilt/Koin for a small app.

`LofiRadioApp` also implements `SingletonImageLoader.Factory` so that Coil shares the same `OkHttpClient` used by NewPipe Downloader, keeping a single connection pool and consistent timeouts.

## Data Flow

```
App Start
  → StreamRepository.fetchLiveStreams()
    → NewPipe ChannelTabExtractor → Live tab
    → Filter LIVE_STREAM only
    → Return List<LofiStream>
    → Cache in memory

User Taps Channel
  → StreamRepository.getHlsUrl(videoId)
    → NewPipe StreamExtractor → HLS manifest URL
  → PlaybackService.play(hlsUrl, metadata)
    → ExoPlayer (HlsMediaSource)
    → MediaMetadata includes artworkUri (thumbnail URL for notification)

```

## Threading

- All NewPipe Extractor calls run on `Dispatchers.IO`
- Network timeout: 15 seconds
- UI state updates via `StateFlow` (main thread safe)
