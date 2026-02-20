# 07 — Sleep Timer

> **Status:** FINAL

## Overview

Sleep timer allows users to automatically stop playback after a set duration. State persists across service restarts via DataStore Preferences.

## Presets

| Option | Duration |
|--------|----------|
| 15 minutes | 15 min |
| 30 minutes | 30 min |
| 1 hour | 60 min |
| 2 hours | 120 min |
| Custom | User-defined (HH:MM) |

## UI

- Accessed via "Sleep Timer" action chip on main screen
- Opens as `ModalBottomSheet`
- Shows preset options with radio-button selection
- Custom time input at bottom with hour/minute fields
- Active timer shows remaining time in the action chip

## State Management

### Data Class

```kotlin
data class SleepTimerState(
    val isActive: Boolean = false,
    val endTimeMillis: Long = 0L, // System.currentTimeMillis() + duration
    val selectedPreset: Int? = null // null = custom
)
```

### Persistence (DataStore)

Stored in DataStore Preferences:
- `sleep_timer_active: Boolean`
- `sleep_timer_end_time: Long`
- `sleep_timer_preset: Int` (-1 for custom)

### Why persist?

When Android kills the service and recreates it, the timer state must survive. On service restart:

1. Read timer state from DataStore
2. If `isActive && endTimeMillis > currentTime` → resume countdown
3. If `isActive && endTimeMillis <= currentTime` → timer expired while killed, stop playback

## Timer Logic

```
User selects duration
  → Calculate endTimeMillis = currentTime + durationMillis
  → Save to DataStore
  → Start countdown in ViewModel (coroutine delay)

Countdown reaches 0
  → Fade out volume over 3 seconds
  → Stop playback
  → Clear timer state in DataStore
  → Update UI

User cancels timer
  → Clear timer state in DataStore
  → Cancel countdown coroutine
```

## Edge Cases

1. **App killed by system** — Timer state in DataStore. On next service start, check and resume/expire.
2. **Phone rebooted** — Timer state persists in DataStore. Same logic as above.
3. **User switches streams** — Timer continues (it's time-based, not stream-based).
