# Test Coverage Improvement Plan

## Context

The app is in daily use with 54 passing tests but only 14% overall coverage.
Core business logic (ordering, progress, converters) is well tested, but the
HabitEditorViewModel, ConfigLoader, and most UI flows have zero coverage.
The user wants to improve test coverage before adding more features.

## Approach

Work through four phases of tests, prioritized by value and ease of writing.
All JVM tests follow the existing pattern: Truth assertions, MockK mocks,
UnconfinedTestDispatcher for Main, runTest blocks.

## Phase 1: HabitEditorViewModel (JVM unit tests)

New file: `app/src/test/kotlin/com/habit/viewmodel/HabitEditorViewModelTest.kt`

Tests to write:
- `loadHabit populates state from repository`
- `setName updates name and marks dirty`
- `addTimeOfDay adds and sorts, rejects duplicates`
- `removeTimeOfDay removes but keeps at least one`
- `toggleDayActive toggles, keeps at least one`
- `setDailyTarget rejects values below 1`
- `setTimed clears chime and threshold fields when false`
- `setThresholdMinutes initializes thresholdType to GOAL`
- `save inserts new habit with generated id`
- `save updates existing habit`
- `save does nothing when invalid`
- `delete marks as deleted, only for existing habits`
- `generateId handles conflicts with suffix`
- `isValid requires name, timesOfDay, daysActive, dailyTarget >= 1`
- `dirty flag set on any field change, cleared on save`

Mock: `HabitRepository` (same pattern as AgendaViewModelTest)

## Phase 2: ConfigLoader (JVM unit test)

New file: `app/src/test/kotlin/com/habit/data/ConfigLoaderTest.kt`

ConfigLoader reads from `Context.assets`, so mock the Context and
AssetManager to provide test JSON strings.

Tests to write:
- `loads valid config with all fields`
- `handles optional fields (null chime, threshold)`
- `converts day-of-week strings to enums`
- `converts priority and target mode strings`
- `converts daily texts map keys to DayOfWeek`
- `dayBoundaryHour is read correctly`

## Phase 3: AgendaViewModel additional coverage (JVM unit tests)

Update: `app/src/test/kotlin/com/habit/viewmodel/AgendaViewModelTest.kt`

New tests for recently added functionality:
- `expandActivity loads history including in-progress activity`
- `expandActivity from completed positions at selected activity`
- `collapseActivity returns to previous layout`
- `historyOlder and historyNewer update index and selectedActivityId`
- `historyBackToAnchor returns to anchor index`
- `hasSwipedFromAnchor is true after navigation`
- `updateNote saves to correct target (active vs history vs completed)`
- `cancelTimer deletes activity and creates fresh one`
- `selectCompletedActivity loads history`
- `completeActivity with null activeActivity falls back to database`
- `switchToReview preserves selectedHabitId and timer state`
- `selectHabit works after switchToReview and switchToMain round-trip`
- `selectHabit blocked only when timer running on a different habit`

## Phase 4: UI functional tests (instrumented)

Update: `app/src/androidTest/kotlin/com/habit/ui/PrimaryScreenTest.kt`

Rewrite to match current UI. Key flows to test:
- agenda displays habits, tap selects one
- checkbox completes untimed habit
- timer start/finish/cancel flow
- progress bar switches to review layout
- agenda bar switches back to main layout
- completed list shows finished activities
- expand/collapse toggle works
- menu opens with Habits and New Habit items

These require the emulator and `./gradlew connectedCheck`.

## Files to create/modify

| File | Action |
|------|--------|
| `app/src/test/.../viewmodel/HabitEditorViewModelTest.kt` | create |
| `app/src/test/.../data/ConfigLoaderTest.kt` | create |
| `app/src/test/.../viewmodel/AgendaViewModelTest.kt` | add tests |
| `app/src/androidTest/.../ui/PrimaryScreenTest.kt` | rewrite |

## Verification

After each phase:
1. `./gradlew testDebugUnitTest` — all JVM tests pass
2. `./gradlew createDebugUnitTestCoverageReport` — check coverage increase
3. After phase 4: `./gradlew connectedCheck` on emulator

Target: get overall coverage above 30%, viewmodel coverage above 60%.
