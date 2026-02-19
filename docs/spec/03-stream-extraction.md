# 03 — Stream Extraction

> **Status:** FINAL

## Overview

We use [NewPipe Extractor](https://github.com/TeamNewPipe/NewPipeExtractor) to extract live stream information from the Lofi Girl YouTube channel without requiring a YouTube Data API key.

## Why NewPipe Extractor?

| Factor | NewPipe Extractor | youtubedl-android |
|--------|------------------|-------------------|
| APK size impact | +5-10 MB | +40-50 MB |
| Runtime | Pure Java/Kotlin | Python runtime |
| Android optimization | Yes | Wrapper |
| Maintenance | Active | Active |
| poToken support | Yes | Yes |

## Lofi Girl Channel

- **Channel ID:** `UCSJ4gkVC6NrvII8umztf0Ow`
- **Handle:** `@LofiGirl`

## Initialization

```kotlin
NewPipe.init(NewPipeDownloader.getInstance())
```

The `NewPipeDownloader` implements NewPipe's `Downloader` abstract class using OkHttp, with a browser User-Agent header to avoid bot detection. It also handles HTTP 429 responses by throwing `ReCaptchaException`.

## Fetching Live Streams

### Step 1: Get Channel Live Tab (Direct Method)

```kotlin
val service = ServiceList.YouTube
// Use getChannelTabExtractorFromId for direct access — no need to fetch the full channel page
val tabExtractor = service.getChannelTabExtractorFromId(
    "UCSJ4gkVC6NrvII8umztf0Ow",  // Channel ID
    ChannelTabs.LIVESTREAMS         // Tab constant
)
tabExtractor.fetchPage()
```

### Step 2: Extract Live Stream Items

```kotlin
val liveStreams = tabExtractor.initialPage.items
    .filterIsInstance<StreamInfoItem>()
    .filter { it.streamType == StreamType.LIVE_STREAM }
    .map { item ->
        LofiStream(
            title = item.name,
            videoId = extractVideoId(item.url),
            thumbnailUrl = item.thumbnails.firstOrNull()?.url,
            viewerCount = item.viewCount
        )
    }
```

### Step 3: Get HLS URL for Playback

```kotlin
val videoUrl = "https://www.youtube.com/watch?v=$videoId"
val streamExtractor = service.getStreamExtractor(
    service.getStreamLHFactory().fromUrl(videoUrl)
)
streamExtractor.fetchPage()
val hlsUrl = streamExtractor.hlsUrl  // HLS manifest for live streams
```

## Caching Strategy

- **Stream list:** Cached in memory. On fetch failure, return cached list.
- **HLS URLs:** NOT cached. They expire within hours. Re-extract on each play.
- **Retry:** Exponential backoff (1s, 2s, 4s) with max 3 retries.

## Known Risks

1. **YouTube HTML changes** — NewPipe Extractor scrapes YouTube HTML. Changes can break extraction.
   - Mitigation: Track upstream issues, cache last successful list, show error UI
2. **poToken** — YouTube may require integrity tokens. NewPipe Extractor handles this.
3. **Rate limiting** — Aggressive requests may trigger YouTube rate limits.
   - Mitigation: 15s timeout, no automatic polling, user-triggered refresh only
