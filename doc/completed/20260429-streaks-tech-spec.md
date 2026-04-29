# Streaks Tech Spec

## Overview

Technical specification for the streaks feature, implementing the
behavior defined in `streaks-func-spec.md` and the UI described in
`streaks-ux-design.md`. Adds streak computation to the tally list,
replaces the tally editor with a Details screen, and adds the Record
First No action. Uses the existing MVVM + Room + manual DI architecture.

No schema migration is required — streak computation uses existing
`Choice` data, and Record First No inserts ordinary `Choice` entities.

## Database Layer

### New DAO Queries

Add the following queries to `ChoiceDao.kt`:

```kotlin
@Query(
    "SELECT * FROM choice WHERE tallyId = :tallyId " +
    "ORDER BY timestamp DESC LIMIT 1"
)
suspend fun mostRecentChoice(tallyId: String): Choice?

@Query(
    "SELECT * FROM choice WHERE tallyId = :tallyId " +
    "AND abstained = 0 ORDER BY timestamp DESC LIMIT 1"
)
suspend fun mostRecentIndulgence(tallyId: String): Choice?

@Query(
    "SELECT * FROM choice WHERE tallyId = :tallyId " +
    "AND abstained = 1 AND timestamp > :after " +
    "ORDER BY timestamp ASC LIMIT 1"
)
suspend fun firstAbstentionAfter(
    tallyId: String,
    after: Long
): Choice?

@Query(
    "SELECT * FROM choice WHERE tallyId = :tallyId " +
    "AND abstained = 1 ORDER BY timestamp ASC LIMIT 1"
)
suspend fun firstAbstention(tallyId: String): Choice?

@Query(
    "SELECT * FROM choice WHERE tallyId = :tallyId " +
    "ORDER BY timestamp ASC LIMIT 1"
)
suspend fun earliestChoice(tallyId: String): Choice?
```

All queries are indexed on `tallyId` and `timestamp` (indices already
exist from `MIGRATION_5_6`).

## Repository Layer

### ChoiceRepository

Add methods that delegate to the new DAO queries, converting
`Instant`/`Long` as needed:

```kotlin
suspend fun mostRecentChoice(tallyId: String): Choice? =
    choiceDao.mostRecentChoice(tallyId)

suspend fun mostRecentIndulgence(tallyId: String): Choice? =
    choiceDao.mostRecentIndulgence(tallyId)

suspend fun firstAbstentionAfter(
    tallyId: String,
    after: Instant
): Choice? =
    choiceDao.firstAbstentionAfter(tallyId, after.toEpochMilli())

suspend fun firstAbstention(tallyId: String): Choice? =
    choiceDao.firstAbstention(tallyId)

suspend fun earliestChoice(tallyId: String): Choice? =
    choiceDao.earliestChoice(tallyId)
```

## ViewModel Layer

### Streak Computation

Add a utility function for computing the current streak start from
repository data. This logic is used by both the list view and the
Details screen.

File: `app/src/main/kotlin/com/habit/viewmodel/StreakCalculator.kt`

```kotlin
class StreakCalculator(private val choiceRepo: ChoiceRepository) {

    suspend fun currentStreakStart(tallyId: String): Instant? {
        val mostRecent = choiceRepo.mostRecentChoice(tallyId)
            ?: return null
        if (!mostRecent.abstained) return null

        val lastYes = choiceRepo.mostRecentIndulgence(tallyId)
        val streakChoice = if (lastYes != null) {
            choiceRepo.firstAbstentionAfter(tallyId, lastYes.timestamp)
        } else {
            choiceRepo.firstAbstention(tallyId)
        }
        return streakChoice?.timestamp
    }
}
```

Returns `null` if there is no current streak (no choices, or most recent
choice was a Yes). Otherwise returns the `Instant` when the streak began.

### Streak Formatting

Add a utility for formatting streak duration into display text.

File: `app/src/main/kotlin/com/habit/viewmodel/StreakFormatter.kt`

```kotlin
fun formatStreakDuration(
    start: Instant,
    now: Instant
): String? {
    val duration = Duration.between(start, now)
    val totalDays = duration.toDays()
    if (totalDays < 1) return null

    val startDate = start.atZone(ZoneId.systemDefault()).toLocalDate()
    val nowDate = now.atZone(ZoneId.systemDefault()).toLocalDate()
    val period = Period.between(startDate, nowDate)

    val years = period.years
    val months = period.years * 12 + period.months

    return when {
        months >= 12 -> "$years year streak"
        months >= 2 -> "$months month streak"
        else -> "$totalDays day streak"
    }
}
```

Uses `Duration.between` for the day count (ensures 23h59m does not count
as a day) and `Period.between` for calendar month/year calculation (which
floors naturally).

### TallyDisplayItem Changes

Add a streak start field:

```kotlin
data class TallyDisplayItem(
    val tally: Tally,
    val abstainCount: Int,
    val totalCount: Int,
    val ratio: Float,
    val sortScore: Float,
    val streakStart: Instant?
)
```

The `streakStart` field is `null` when there is no current streak.

### ChoicesViewModel Changes

Update `refreshDisplay()` to compute streak starts using
`StreakCalculator`:

```kotlin
class ChoicesViewModel(
    private val tallyRepo: TallyRepository,
    private val choiceRepo: ChoiceRepository,
    private val dayBoundary: DayBoundary,
    private val streakCalculator: StreakCalculator
) : ViewModel() {

    // in refreshDisplay():
    val items = tallies.map { tally ->
        // ... existing indicator logic (kept for Details screen) ...

        val streakStart = streakCalculator.currentStreakStart(tally.id)

        TallyDisplayItem(
            // ... existing fields ...
            streakStart = streakStart
        )
    }
}
```

The list row reads `streakStart` and formats it using
`formatStreakDuration()`. The N/10 values (`abstainCount`, `totalCount`)
remain on `TallyDisplayItem` for use by the Details screen.

### TallyDetailsViewModel

Rename `TallyEditorViewModel` to `TallyDetailsViewModel`. Add streak,
stats, and Record First No support.

File:
`app/src/main/kotlin/com/habit/viewmodel/TallyDetailsViewModel.kt`

```kotlin
data class TallyDetailsState(
    val id: String = "",
    val name: String = "",
    val priority: Priority = Priority.MEDIUM,
    val isNew: Boolean = true,
    val dirty: Boolean = false,
    val saved: Boolean = false,
    val deleted: Boolean = false,
    val streakStart: Instant? = null,
    val abstainCountLast10: Int = 0,
    val totalCountLast10: Int = 0,
    val abstainCountToday: Int = 0,
    val totalCountToday: Int = 0,
    val hasChoices: Boolean = false,
    val earliestChoiceDate: LocalDate? = null
) {
    val isValid: Boolean get() = name.isNotBlank()
    val showTodayStats: Boolean get() = totalCountToday >= 3
}

class TallyDetailsViewModel(
    private val tallyRepo: TallyRepository,
    private val choiceRepo: ChoiceRepository,
    private val dayBoundary: DayBoundary,
    private val streakCalculator: StreakCalculator
) : ViewModel() {

    fun loadTally(tallyId: String) {
        viewModelScope.launch {
            tallyRepo.getById(tallyId)?.let { tally ->
                _state.value = TallyDetailsState(
                    id = tally.id,
                    name = tally.name,
                    priority = tally.priority,
                    isNew = false
                )
                refreshStats(tally.id)
            }
        }
    }

    private suspend fun refreshStats(tallyId: String) {
        val streakStart = streakCalculator.currentStreakStart(tallyId)
        val recent = choiceRepo.recentChoices(tallyId, 10)
        val today = dayBoundary.today()
        val dayStart = today.atStartOfDay(
            ZoneId.systemDefault()
        ).toInstant()
        val dayEnd = today.plusDays(1).atStartOfDay(
            ZoneId.systemDefault()
        ).toInstant()
        val todayChoices = choiceRepo.choicesToday(
            tallyId, dayStart, dayEnd
        )
        val earliest = choiceRepo.earliestChoice(tallyId)

        _state.value = _state.value.copy(
            streakStart = streakStart,
            abstainCountLast10 = recent.count { it.abstained },
            totalCountLast10 = recent.size,
            abstainCountToday = todayChoices.count { it.abstained },
            totalCountToday = todayChoices.size,
            hasChoices = earliest != null,
            earliestChoiceDate = earliest?.timestamp?.let {
                dayBoundary.attributedDate(it)
            }
        )
    }

    fun recordFirstNo(date: LocalDate) {
        val s = _state.value
        if (s.isNew) return
        viewModelScope.launch {
            val timestamp = date.atStartOfDay(
                ZoneId.systemDefault()
            ).toInstant()
            choiceRepo.record(
                Choice(
                    tallyId = s.id,
                    timestamp = timestamp,
                    abstained = true
                )
            )
            refreshStats(s.id)
        }
    }

    // setName, setPriority, save, delete — same as current
    // TallyEditorViewModel
}
```

### TallyDetailsViewModelFactory

Rename `TallyEditorViewModelFactory` and update constructor to pass
the additional dependencies:

```kotlin
class TallyDetailsViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TallyDetailsViewModel(
            container.tallyRepo,
            container.choiceRepo,
            container.dayBoundary,
            StreakCalculator(container.choiceRepo)
        ) as T
    }
}
```

## UI Layer

### TallyRow Changes

File: `app/src/main/kotlin/com/habit/ui/ChoicesScreen.kt`

Remove the edit icon button. Make the tally name tappable (add
`clickable` modifier with `onDetails` callback). Replace the N/10
indicator with streak text.

```kotlin
@Composable
fun TallyRow(
    item: TallyDisplayItem,
    onNo: () -> Unit,
    onYes: () -> Unit,
    onDetails: () -> Unit
) {
    Row(/* existing modifiers */) {
        Text(
            text = item.tally.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .clickable { onDetails() }
        )

        val streakText = item.streakStart?.let {
            formatStreakDuration(it, Instant.now())
        }
        if (streakText != null) {
            Text(
                text = streakText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Green
            )
        }

        OutlinedButton(onClick = onNo) { Text("No") }
        OutlinedButton(onClick = onYes) { Text("Yes") }
    }
}
```

### TallyDetailsScreen

File: `app/src/main/kotlin/com/habit/ui/TallyDetailsScreen.kt`

Replaces `TallyEditorScreen.kt`. Adds streak display, choice stats,
and Record First No below the existing editing fields.

Key composable sections:

1. **Top bar** — back arrow, tally name as title, Save button.
2. **Name field** — `OutlinedTextField`, same as current editor.
3. **Priority selector** — dropdown, same as current editor.
4. **Streak section** — section divider + streak duration + start date.
   Hidden when no choices exist. Shows "no current streak" in muted
   text when the most recent choice was a Yes.
5. **Choices section** — section divider + last 10 stats + today stats
   (if 3+ today). Hidden when no choices exist. Color-coded with the
   existing `indicatorColor()` function.
6. **Record First No button** — opens `DatePickerDialog`. Constrains
   max date to the day before `earliestChoiceDate`, or today if no
   choices exist.
7. **Delete button** — same as current editor.

### Date Picker for Record First No

Use Material3 `DatePickerDialog` with `rememberDatePickerState()`.
Set `selectableDates` to constrain the range:

```kotlin
val maxDateMillis = state.earliestChoiceDate?.let {
    it.minusDays(1)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
} ?: Instant.now().toEpochMilli()

val datePickerState = rememberDatePickerState(
    selectableDates = object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
            return utcTimeMillis <= maxDateMillis
        }
    }
)
```

## Navigation

Update routes in `AppNavigation.kt`:

- Rename `"tally-editor/{tallyId}"` to `"tally-details/{tallyId}"`.
- Update the `ChoicesScreen` callback from `onEditTally` to
  `onDetails`.
- Replace `TallyEditorViewModelFactory` with
  `TallyDetailsViewModelFactory`.

```kotlin
composable("tally-details/{tallyId}") { backStackEntry ->
    val tallyIdStr =
        backStackEntry.arguments?.getString("tallyId")
    val detailsVm: TallyDetailsViewModel = viewModel(
        factory = TallyDetailsViewModelFactory(container)
    )
    TallyDetailsScreen(
        viewModel = detailsVm,
        tallyId = if (tallyIdStr == "new") null
                  else tallyIdStr,
        onBack = { navController.popBackStack() }
    )
}
```

The `ChoicesScreen` callback changes:

```kotlin
ChoicesScreen(
    viewModel = choicesVm,
    onDetails = { tallyId ->
        navController.navigate("tally-details/$tallyId")
    },
    onNewTally = {
        navController.navigate("tally-details/new")
    },
    onBack = { navController.popBackStack() }
)
```

## Dependency Wiring

### AppContainer

Add `StreakCalculator` as a shared instance:

```kotlin
class AppContainer(context: Context) {
    // ... existing fields ...

    val streakCalculator = StreakCalculator(choiceRepo)
}
```

Update `ChoicesViewModelFactory` to pass `streakCalculator`.

## File Changes Summary

| Action | File                                     |
|--------|------------------------------------------|
| Modify | `ChoiceDao.kt` — add 5 queries           |
| Modify | `ChoiceRepository.kt` — add 5 methods    |
| Create | `StreakCalculator.kt`                     |
| Create | `StreakFormatter.kt`                      |
| Modify | `TallyDisplayItem.kt` — add streakStart  |
| Modify | `ChoicesViewModel.kt` — compute streaks   |
| Rename | `TallyEditorViewModel` → Details          |
| Rename | `TallyEditorState` → `TallyDetailsState`  |
| Rename | `TallyEditorViewModelFactory` → Details   |
| Modify | `ChoicesScreen.kt` — row changes           |
| Rename | `TallyEditorScreen` → `TallyDetailsScreen` |
| Modify | `AppNavigation.kt` — route rename          |
| Modify | `AppContainer.kt` — add StreakCalculator   |

No database migration is required.

## Testing Plan

### StreakCalculator Tests

- no choices → returns null.
- only No choices → returns timestamp of the first No.
- only Yes choices → returns null.
- Yes then No → returns timestamp of the No.
- Yes, No, No, No → returns timestamp of the first No after
  the Yes.
- No, No, Yes, No, No → returns timestamp of the first No after
  the last Yes.
- most recent choice is Yes → returns null even if there were
  earlier No runs.
- Record First No (early backdated No), then recent Nos →
  returns the backdated No timestamp if no Yes intervenes.

### StreakFormatter Tests

- 23 hours 59 minutes → returns null (under 1 day).
- exactly 24 hours → returns "1 day streak".
- 59 days → returns "59 day streak".
- 60 days → returns "2 month streak".
- 89 days → returns "89 day streak" (not "3 month streak" —
  depends on calendar months, not fixed 30-day periods).
- 11 months 29 days → returns "11 month streak".
- exactly 12 months → returns "1 year streak".
- 4 years 3 months → returns "4 year streak".

### TallyDetailsViewModel Tests

- loadTally populates all fields including streak and stats.
- recordFirstNo inserts a choice and refreshes stats.
- recordFirstNo on a new (unsaved) tally is a no-op.
- streak display updates after recordFirstNo.
- today stats hidden when fewer than 3 choices today.
- today stats shown when 3+ choices today.

### ChoicesViewModel Tests

- streakStart is populated on TallyDisplayItem when streak exists.
- streakStart is null when most recent choice is Yes.
- streakStart is null when tally has no choices.

### UI Tests

- tally row shows streak text when streak exists.
- tally row shows no indicator when no streak.
- tapping tally name navigates to Details screen.
- edit icon no longer appears in tally row.
- Details screen shows streak section with duration and date.
- Details screen hides streak/choices sections for new tally.
- Record First No opens date picker and inserts choice.
- date picker constrains to dates before earliest choice.
