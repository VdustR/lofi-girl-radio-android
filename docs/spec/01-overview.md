# 01 — Overview

> **Status:** FINAL

## App Purpose

Lofi Girl Radio is an unofficial Android application that provides a streamlined, audio-focused experience for listening to Lofi Girl's live YouTube streams. It serves as a lightweight radio player — no video, no accounts, just music.

## Goals

1. **Simple radio experience** — Launch app, see live channels, tap to play
2. **Background playback** — Audio continues with system notification controls
3. **Low resource usage** — Audio-only mode, no video decoding
4. **Sleep-friendly** — Built-in sleep timer with persistence across service restarts
5. **Cast support** — Send audio to Chromecast devices
6. **Offline resilience** — Graceful error handling with cached stream lists

## Non-Goals

- Video playback (removed — audio-only app)
- User accounts / login / authentication
- Premium features or in-app purchases
- Playlist management or favorites
- Downloading streams for offline use
- Support for channels other than Lofi Girl

## Target Audience

People who listen to Lofi Girl streams while studying, working, or sleeping, and want a dedicated app that doesn't require keeping YouTube open.

## Platform Requirements

- **minSdk:** 24 (Android 7.0)
- **targetSdk:** 35
- **Permissions:** INTERNET, FOREGROUND_SERVICE, FOREGROUND_SERVICE_MEDIA_PLAYBACK
