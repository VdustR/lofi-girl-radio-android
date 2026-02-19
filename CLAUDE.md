# Lofi Girl Radio — Android App

## Project Overview

Unofficial Lofi Girl Radio player for Android. Streams live audio from Lofi Girl's YouTube channel using NewPipe Extractor for stream extraction and Media3 ExoPlayer for HLS playback.

## Tech Stack

- **Language:** Kotlin 2.0+
- **UI:** Jetpack Compose + Material 3
- **Image loading:** Coil 3 (stream thumbnails, notification artwork)
- **Stream extraction:** NewPipe Extractor (JitPack)
- **Playback:** Media3 ExoPlayer + HLS
- **Background playback:** Media3 Session (MediaSessionService)
- **Cast:** Media3 Cast + Google Cast SDK (Default Receiver)
- **Architecture:** MVVM (ViewModel + StateFlow)
- **DI:** Manual (Application class as DI root)
- **Build:** Gradle Kotlin DSL + Version Catalogs

## Package Structure

```
com.vdustr.lofiradio
├── data/          # Data models, NewPipe downloader, stream repository
├── playback/      # PlaybackService, CastOptionsProvider
├── ui/            # Compose screens, components, PlayerBar (quality selector)
│   └── theme/     # Color, Theme, Type
├── viewmodel/     # RadioViewModel
└── util/          # FuzzyMatch helper
```

## Build Commands

```bash
./gradlew assembleDebug       # Debug build
./gradlew assembleRelease     # Release build (with R8)
./gradlew dependencies        # Check dependency tree
```

## Key Conventions

- All network operations run on `Dispatchers.IO` with 15s timeout
- HLS URLs are NOT cached (they expire); re-extract on each play
- Memory-cache the stream list for fallback on fetch failure
- `startForeground()` must be called immediately when PlaybackService starts
- Audio quality selection (Auto/High/Normal/Low) available via PlayerBar dropdown
- Stream list sorts by viewer count (descending) by default; search results sort by fuzzy match score
- Notification displays artwork thumbnail, no prev/next buttons, tap opens app

## Spec Documents

See `docs/spec/01-08` for detailed specifications:

1. [Overview](docs/spec/01-overview.md)
2. [Architecture](docs/spec/02-architecture.md)
3. [Stream Extraction](docs/spec/03-stream-extraction.md)
4. [Playback](docs/spec/04-playback.md)
5. [Cast](docs/spec/05-cast.md)
6. [UI Design](docs/spec/06-ui-design.md)
7. [Sleep Timer](docs/spec/07-sleep-timer.md)
8. [Error Handling](docs/spec/08-error-handling.md)

## Release CI

- **Tool:** [Release Please](https://github.com/googleapis/release-please) (Google)
- **Workflow:** `.github/workflows/release.yml` runs on push to `main`
- **Convention:** **All commits** must follow [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `chore:`, `docs:`, etc.)
- **Version source:** `app/build.gradle.kts` → `versionName` with `x-release-please-version` annotation
- **versionCode:** Auto-derived from versionName (`major * 10000 + minor * 100 + patch`)
- **Config:** `release-please-config.json` + `.release-please-manifest.json`

## Known Limitations

1. NewPipe Extractor depends on YouTube HTML scraping — may break when YouTube changes layout
2. YouTube HLS URLs contain session tokens — Chromecast may fail to play them
3. Vendor ROMs (MIUI, ColorOS) may kill background services aggressively
4. HLS URLs expire after a few hours — must re-extract on each play
