# 05 — Cast

> **Status:** FINAL

## Overview

Google Cast integration allows users to send audio to Chromecast devices. Uses Media3 Cast with the Default Media Receiver.

## Implementation

### CastOptionsProvider

```kotlin
class CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        return CastOptions.Builder()
            .setReceiverApplicationId("A12D4273") // Default Media Receiver
            .build()
    }
}
```

Registered in AndroidManifest:
```xml
<meta-data
    android:name="com.google.android.gms.cast.framework.OPTIONS_PROVIDER_CLASS_NAME"
    android:value="com.vdustr.lofiradio.playback.CastOptionsProvider" />
```

### Player Switching

When the user connects to a Cast device:

1. Stop local ExoPlayer
2. Create CastPlayer with the CastContext
3. Set the same media item on CastPlayer
4. Start playback on CastPlayer

When disconnecting:
1. Stop CastPlayer
2. Resume on local ExoPlayer

```kotlin
val castPlayer = CastPlayer(CastContext.getSharedInstance(context))
```

**Important:** Live streams have no position to sync. Simply start playback on the new player.

### HLS MIME Type for Cast

When building `MediaItem` for Cast playback, **must** set `MimeTypes.APPLICATION_M3U8` explicitly. Without this, Chromecast may fail to recognize the HLS stream:

```kotlin
val mediaItem = MediaItem.Builder()
    .setUri(hlsUrl)
    .setMimeType(MimeTypes.APPLICATION_M3U8)
    .build()
```

## Known Limitations

### HLS URL Session Token

YouTube HLS URLs may contain session tokens that are tied to the requesting device/IP. When sent to a Chromecast:

- The Chromecast fetches the URL from a different IP
- YouTube may reject the request
- Result: Cast playback fails silently or with an error

### Mitigation

1. Attempt Cast playback
2. If it fails within 10 seconds, show a toast/snackbar:
   > "Cast playback failed. Try 'Open in YouTube' to cast from YouTube app."
3. Offer "Open in YouTube" as fallback
4. Fall back to local playback

### Cast Button

- Shown in the PlayerBar using `CastButtonFactory`
- Only visible when Cast devices are available on the network
- Uses Media3's built-in Cast UI integration
