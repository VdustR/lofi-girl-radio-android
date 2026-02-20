# 07 — Sleep Timer

> **Status:** FINAL

## Overview

Sleep timer allows users to automatically stop playback after a set duration. State is held in-memory via ViewModel StateFlow.

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

State is held in `MutableStateFlow<SleepTimerState>` inside `RadioViewModel`. Timer countdown runs as a coroutine job in `viewModelScope`.

**Note:** Timer state does not survive process death. If the system kills the app, the timer is lost. This is acceptable for a sleep timer — the user is likely asleep and the playback service will stop on its own when the process is killed.

## Timer Logic

```
User selects duration
  → Calculate endTimeMillis = currentTime + durationMillis
  → Update StateFlow
  → Start countdown in ViewModel (coroutine delay)

Countdown reaches 0
  → Fade out volume over 3 seconds
  → Stop playback
  → Reset timer state
  → Update UI

User cancels timer
  → Cancel countdown coroutine
  → Reset timer state
```

## Edge Cases

1. **App killed by system** — Timer state is lost; playback stops when service is killed.
2. **User switches streams** — Timer continues (it's time-based, not stream-based).
