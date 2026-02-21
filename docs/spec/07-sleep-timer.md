# 07 — Sleep Timer

> **Status:** FINAL

## Overview

Sleep timer allows users to automatically stop playback after a set duration or at a specific time. State is held in-memory via ViewModel StateFlow.

## UI

### Timer Icon (TopBar)

- Location: TopBar, left of Info icon (always visible)
- Inactive: `TextSecondary` color icon
- Active: `Primary` color icon (no countdown text in TopBar)
- Tap opens `ModalBottomSheet`

### Sheet Layout — Duration Mode

```
┌──────────────────────────────────────┐
│ Sleep Timer                    14:57 │ ← remaining (only when active)
│                                      │
│  ┌Duration──┐ ┌At Time──┐            │ ← M3 SegmentedButton
│                                      │
│  [15m] [30m] [1h] [2h]              │ ← preset chips (tap = instant start)
│                                      │
│  ┌─ Custom ──────────────────────┐  │
│  │  [▲]       [▲]               │  │
│  │ [ 0 h ] : [45 m]    [Start]  │  │
│  │  [▼]       [▼]               │  │
│  └───────────────────────────────┘  │
│                                      │
│  [Cancel Timer]                      │ ← only when active
└──────────────────────────────────────┘
```

### Sheet Layout — At Time Mode

```
┌──────────────────────────────────────┐
│ Sleep Timer                          │
│                                      │
│  ┌Duration──┐ ┌At Time──┐            │
│                                      │
│     [▲]       [▲]                   │
│    [02 H ] : [30 M ]                │
│     [▼]       [▼]                   │
│                                      │
│  Stops in ~2 hours 15 minutes        │ ← dynamic preview
│  (tomorrow)                          │ ← only when crossing midnight
│                                      │
│  [Start Timer]                       │
│  [Cancel Timer]                      │ ← only when active
└──────────────────────────────────────┘
```

## Presets

| Option | Duration | Behavior |
|--------|----------|----------|
| 15m | 15 min | Tap = immediate start + close sheet |
| 30m | 30 min | Tap = immediate start + close sheet |
| 1h | 60 min | Tap = immediate start + close sheet |
| 2h | 120 min | Tap = immediate start + close sheet |

Presets only appear in Duration mode.

## State Management

### Data Class (ViewModel)

```kotlin
data class SleepTimerState(
    val isActive: Boolean = false,
    val remainingMillis: Long = 0L
)
```

ViewModel only tracks the running timer. Sheet-local state handles mode and draft values.

State is held in `MutableStateFlow<SleepTimerState>` inside `RadioViewModel`. Timer countdown runs as a coroutine job in `viewModelScope`.

### Sheet-Local State

```kotlin
var selectedMode by remember { mutableStateOf(TimerMode.DURATION) }
var draftHours by remember { mutableIntStateOf(0) }
var draftMinutes by remember { mutableIntStateOf(30) }
var draftTargetHour by remember { mutableIntStateOf(LocalTime.now().hour) }
var draftTargetMinute by remember { mutableIntStateOf(0) }
```

Draft state resets on sheet dismiss (expected behavior — `ModalBottomSheet` leaves composition on dismiss).

**Note:** Timer state does not survive process death. If the system kills the app, the timer is lost. This is acceptable for a sleep timer — the user is likely asleep and the playback service will stop on its own when the process is killed.

## Timer Logic

### Duration Mode

```
User taps preset or Start button
  → Calculate durationMillis = hours * 3_600_000L + minutes * 60_000L
  → Call viewModel.startSleepTimer(durationMillis)
  → Close sheet
```

### At Time Mode

```
User sets target time (HH:MM) and taps Start
  → Calculate durationMillis via durationUntilTime(hour, minute)
  → Call viewModel.startSleepTimer(durationMillis)
  → Close sheet
```

Cross-midnight calculation using `java.time`:

```kotlin
fun durationUntilTime(hour: Int, minute: Int): Long {
    val now = ZonedDateTime.now()
    var target = now.with(LocalTime.of(hour, minute, 0))
    if (!target.isAfter(now)) target = target.plusDays(1)
    return target.toInstant().toEpochMilli() - System.currentTimeMillis()
}
```

### ViewModel Countdown

```
startSleepTimer(durationMillis: Long)
  → endTime = SystemClock.elapsedRealtime() + durationMillis
  → Start countdown in ViewModel (coroutine, 1s tick)
  → Update remainingMillis each second

Countdown reaches 0
  → Reset timer state
  → Pause playback

User cancels timer
  → Cancel countdown coroutine
  → Reset timer state
```

### Validation

- **Duration mode:** Start button disabled when total duration = 0 (hours=0 and minutes=0)
- **At Time mode:** Start button disabled when `durationUntilTime` < 60,000ms (less than 1 minute)
- Both modes enforce minimum 1-minute duration to prevent instant-stop

### Stepper Boundary Behavior

- ▲ button disabled at max value (23 for hours, 59 for minutes)
- ▼ button disabled at min value (0)
- No wrap-around

## Edge Cases

1. **App killed by system** — Timer state is lost; playback stops when service is killed.
2. **User switches streams** — Timer continues (it's time-based, not stream-based).
3. **Cross-midnight (At Time)** — If target time ≤ current time, adds 1 day automatically. DST-safe via `java.time.ZonedDateTime`.
4. **At Time defaults** — Hour = current hour, minute = 0. Dynamic preview shows duration and "(tomorrow)" indicator when crossing midnight.
