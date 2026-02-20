# 06 — UI Design

> **Status:** FINAL

## Design Language

Dark theme inspired by Lofi Girl's aesthetic. Based on the HTML mockup (`lofi-stream-mockup.html`).

## Color Palette

| Token | Hex | Usage |
|-------|-----|-------|
| bg | `#0F0A1A` | App background |
| surface | `#1A1228` | Cards, list items |
| surfaceVariant | `#251C36` | Elevated surfaces |
| surfaceElevated | `#2D2240` | Dialogs, sheets |
| primary | `#C084FC` | Primary accent |
| primaryDim | `#A855F7` | Darker primary |
| primarySubtle | `rgba(192,132,252,0.12)` | Primary tint |
| secondary | `#F59E0B` | Secondary accent (unused in this version) |
| accent | `#F9A8D4` | Gradient accent (pink) |
| textPrimary | `#F5F0FF` | Main text |
| textSecondary | `#A78BFA` | Secondary text |
| textMuted | `#6D5D8A` | Muted text |
| playerBar | `#16101F` | Bottom player bg |
| liveRed | `#EF4444` | Live indicator |
| error | `#F87171` | Error states |
| warning | `#FBBF24` | Warning states |
| success | `#34D399` | Success states |

## Typography

| Role | Font | Weight |
|------|------|--------|
| App title, section headers | Varela Round | Regular |
| Body text | Nunito Sans | 300-700 |
| Status bar, timestamps | Plus Jakarta Sans | 400-700 |

**Note:** Varela Round and Nunito Sans are loaded from Google Fonts (bundled in APK via `res/font/`).

## App Icon

AI-generated lofi radio icon featuring headphones and a vintage radio with a purple gradient. Matches the overall color palette of the app.

## Screens

### 1. Main Screen (Home)

```
┌──────────────────────────────┐
│ Lofi Girl Radio [unofficial] │  ← Top bar + Info icon
├──────────────────────────────┤
│                              │
│ ┌─ Filter streams... ──────┐ │  ← Search bar
│                              │
│ All Live Streams             │
│ ┌──────────────────────────┐ │
│ │ [thumb] beats to relax   │ │  ← Active (highlighted)
│ │         32,451 · LIVE    │ │
│ ├──────────────────────────┤ │
│ │ [thumb] beats to sleep   │ │
│ │         28,103 · LIVE    │ │
│ ├──────────────────────────┤ │
│ │ [thumb] peaceful piano   │ │
│ │         15,220 · LIVE    │ │
│ ├──────────────────────────┤ │
│ │ [thumb] dark ambient     │ │
│ │          8,910 · LIVE    │ │
│ └──────────────────────────┘ │
│                              │
├──────────────────────────────┤
│ ▶ beats to relax/study       │  ← Player bar
│   Lofi Girl · LIVE      [▶] │
└──────────────────────────────┘
```

### 2. Sleep Timer (Bottom Sheet)

- Modal bottom sheet overlaying main screen
- Preset options: 15m, 30m, 1h, 2h
- Custom time input (HH:MM picker)
- Radio-button style selection

### 3. About Screen

- App info (name, version, license)
- Links: GitHub source, lofigirl.com
- Disclaimer text

### 4. Error States

Three variants:
- **Offline** (purple icon) — No internet connection
- **Stream Down** (amber icon) — Stream temporarily unavailable
- **API Error** (red icon) — Extraction/unknown error

Each shows: icon, title, description, retry button.

## Components

### Top Bar

- App title: "Lofi Girl Radio" with `[unofficial]` badge
- Info icon (opens About screen)

### StreamListItem

- Thumbnail loaded via Coil (from stream `thumbnailUrl`)
- Live indicator (red dot)
- Stream title
- Viewer count

**Sorting:**
- Default list order: by `viewerCount` descending (most popular first)
- When search filter is active: sorted by fuzzy match score descending (best match first)

### PlayerBar

- Fixed at bottom
- Thumbnail + title + "LIVE" indicator
- Play/Pause button
- Purple gradient progress indicator at top

### Sleep Timer (Bottom Sheet)

- See Screens > Sleep Timer above
