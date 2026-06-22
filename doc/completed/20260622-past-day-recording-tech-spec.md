# Past-day Recording Tech Spec

## Overview

Technical specification for past-day recording, implementing the behavior in
`past-day-recording-func-spec.md` and the UI in `past-day-recording-ux-design.md`. The feature lets
the user step the Review (Done) view back up to seven days and back-fill activities they forgot to
log. It uses the existing MVVM + Room + manual DI architecture.

The central change is small: `AgendaViewModel` is currently hard-wired to a single `today` date that
drives every query and computation. We generalize that date into a **selected date** that the user
can move within a fixed window, while keeping a separate notion of the **real today** for window
bounds, labels, and the day-boundary snap. Everything that already keys on a date — the
`activitiesForDate(date)` flow, the agenda/completed/target computations — follows for free.

Two consequences worth stating up front:

- **No schema migration.** `Activity.attributedDate` is already a stored, indexed column, and
  back-fill simply inserts an `Activity` with that column set to a past date.
- **No new DAO methods.** `ActivityDao.activitiesForDate(date)` already returns the right `Flow` for
  any date. Missed-row counts and the remaining count are derived in memory from that one list.

The only genuinely new surfaces are the date strip and the missed-row / Other... section of the
past-day list.

## Architectural approach: one date becomes two

Today the viewmodel holds:

```kotlin
private val today = MutableStateFlow(dayBoundary.today())
```

and this single flow drives the activity-loading `combine`, while the state field `today` and the
list field `todayActivities` carry that one date's data.

We split this into two private flows:

```kotlin
private val selectedDate = MutableStateFlow(dayBoundary.today())  // what the Review view displays
private val realToday = MutableStateFlow(dayBoundary.today())     // the actual attributed today
```

- `selectedDate` drives which date's activities are loaded. The user moves it with the chevrons.
- `realToday` is advanced only by the day-boundary refresh. It defines the navigable window
  (`realToday-7 .. realToday`), the `Today` / `Yesterday` labels, whether Easy Day applies, and the
  snap-back behavior.

`selectedDate` differs from `realToday` only inside the Review layout. Switching to Main (Day Plan)
always resets `selectedDate` to `realToday`, so the agenda is always today, per the func spec's
out-of-scope note.

### State field renames

To keep the code honest now that the displayed date is not necessarily today, two fields are
renamed (mechanical, viewmodel-internal — the UI reads the computed lists, not these fields
directly):

| old name          | new name                 |
|-------------------|--------------------------|
| `today`           | `selectedDate`           |
| `todayActivities` | `selectedDateActivities` |

A new `today: LocalDate` field is added to carry the real today (used by the date strip and the
`isViewingPastDay` check). The net field set is `selectedDate`, `today`, and
`selectedDateActivities`.

## Domain / data layer

### No entity, DAO, or migration changes

`Activity`, `ActivityDao`, `ActivityRepository`, and the database version are all unchanged. This is
the main reason the feature is cheap.

### DayBoundary: last instant of an attributed date

Back-fill defaults `completedAt` to "the last instant of the selected attributed date" (func spec,
Back-fill action). Add the inverse of the existing `attributedDate(instant)` mapping to
`DayBoundary`:

```kotlin
fun lastInstantOf(date: LocalDate): Instant {
    val nextBoundary = date.plusDays(1)
        .atTime(boundaryHour, 0)
        .atZone(ZoneId.systemDefault())
        .toInstant()
    return nextBoundary.minusMillis(1)
}
```

The attributed day `date` covers `[date @ boundaryHour, date+1 @ boundaryHour)`; this returns the
instant one millisecond before that half-open interval closes. By construction
`attributedDate(lastInstantOf(d)) == d` for every `d`, so the inserted activity lands on the
intended day.

## ViewModel layer

### New and changed state

`AgendaUiState` gains the selected date, the real today, the renamed activity list, and three
derived helpers. New computed members:

```kotlin
val isViewingPastDay: Boolean
    get() = selectedDate != today

// Easy Day is forward-looking only; past days always compute from raw dailyTarget (func spec).
val effectiveEasyDay: EasyDayLevel
    get() = if (isViewingPastDay) EasyDayLevel.OFF else easyDayLevel
```

`agendaItems`, `totalTarget`, `progressCount`, and `completedItems` are updated to read
`selectedDate` / `selectedDateActivities` / `effectiveEasyDay` instead of `today` /
`todayActivities` / `easyDayLevel`. Their logic is otherwise unchanged. Because `selectedDate ==
today` whenever the Main layout is showing, today's behavior is identical to current.

### Missed rows

A new item type, in its own file `viewmodel/MissedItem.kt`:

```kotlin
data class MissedItem(
    val habit: Habit,
    val count: Int,
    val target: Int
)
```

Computed in `AgendaUiState`, empty unless viewing a past day:

```kotlin
val missedItems: List<MissedItem>
    get() {
        if (!isViewingPastDay) return emptyList()
        val completedCounts = selectedDateActivities
            .filter { it.completedAt != null && !it.skipped }
            .groupBy { it.habitId }
            .mapValues { it.value.size }
        return habits
            .filter { selectedDate.dayOfWeek in it.daysActive }
            .mapNotNull { habit ->
                val count = completedCounts[habit.id] ?: 0
                if (count < habit.dailyTarget) MissedItem(habit, count, habit.dailyTarget) else null
            }
            .sortedWith(
                compareBy<MissedItem> { it.habit.timesOfDay.firstOrNull() ?: 0 }
                    .thenBy { it.habit.priority.ordinal }
                    .thenByDescending { it.habit.tieBreaker }
            )
    }
```

Uses raw `habit.dailyTarget` and current `daysActive` (func spec: missed-row eligibility and counts
use current config, ignore Easy Day). Ordering mirrors the agenda's time-of-day → priority →
tie-breaker scheme so the list reads consistently; missed rows have no "past time" concept so that
leading sort key is dropped.

### Other... on a past day

The past-day Other... is today's `otherHabits` rule with one substitution: "is a missed row" takes
the place of "is on the agenda." Both still suppress EXACTLY-mode habits already at target, so
back-fill can never create a second instance of an exactly-once habit (func spec, Other... entry):

```kotlin
val pastDayOtherHabits: List<Habit>
    get() {
        val missedIds = missedItems.map { it.habit.id }.toSet()
        val completedCounts = selectedDateActivities
            .filter { it.completedAt != null && !it.skipped }
            .groupBy { it.habitId }
            .mapValues { it.value.size }
        return habits.filter { habit ->
            habit.id !in missedIds &&
                !(habit.dailyTargetMode == TargetMode.EXACTLY &&
                    (completedCounts[habit.id] ?: 0) >= habit.dailyTarget)
        }
    }
```

This keeps at-least habits at/above target and not-active-that-weekday habits in the list, while an
EXACTLY-1 habit not yet done that day surfaces as a missed row (count below target) rather than
here. The completion count excludes skips (`!it.skipped`), matching `missedItems` — a skip is not a
completion. (This is intentionally stricter than today's `otherHabits`, which counts skips; the
divergence only affects which skipped-to-target EXACTLY habits appear, an edge with no user-visible
consequence since such habits surface as missed rows anyway.)

### Actions

#### Date navigation

```kotlin
fun stepDate(deltaDays: Int) {
    val today = realToday.value
    val candidate = selectedDate.value.plusDays(deltaDays.toLong())
    if (candidate in today.minusDays(WINDOW_DAYS)..today) {
        clearViewSelection()
        selectedDate.value = candidate
    }
}

fun goToToday() {
    val running = _uiState.value.timerRunning
    val timedId = _uiState.value.timedHabitId
    clearViewSelection()
    selectedDate.value = realToday.value
    if (running && timedId != null) resumeRunningTimerView(timedId)
}
```

`WINDOW_DAYS = 7` (companion constant). The chevrons call `stepDate(-1)` / `stepDate(+1)`; the
date-label tap and the menu's `Done Today` both call `goToToday()`. The candidate-range check makes
the disabled-chevron states fall out naturally (`canStepBack = selectedDate > today-7`,
`canStepForward = selectedDate < today` exposed for the UI).

#### Back-fill

Tapping a missed row or selecting a habit through past-day Other... runs the same path:

```kotlin
fun backFill(habitId: String) {
    val date = selectedDate.value
    viewModelScope.launch {
        val activity = Activity(
            habitId = habitId,
            attributedDate = date,
            startTime = null,
            note = "",
            completedAt = dayBoundary.lastInstantOf(date),
            trackId = null,
            milestoneId = null,
            skipped = false
        )
        val id = activityRepo.create(activity)
        selectBackFilledActivity(activity.copy(id = id))
    }
}
```

`selectBackFilledActivity` is a private helper that mirrors `selectCompletedActivity`, with one
addition: it **optimistically inserts** the new activity into `selectedDateActivities` before
selecting it. `CompletedActivityDetail` (and `currentEditableActivity`) resolve the selected
activity by id from `selectedDateActivities` (`ActivityView.kt:565`), and the date-keyed Room flow
has not re-emitted the freshly inserted row yet — without the optimistic insert the detail would
render empty for a beat. The later flow emit rebuilds the list from the database and is idempotent
on the same id:

```kotlin
private suspend fun selectBackFilledActivity(activity: Activity) {
    val s = _uiState.value
    val activities =
        if (s.selectedDateActivities.any { it.id == activity.id }) s.selectedDateActivities
        else s.selectedDateActivities + activity
    _uiState.value = s.copy(
        selectedDateActivities = activities,
        selectedActivityId = activity.id,
        selectedHabitId = activity.habitId
    )
    loadHistory(activity.habitId, activity.id)
    loadAndSetTracks(activity.habitId)
    hydrateTrackStateForActivity(null)
}
```

Setting `selectedActivityId` with the layout left at `REVIEW` makes `ActivityView` render
`CompletedActivityDetail` — exactly the surface reached by tapping a completed row, satisfying the
func spec's auto-open-for-editing. `loadHistory` populates the swipe history from
`completedHistoryForHabit`, which already includes the just-inserted activity. Dismissing the detail
(`collapseActivity` / clearing selection) returns to the past-day list with the back-fill persisted;
the date-keyed flow has re-emitted by then, so the new row appears in `completedItems` and the
missed row's count updates or disappears.

The `Do again` button on a past-day completed activity routes to `backFill(habitId)` for the
selected date rather than the today-oriented `doAgain`, so an extra activity is added to the same
past day with the same auto-open flow. `PrimaryScreen` picks the callback by date —
`onDoAgain = if (isViewingPastDay) viewModel::backFill else viewModel::doAgain` — and passes it to
both `CompletedList` (the row "Again" button) and `CompletedActivityDetail` (the detail "Again"
button), which only appear for at-least habits in either place.

#### Editing track and milestone on a completed activity

Back-fill auto-opens the new activity in `CompletedActivityDetail`, where the user may set its track
and milestone. Today `selectTrack` / `selectMilestone` operate only on `activeActivity`, which is
null for a completed (selected) activity, so both are widened to resolve their target through a
shared helper:

```kotlin
private fun currentEditableActivity(): Activity? {
    val s = _uiState.value
    return s.activeActivity
        ?: s.selectedActivityId?.let { id -> s.selectedDateActivities.find { it.id == id } }
}
```

`selectTrack` / `selectMilestone` call `currentEditableActivity()` instead of reading
`activeActivity` directly, persist the updated activity, and refresh `selectedTrack` /
`selectedMilestone` / `incompleteMilestones` for the detail. When the edited activity is the
`activeActivity` (the live, in-progress case) behavior is unchanged; when it is a selected completed
activity, the change is written through the repository and the date-keyed flow re-emits the updated
row.

*Side effect:* this also enables changing the track/milestone of any already-completed activity,
including on today — a capability the app did not previously have. That is intentional and the same
edit surface back-fill needs. Marking a milestone as *completed in its series* stays a
completion-time action (`toggleMilestoneChecked` + `persistMilestoneIfChecked`) and is out of scope
for back-fill, which only associates an existing track/milestone with the activity.

#### Clearing the view selection on date change

```kotlin
private fun clearViewSelection() {
    val s = _uiState.value
    _uiState.value = s.copy(
        selectedHabitId = null,
        selectedActivityId = null,
        layout = if (s.layout == Layout.ACTIVITY_FOCUSED) Layout.REVIEW else s.layout,
        historyActivities = emptyList(),
        historyIndex = -1,
        historyAnchorIndex = -1,
        trackHistoryVisible = false,
        trackHistory = emptyList()
    )
}
```

This collapses any expanded detail back to the summary on the new date (UX: "Selection on date
change"). It deliberately does **not** touch `activeActivity`, `timerRunning`, `timedHabitId`, or
the interval-chime fields — see the running-timer note below.

### Running timer while viewing a past day

A timer is a live, today-only session backed by the foreground service. Navigating the Review view
to a past day must not cancel it. The approach:

- `clearViewSelection()` clears only the *displayed* selection, leaving the live-session fields
  intact and the foreground service running with its notification.
- The tick loop in `startTimerTick` finds its activity via
  `selectedHabitId == timedHabitId ? activeActivity : selectedDateActivities.find { ... }`. On a
  past day neither branch matches (selection cleared; the running activity belongs to today, not
  `selectedDateActivities`), so the loop hits its `?: break` and the on-screen ticking pauses — but
  the service, the elapsed-from-`startTime` computation, and the chimes are unaffected.
- `goToToday()` calls `resumeRunningTimerView(timedHabitId)`, which re-selects the running habit
  (`selectedHabitId = timedHabitId`) and calls `startTimerTick()` again. Because `elapsedMs` derives
  from the stored `startTime`, the resumed display is accurate.

*Motivation:* this keeps a running session non-destructive across a quick past-day peek without
threading today's activity through the past-day data flow. It does add the `resumeRunningTimerView`
restart on return-to-today. See Open decisions.

### Day-boundary crossing

`refreshToday` advances `realToday` and snaps the selection if it has fallen out of the window:

```kotlin
fun refreshToday() {
    val current = dayBoundary.today()
    if (current != realToday.value) {
        realToday.value = current
        if (selectedDate.value !in current.minusDays(WINDOW_DAYS)..current) {
            clearViewSelection()
            selectedDate.value = current
        }
    }
}
```

If the crossed-over selection is still in the window, only its label changes (handled in the UI from
`selectedDate` vs `today`); if it has aged out, the view snaps to the new today and collapses, per
the UX "Day boundary crossing" section.

### Flow restructuring in `init`

The activity-loading flow keys on `selectedDate`; the Easy Day flow keys on `realToday` (Easy Day is
a today-only concept and its controls always act on today):

```kotlin
combine(selectedDate, realToday) { sel, today -> sel to today }
    .flatMapLatest { (sel, today) ->
        val easyDayFlow = easyDayRepo?.flowForDate(today) ?: flowOf(EasyDayLevel.OFF)
        val carryOverFlow = easyDayRepo?.carryOverFlow() ?: flowOf(false)
        combine(
            habitRepo.allHabits(),
            activityRepo.activitiesForDate(sel),
            easyDayFlow,
            carryOverFlow
        ) { habits, activities, easyDayLevel, carryOver ->
            _uiState.value.copy(
                habits = habits,
                selectedDateActivities = activities,
                selectedDate = sel,
                today = today,
                easyDayLevel = easyDayLevel,
                easyDayCarryOver = carryOver
            )
        }
    }
    .collect { _uiState.value = it }
```

## UI layer

### Date strip

New composable `ui/DateStrip.kt`, rendered at the top of the Review layout content, above
`ActivityView` (UX: "a persistent strip at the top of the activity view on the Review layout"). It
is shown only in the `REVIEW` branch of `PrimaryScreen`; Main and Activity-focused do not get it.

```kotlin
@Composable
fun DateStrip(
    label: String,            // "Today" | "Yesterday" | "Tue Jun 2"
    canStepBack: Boolean,
    canStepForward: Boolean,
    onStepBack: () -> Unit,
    onStepForward: () -> Unit,
    onLabelTap: () -> Unit,   // jump to today; no-op visually when already today
    modifier: Modifier = Modifier
)
```

`ChevronLeft` / `ChevronRight` `IconButton`s with `enabled = canStepBack / canStepForward` (disabled
renders muted with no ripple, Material default). The centered label is a clickable `Text`. Label
formatting lives in a small pure helper for unit testing:

```kotlin
fun dateStripLabel(selected: LocalDate, today: LocalDate): String = when (selected) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> selected.format(DateTimeFormatter.ofPattern("EEE MMM d"))
}
```

`PrimaryScreen` derives the strip inputs from state: `label = dateStripLabel(selectedDate, today)`,
`canStepForward = isViewingPastDay`, `canStepBack = selectedDate > today.minusDays(7)`.

### Past-day completed list

`CompletedList` is extended to render the three-section past-day layout (completed activities →
divider → missed rows → Other...). The today path renders exactly as now. New parameters:

```kotlin
@Composable
fun CompletedList(
    items: List<CompletedItem>,
    missed: List<MissedItem>,          // empty on today
    showOther: Boolean,                // true on past days
    onSelect: (Long) -> Unit,
    onDoAgain: (String) -> Unit,
    onBackfillMissed: (String) -> Unit,
    onOther: () -> Unit,
    modifier: Modifier = Modifier
)
```

Within the `LazyColumn`: existing completed rows; then, when `missed` is non-empty, a thin
`HorizontalDivider` followed by a `MissedRow` per item; then, when `showOther`, the `Other…` row.
With no missed rows the divider is omitted (UX). The `Other…` row reuses the agenda's `Other…` style
(`bodyLarge`, muted, end-of-list) and calls `onOther`, which opens the existing Other dialog (see
below).

`MissedRow` in its own file `ui/MissedRow.kt`:

```kotlin
@Composable
fun MissedRow(item: MissedItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckBoxOutlineBlank,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = item.habit.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).padding(start = 12.dp)
        )
        if (item.target > 1) {
            Text(
                text = "${item.count}/${item.target}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

The empty checkbox and habit name use `onSurfaceVariant`; the `count/target` is shown only when
`target > 1` (UX: `0/1` adds nothing). Tapping anywhere on the row calls
`onBackfillMissed(habitId)`.

### Other... dialog reuse

`PrimaryScreen` already owns `showOtherDialog`. The dialog's habit list is sourced from
`uiState.otherHabits` on Main and `uiState.pastDayOtherHabits` on a past-day Review, and its
on-click dispatches `viewModel.selectHabit` (today) or `viewModel.backFill` (past day). The dialog
composable is unchanged apart from the parameterized list and click handler.

### Track and milestone selectors in the completed-activity detail

`CompletedActivityDetail` gains the existing `TrackSelector` (and, for series tracks, the
`MilestoneSelector`) between the habit-name row and the note field, shown when
`state.availableTracks.isNotEmpty()` — the same condition `HabitView` uses. They wire to the
`onSelectTrack` / `onSelectMilestone` callbacks, which `CompletedActivityDetail` does not currently
receive, so its parameter list and the `ActivityView` call site (`ActivityView.kt:91`) gain them.
This makes the back-fill's "pick a track/milestone" promise real and applies to any completed
activity opened in the detail.

### Activity detail subtitle on a past day

When `isViewingPastDay`, a small date subtitle appears under the habit name showing
`dateStripLabel(selectedDate, today)` in `bodySmall` / `onSurfaceVariant` (UX: "Activity detail on a
past day"). It is added in two header spots so the day context is present in every detail surface a
past-day activity can occupy:

- the compact `CompletedActivityDetail` (the back-fill / tap-a-row surface), and
- the shared expanded header used by `HabitView` (`ActivityView.kt:290`), which the full-screen
  `ACTIVITY_FOCUSED` layout renders when an activity is unfolded — that layout has no date strip, so
  without the subtitle there would be no date cue at all.

No other detail behavior changes.

### Collapsed summary on a past day

`CollapsedSummary` (the activity view shown when nothing is selected) already reflects the selected
date once the field renames land — its "N/M activities complete" and "K incomplete" lines read from
`progressCount` / `totalTarget`, which now compute against `selectedDate`. One case needs explicit
handling: a barren past day with nothing recorded and no missed rows (no habits active that weekday)
would otherwise read "0/0 activities complete". Per the UX it instead reads `Nothing recorded on
<date>`:

```kotlin
@Composable
private fun CollapsedSummary(state: AgendaUiState) {
    Column(modifier = Modifier.padding(16.dp)) {
        if (state.isViewingPastDay &&
            state.completedItems.isEmpty() && state.missedItems.isEmpty()
        ) {
            Text(
                text = "Nothing recorded on " +
                    state.selectedDate.format(DateTimeFormatter.ofPattern("EEE MMM d")),
                style = MaterialTheme.typography.bodyLarge
            )
            return@Column
        }
        // unchanged: "N/M activities complete" + optional "K incomplete"
    }
}
```

The empty text uses the absolute `EEE MMM d` form (e.g. `Nothing recorded on Tue Jun 2`) rather than
the strip's relative `Today` / `Yesterday` label, which reads awkwardly mid-sentence. The guard is
`isViewingPastDay`, so a barren *today* is unchanged. The UX's "time tracked" in the summary is not
added — the current summary shows only completion counts; an elapsed-time total would be a separate
enhancement touching today's summary too.

### Agenda bar on a past day

The bar is already driven by nullable callbacks, so the past-day differences are wiring-only in the
`REVIEW` branch of `PrimaryScreen`:

- `onEasyDay = null` and `easyDaySubLabel = null` when `isViewingPastDay` — the menu hides the Easy
  Day entry and its sub-label (UX: suppressed, not disabled).
- `onDoneToday = viewModel::goToToday` — already present; on a past day it is the backup return.
- `remaining = totalTarget - progressCount` already reflects `selectedDate` through the computed
  props; the label text is the "incomplete" wording from the prior change.

`New habit`, `Habit list`, `Choices`, and `Day Plan` are unchanged. `Day Plan` (`switchToMain`)
resets `selectedDate` to today.

### switchToMain resets the date

`switchToMain` gains one line — `selectedDate.value = realToday.value` — so leaving Review always
returns the agenda to today. (It already resumes a running timer via `resumeHabitId`.)

## Dependency wiring

No changes. `AgendaViewModel`'s constructor, `AgendaViewModelFactory`, and `AppContainer` are
untouched — the feature adds no new repositories or dependencies.

## Testing plan

### DayBoundary (unit, JVM)

- `lastInstantOf(d)` round-trips: `attributedDate(lastInstantOf(d)) == d` for a range of dates and
  several `boundaryHour` values (0, 2, 23).
- `lastInstantOf(d)` is strictly before the next day's boundary and after `d`'s boundary.

### Date-label formatting (unit, JVM)

- selected == today → "Today".
- selected == today − 1 → "Yesterday".
- selected == today − 2 → "Wed …"-style weekday/month/day.
- oldest reachable day formats as a normal date (no year).

### AgendaUiState computed props (unit, JVM)

- `isViewingPastDay` false when `selectedDate == today`, true otherwise.
- `effectiveEasyDay` is `OFF` on a past day even when `easyDayLevel` is set; equals `easyDayLevel`
  on today.
- `missedItems` empty on today.
- `missedItems` includes a habit active that weekday with completions below raw `dailyTarget`, with
  the correct `count`/`target`; excludes habits at/above target and habits not active that weekday.
- `missedItems` ignores Easy Day (uses raw `dailyTarget`).
- `missedItems` ordering: time-of-day, then priority, then tie-breaker.
- `pastDayOtherHabits` excludes habits present as missed rows and EXACTLY-mode habits already at
  target; includes at-least habits at/above target and inactive-that-weekday habits.
- `totalTarget` / `progressCount` computed against the selected date and raw targets on a past day.

### AgendaViewModel (unit, JVM — JUnit + MockK + Turbine)

- `stepDate(-1)` moves the selected date back one day and clears the view selection.
- `stepDate` is clamped: no move past `today` (forward) or `today − 7` (back).
- `goToToday` resets the selected date and collapses to the summary.
- `goToToday` with a running timer re-selects the timed habit.
- `backFill` inserts an `Activity` with `attributedDate == selectedDate`, end-of-day `completedAt`,
  null `startTime`, and selects it as a completed activity (no timer started).
- `backFill` makes the new activity immediately resolvable — present in `selectedDateActivities` and
  set as `selectedActivityId` — without waiting on the date-keyed flow to re-emit.
- back-fill from Other... behaves identically to a missed-row tap.
- `Do again` on a past-day completed activity inserts another activity on the same date.
- `selectTrack` on a selected completed activity (no `activeActivity`) associates and persists the
  track; `selectMilestone` likewise persists the milestone.
- `selectTrack` on a live `activeActivity` is unchanged from current behavior.
- editing a back-fill's `completedAt` to another day moves its `attributedDate` (existing
  `updateActivityCompletedAt` behavior; assert it still holds on a past-day selection).
- date change does not cancel a running timer (timer/service state preserved).
- `refreshToday` advances the real today; an in-window selection is kept, an out-of-window selection
  snaps to today and collapses.
- `switchToMain` resets the selected date to today.

### UI (functional, emulator — Compose Testing)

- date strip: forward chevron disabled on today, back chevron disabled at the oldest day, label tap
  jumps to today.
- past-day list renders completed rows, divider, missed rows, and Other…; today's list renders none
  of the past-day extras.
- missed row hides `count/target` when target is 1 and shows it otherwise.
- tapping a missed row opens the completed-activity detail with the new activity (auto-open).
- the completed-activity detail shows track/milestone selectors when the habit has tracks, and a
  back-fill can set a track/milestone through them.
- the activity detail shows the date subtitle on a past day and not on today, in both the compact
  detail and the full-screen expanded view (which has no date strip).
- a barren past day (no completions, no missed rows) shows "Nothing recorded on <date>"; a barren
  today is unchanged.
- the menu hides Easy Day on a past day and shows Done Today.

### Navigation / flow (functional, emulator)

- Main → Review → step back → back-fill → dismiss detail → row appears in the completed group and
  the missed row updates.
- Review past day → Day Plan returns to Main on today.

## File changes summary

| Action | File                                                                    |
|--------|-------------------------------------------------------------------------|
| Modify | `data/DayBoundary.kt` — add `lastInstantOf`                             |
| Modify | `viewmodel/AgendaViewModel.kt` — date flows, nav, back-fill, track edit |
| Modify | `viewmodel/AgendaUiState.kt` — rename fields, add date helpers          |
| Create | `viewmodel/MissedItem.kt`                                               |
| Modify | `ui/PrimaryScreen.kt` — date strip wiring, past-day list + dialog       |
| Create | `ui/DateStrip.kt`                                                       |
| Modify | `ui/CompletedList.kt` — missed rows, divider, Other… row                |
| Create | `ui/MissedRow.kt`                                                       |
| Modify | `ui/ActivityView.kt` — date subtitle, track/milestone selectors         |

No migration. No DAO, repository, or dependency-wiring changes.

## Resolved decisions

Judgment calls where the specs left room, now settled.

1. **Running timer across a past-day peek.** Keep the session running in the service, pause only the
   on-screen tick while away, and re-show + restart the tick on return to today. The timer is never
   cancelled.

2. **Missed-row ordering.** Reuse the agenda's time-of-day → priority → tie-breaker order, so the
   missed rows read against the chronological completed group above them as the shape of the day.

3. **Past-day Other... for exactly-mode habits.** Suppress EXACTLY-mode habits already at target,
   same as today's Other..., so back-fill can never create a second instance of an exactly-once
   habit. The func spec was updated to match.

4. **Track/milestone editing on a back-fill.** Build it (rather than descope): add the selectors to
   `CompletedActivityDetail` and widen `selectTrack` / `selectMilestone` to act on a selected
   completed activity. This also gives the app retroactive track editing for any completed activity,
   which it previously lacked — intentional, since it is the same edit surface back-fill needs.
