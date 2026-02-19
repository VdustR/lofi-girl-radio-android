# 08 — Error Handling

> **Status:** FINAL

## Error Categories

### 1. No Internet Connection

- **Detection:** Network connectivity check or NewPipe throws `IOException`
- **UI:** Purple wifi-off icon + "No Internet Connection" + retry button
- **Behavior:** Show cached stream list if available, otherwise show error screen

### 2. Stream Temporarily Offline

- **Detection:** ExoPlayer playback error or NewPipe returns empty stream list
- **UI:** Amber warning icon + "Stream Temporarily Offline" + retry button
- **Behavior:** Auto-retry after 30 seconds, show message about Lofi Girl switching streams

### 3. Extraction Error (API/Scraping Failure)

- **Detection:** NewPipe Extractor throws `ExtractionException` or `ParsingException`
- **UI:** Red error icon + "Something Went Wrong" + retry button
- **Behavior:** Show cached stream list if available, suggest trying again later

## Retry Strategy

### Automatic Retry

| Scenario | Retry Count | Backoff |
|----------|------------|---------|
| Stream list fetch | 3 | Exponential (1s, 2s, 4s) |
| HLS URL extraction | 3 | Exponential (1s, 2s, 4s) |
| ExoPlayer playback error | 3 | Re-extract HLS URL + retry |

### Manual Retry

- Retry button on all error screens
- Pull-to-refresh on stream list (future enhancement)

## HLS URL Expiration

YouTube HLS manifest URLs expire after several hours. Handling:

1. ExoPlayer reports error (HTTP 403 or similar)
2. Re-extract HLS URL from NewPipe
3. Create new HlsMediaSource with fresh URL
4. Resume playback
5. If re-extraction fails → show error UI

## Caching Strategy

### Memory Cache

- Last successful `List<LofiStream>` cached in `StreamRepository`
- Used as fallback when network fetch fails
- Cleared on app process death (acceptable — it's just a fallback)

### No Disk Cache

- HLS URLs expire — no point caching them
- Stream list changes rarely — memory cache is sufficient
- Avoids complexity of cache invalidation

## User-Facing Messages

| Error | Title | Description |
|-------|-------|-------------|
| Offline | "No Internet Connection" | "Check your connection and try again. Live streams require an active internet connection." |
| Stream Down | "Stream Temporarily Offline" | "This stream is currently unavailable. Lofi Girl may be switching streams. We'll auto-reconnect." |
| Extraction | "Something Went Wrong" | "Unable to load streams. This may be a temporary issue. Please try again later." |
| Cast Failed | (Toast) | "Cast playback failed. Try 'Open in YouTube' to cast from YouTube app." |
