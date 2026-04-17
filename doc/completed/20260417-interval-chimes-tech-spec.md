# Interval Chimes Tech Spec

## Overview

Technical specification for interval chimes, implementing the behavior defined in
`interval-chimes-func-spec.md` and the UI defined in `interval-chimes-ux-design.md`.
Interval chimes are repeating audio cues that fire at a fixed cadence during a timed
activity. The interval is ephemeral state — no database changes are needed.

## Domain Entities

No new entities or database changes. The interval is ViewModel-only state that lives
for the duration of a session.

### Available Intervals

Define a fixed list of intervals as a constant. This list is the single source of
truth for both the UI chips and validation.

File: `app/src/main/kotlin/com/habit/viewmodel/IntervalChime.kt`

```kotlin
enum class IntervalChimeState { IDLE, SELECTING, RUNNING }

object IntervalOptions {
    val seconds = listOf(6, 7, 8, 9, 10, 11, 12)
    val minutes = listOf(3, 4, 5, 8, 10)

    fun labelFor(ms: Long): String {
        val sec = ms / 1000
        return if (sec < 60) "${sec}s" else "${sec / 60}m"
    }

    fun isSecondsInterval(ms: Long): Boolean = ms < 60_000
}
```

## ViewModel Layer

### AgendaUiState Changes

File: `app/src/main/kotlin/com/habit/viewmodel/AgendaUiState.kt`

Add three fields:

```kotlin
data class AgendaUiState(
    // ... existing fields ...
    val intervalChimeState: IntervalChimeState = IntervalChimeState.IDLE,
    val intervalChimeMs: Long = 0,
    val intervalCountdownMs: Long = 0
)
```

- `intervalChimeState` — which of the three UI states the control is in.
- `intervalChimeMs` — the selected interval in milliseconds (0 when idle).
- `intervalCountdownMs` — countdown to the next chime, updated by the tick loop.

### AgendaViewModel Changes

File: `app/src/main/kotlin/com/habit/viewmodel/AgendaViewModel.kt`

Add a tracking field for the next chime time:

```kotlin
private var nextIntervalChimeAtMs: Long = 0
```

New methods:

```kotlin
fun openIntervalSelector() {
    _uiState.value = _uiState.value.copy(
        intervalChimeState = IntervalChimeState.SELECTING
    )
}

fun closeIntervalSelector() {
    _uiState.value = _uiState.value.copy(
        intervalChimeState = IntervalChimeState.IDLE
    )
}

fun startIntervalChime(intervalMs: Long) {
    val state = _uiState.value

    // auto-start timer if not running
    if (!state.timerRunning) {
        startTimer()
    }

    // fire the first chime immediately
    val isSeconds = IntervalOptions.isSecondsInterval(intervalMs)
    _chimeEvents.tryEmit(ChimeEvent.Interval(isSeconds))

    val elapsed = _uiState.value.activeActivity?.elapsedMs ?: 0
    nextIntervalChimeAtMs = elapsed + intervalMs

    _uiState.value = _uiState.value.copy(
        intervalChimeState = IntervalChimeState.RUNNING,
        intervalChimeMs = intervalMs,
        intervalCountdownMs = intervalMs
    )
}

fun cancelIntervalChime() {
    nextIntervalChimeAtMs = 0
    _uiState.value = _uiState.value.copy(
        intervalChimeState = IntervalChimeState.IDLE,
        intervalChimeMs = 0,
        intervalCountdownMs = 0
    )
}
```

### Timer Tick Loop Changes

The existing `startTimerTick()` method's tick loop adds interval chime logic
after the existing threshold chime checks:

```kotlin
// inside the while (isActive) loop, after threshold checks:
val chimeMs = _uiState.value.intervalChimeMs
if (chimeMs > 0 && elapsed >= nextIntervalChimeAtMs) {
    val isSeconds = IntervalOptions.isSecondsInterval(chimeMs)
    _chimeEvents.tryEmit(ChimeEvent.Interval(isSeconds))
    nextIntervalChimeAtMs += chimeMs
}
if (chimeMs > 0) {
    _uiState.value = _uiState.value.copy(
        intervalCountdownMs = maxOf(0, nextIntervalChimeAtMs - elapsed)
    )
}
```

### Clearing Interval State on Activity End

`completeActivity()`, `cancelTimer()`, and `skipActivity()` all already clear
timer state. Add interval clearing to each:

```kotlin
intervalChimeState = IntervalChimeState.IDLE,
intervalChimeMs = 0,
intervalCountdownMs = 0
```

Also reset the tracking field:

```kotlin
nextIntervalChimeAtMs = 0
```

## ChimeEvent Changes

File: `app/src/main/kotlin/com/habit/viewmodel/ChimeEvent.kt`

```kotlin
sealed class ChimeEvent {
    data object Threshold : ChimeEvent()
    data class Interval(val isSeconds: Boolean) : ChimeEvent()
}
```

The `isSeconds` flag tells the player which sound to use.

## Service Layer

### ChimePlayer Changes

File: `app/src/main/kotlin/com/habit/service/ChimePlayer.kt`

Add a method for interval chimes. Seconds intervals use `ToneGenerator` for a
short programmatic tone. Minutes intervals use the notification sound without
vibration.

```kotlin
import android.media.ToneGenerator
import android.media.AudioManager

class ChimePlayer(private val context: Context) {

    private var toneGenerator: ToneGenerator? = null

    fun playThresholdChime() {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        RingtoneManager.getRingtone(context, uri)?.play()
        vibrate()
    }

    fun playIntervalChime(isSeconds: Boolean) {
        if (isSeconds) {
            if (toneGenerator == null) {
                toneGenerator = ToneGenerator(
                    AudioManager.STREAM_NOTIFICATION, 80
                )
            }
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 200)
        } else {
            val uri = RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_NOTIFICATION
            )
            RingtoneManager.getRingtone(context, uri)?.play()
            // no vibration for interval chimes
        }
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }
}
```

*Note:* the `TONE_PROP_BEEP2` constant and volume level (80 out of 100) are
starting points. tune during implementation by testing on the phone.

### TimerService Changes

File: `app/src/main/kotlin/com/habit/service/TimerService.kt`

The service needs to handle interval chimes when the app is backgrounded.
Add interval tracking fields and two new actions.

New fields:

```kotlin
private var intervalMs: Long = 0
private var nextIntervalAtMs: Long = 0
```

New actions:

```kotlin
companion object {
    // ... existing constants ...
    const val ACTION_START_INTERVAL = "com.habit.timer.START_INTERVAL"
    const val ACTION_STOP_INTERVAL = "com.habit.timer.STOP_INTERVAL"
    const val EXTRA_INTERVAL_MS = "interval_ms"
    const val EXTRA_CURRENT_ELAPSED_MS = "current_elapsed_ms"
    const val EXTRA_IS_SECONDS = "is_seconds"

    fun startIntervalIntent(
        context: Context,
        intervalMs: Long,
        currentElapsedMs: Long,
        isSeconds: Boolean
    ): Intent = Intent(context, TimerService::class.java).apply {
        action = ACTION_START_INTERVAL
        putExtra(EXTRA_INTERVAL_MS, intervalMs)
        putExtra(EXTRA_CURRENT_ELAPSED_MS, currentElapsedMs)
        putExtra(EXTRA_IS_SECONDS, isSeconds)
    }

    fun stopIntervalIntent(context: Context): Intent =
        Intent(context, TimerService::class.java).apply {
            action = ACTION_STOP_INTERVAL
        }
}
```

Handle in `onStartCommand`:

```kotlin
ACTION_START_INTERVAL -> {
    intervalMs = intent.getLongExtra(EXTRA_INTERVAL_MS, 0)
    val currentElapsed = intent.getLongExtra(EXTRA_CURRENT_ELAPSED_MS, 0)
    isSecondsInterval = intent.getBooleanExtra(EXTRA_IS_SECONDS, true)
    nextIntervalAtMs = currentElapsed + intervalMs
}
ACTION_STOP_INTERVAL -> {
    intervalMs = 0
    nextIntervalAtMs = 0
}
```

Add to the existing tick loop, after the threshold chime check:

```kotlin
if (intervalMs > 0) {
    val elapsed = System.currentTimeMillis() - startEpochMs
    if (elapsed >= nextIntervalAtMs) {
        chimePlayer?.playIntervalChime(isSecondsInterval)
        nextIntervalAtMs += intervalMs
    }
}
```

When `ACTION_STOP` fires (timer cancelled or activity completed), interval
state is implicitly cleared because the service stops.

### MainActivity Changes

File: `app/src/main/kotlin/com/habit/MainActivity.kt`

Update the `chimeEvents` collector to handle the new event type:

```kotlin
viewModel.chimeEvents.collect { event ->
    when (event) {
        is ChimeEvent.Threshold -> chimePlayer.playThresholdChime()
        is ChimeEvent.Interval ->
            chimePlayer.playIntervalChime(event.isSeconds)
    }
}
```

Add a state observer to communicate interval changes to the service.
This runs alongside the existing timer service observer:

```kotlin
lifecycleScope.launch {
    var intervalRunning = false
    viewModel.uiState.collect { state ->
        if (state.intervalChimeMs > 0 && serviceRunning && !intervalRunning) {
            startService(TimerService.startIntervalIntent(
                context = this@MainActivity,
                intervalMs = state.intervalChimeMs,
                currentElapsedMs = state.activeActivity?.elapsedMs ?: 0,
                isSeconds = IntervalOptions.isSecondsInterval(
                    state.intervalChimeMs
                )
            ))
            intervalRunning = true
        } else if (state.intervalChimeMs == 0L && intervalRunning) {
            startService(
                TimerService.stopIntervalIntent(this@MainActivity)
            )
            intervalRunning = false
        }
    }
}
```

## UI Layer

### IntervalChimeControl Composable

File: `app/src/main/kotlin/com/habit/ui/IntervalChimeControl.kt`

A single composable that renders one of three states based on
`IntervalChimeState`. See `interval-chimes-ux-design.md` for layout details.

```kotlin
@Composable
fun IntervalChimeControl(
    state: IntervalChimeState,
    intervalMs: Long,
    countdownMs: Long,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    onSelect: (Long) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
)
```

#### Idle State

A right-justified `TextButton` labeled "Interval Chime."

#### Selecting State

Two rows of `FilterChip` or small `Button` composables for each interval
value, plus a right-justified "Close" `TextButton`. Seconds row first,
minutes row second.

```kotlin
Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    IntervalOptions.seconds.forEach { sec ->
        FilterChip(
            selected = false,
            onClick = { onSelect(sec * 1000L) },
            label = { Text("${sec}s") }
        )
    }
}
Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    IntervalOptions.minutes.forEach { min ->
        FilterChip(
            selected = false,
            onClick = { onSelect(min * 60_000L) },
            label = { Text("${min}m") }
        )
    }
}
```

#### Running State

A row showing the active interval label, countdown, and a cancel button:

```kotlin
Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
) {
    Text("every ${IntervalOptions.labelFor(intervalMs)}")
    val countdownText = formatCountdown(countdownMs, intervalMs)
    Text(
        text = " — next in $countdownText",
        modifier = Modifier.weight(1f)
    )
    TextButton(onClick = onCancel) { Text("Cancel") }
}
```

Countdown formatting:
- Seconds intervals: bare number (e.g., "4")
- Minutes intervals: m:ss (e.g., "2:47")

```kotlin
private fun formatCountdown(countdownMs: Long, intervalMs: Long): String {
    val totalSec = (countdownMs + 999) / 1000  // round up
    return if (IntervalOptions.isSecondsInterval(intervalMs)) {
        "$totalSec"
    } else {
        val min = totalSec / 60
        val sec = totalSec % 60
        "%d:%02d".format(min, sec)
    }
}
```

### ActivityView Integration

File: `app/src/main/kotlin/com/habit/ui/ActivityView.kt`

In `CurrentActivityView`, add `IntervalChimeControl` between the
`TimerDisplay` and `EditableActivityTimes`. Only show for timed habits
when the activity is not completed:

```kotlin
if (habit.timed) {
    TimerDisplay(...)
}

val activity = state.activeActivity
if (habit.timed && activity?.completedAt == null) {
    IntervalChimeControl(
        state = state.intervalChimeState,
        intervalMs = state.intervalChimeMs,
        countdownMs = state.intervalCountdownMs,
        onOpen = onOpenIntervalSelector,
        onClose = onCloseIntervalSelector,
        onSelect = onStartIntervalChime,
        onCancel = onCancelIntervalChime
    )
}

EditableActivityTimes(...)
```

Thread the four new callbacks through the composable hierarchy:

- `ActivityView` → `HabitView` → `CurrentActivityView`
- `onOpenIntervalSelector: () -> Unit`
- `onCloseIntervalSelector: () -> Unit`
- `onStartIntervalChime: (Long) -> Unit`
- `onCancelIntervalChime: () -> Unit`

### PrimaryScreen Wiring

File: `app/src/main/kotlin/com/habit/ui/PrimaryScreen.kt`

Wire the new callbacks to ViewModel methods:

```kotlin
ActivityView(
    // ... existing callbacks ...
    onOpenIntervalSelector = viewModel::openIntervalSelector,
    onCloseIntervalSelector = viewModel::closeIntervalSelector,
    onStartIntervalChime = viewModel::startIntervalChime,
    onCancelIntervalChime = viewModel::cancelIntervalChime
)
```

## Navigation

No new routes. All interval chime UI is inline in the activity view.

## Dependency Wiring

No new dependencies. `ChimePlayer` and `TimerService` already exist and are
already wired. The `ToneGenerator` is created lazily inside `ChimePlayer`.

## Testing Plan

### ViewModel Tests

File: `app/src/test/kotlin/com/habit/viewmodel/AgendaViewModelTest.kt`

- `openIntervalSelector` sets state to SELECTING.
- `closeIntervalSelector` returns state to IDLE.
- `startIntervalChime` sets state to RUNNING, sets `intervalChimeMs`,
  emits an immediate `ChimeEvent.Interval`.
- `startIntervalChime` auto-starts the timer if not running.
- `cancelIntervalChime` clears interval state, returns to IDLE.
- tick loop emits `ChimeEvent.Interval` at the correct cadence.
- tick loop updates `intervalCountdownMs` on each tick.
- `completeActivity` clears interval state.
- `cancelTimer` clears interval state.
- `skipActivity` clears interval state.
- seconds vs. minutes intervals emit correct `isSeconds` flag.

### ChimePlayer Tests

- `playIntervalChime(true)` uses `ToneGenerator` (verify no crash,
  no vibration).
- `playIntervalChime(false)` uses `RingtoneManager` (verify no vibration).
- `release()` cleans up `ToneGenerator`.

### Service Tests

- `ACTION_START_INTERVAL` sets interval tracking fields.
- `ACTION_STOP_INTERVAL` clears interval tracking fields.
- `ACTION_STOP` clears all state including interval.
- tick loop fires interval chimes at correct cadence when interval is set.

### IntervalOptions Tests

- `labelFor` returns correct strings ("8s", "3m").
- `isSecondsInterval` returns true for < 60000, false otherwise.

### Compose Functional Tests

- idle state shows "Interval Chime" button for timed habits.
- tapping button shows selecting state with chip rows.
- tapping a chip transitions to running state with countdown.
- tapping Cancel returns to idle.
- tapping Close in selecting state returns to idle.
- interval chime control hidden for untimed habits.
- interval chime control hidden for completed activities.
