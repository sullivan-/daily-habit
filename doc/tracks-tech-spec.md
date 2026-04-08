# Tracks Tech Spec

## Overview

Technical specification for the tracks feature, implementing the behavior defined in
`tracks-func-spec.md` and the layouts defined in `tracks-ux-design.md`. Adds track sub-categories
to habits, with optional day-of-week defaults and ordered milestone series. Also changes the
JSON config loading strategy from "insert with IGNORE on every start" to "seed only when the
database is empty."

## Domain Entities

### Track

A sub-category within a habit. Tracks are user-managed (created, edited, archived via the UI)
and stored in Room.

File: `app/src/main/kotlin/com/habit/data/Track.kt`

```kotlin
@Entity(
    tableName = "track",
    foreignKeys = [ForeignKey(
        entity = Habit::class,
        parentColumns = ["id"],
        childColumns = ["habitId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("habitId")]
)
data class Track(
    @PrimaryKey val id: String,
    val habitId: String,
    val name: String,
    val priority: Priority,
    val dayOfWeek: DayOfWeek? = null,
    val archived: Boolean = false
)
```

- `id` is a stable string, generated from the name (slug) on creation, matching the habit/tally
  pattern.
- `dayOfWeek` is the optional day-of-week default. null means no default day.
- cascade delete ensures tracks are removed when their parent habit is deleted.

### Milestone

An ordered item within a track's series. the series order is a suggestion — the user can pick any
incomplete milestone.

File: `app/src/main/kotlin/com/habit/data/Milestone.kt`

```kotlin
@Entity(
    tableName = "milestone",
    foreignKeys = [ForeignKey(
        entity = Track::class,
        parentColumns = ["id"],
        childColumns = ["trackId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("trackId")]
)
data class Milestone(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: String,
    val name: String,
    val sortOrder: Int,
    val completed: Boolean = false
)
```

- `sortOrder` defines the suggested order. the first incomplete milestone (by `sortOrder`) is the
  default selection, but the user can pick any incomplete milestone.
- cascade delete removes milestones when their parent track is deleted.

### Activity Changes

Add an optional `trackId` and `milestoneId` to the existing Activity entity.

File: `app/src/main/kotlin/com/habit/data/Activity.kt`

```kotlin
@Entity(
    tableName = "activity",
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Track::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Milestone::class,
            parentColumns = ["id"],
            childColumns = ["milestoneId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("habitId"), Index("attributedDate"), Index("trackId"), Index("milestoneId")]
)
data class Activity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: String,
    val attributedDate: LocalDate,
    val startTime: Instant?,
    val note: String,
    val completedAt: Instant?,
    val trackId: String? = null,
    val milestoneId: Long? = null
) {
    val elapsedMs: Long
        get() = when {
            startTime == null -> 0
            completedAt != null -> completedAt.toEpochMilli() - startTime.toEpochMilli()
            else -> System.currentTimeMillis() - startTime.toEpochMilli()
        }
}
```

- `trackId` references the selected track. null means no track was selected.
- `milestoneId` references the milestone that was active during this activity. null for tracks
  without series or when no track was selected.
- foreign keys on both fields. tracks and milestones are never deleted independently when they
  have associated activities — they are archived instead. deletion is only allowed when no
  activities reference them (user is still setting up). cascade delete from the parent habit
  cleans up everything.

### Habit Changes

Remove `dailyTexts` from the Habit entity. Day-of-week defaults move to Track.dayOfWeek.

```kotlin
@Entity(tableName = "habit")
data class Habit(
    @PrimaryKey val id: String,
    val name: String,
    val timesOfDay: List<Int>,
    val sortOrder: Int,
    val daysActive: Set<DayOfWeek>,
    val dailyTarget: Int,
    val dailyTargetMode: TargetMode,
    val timed: Boolean,
    val goalMinutes: Int?,
    val stopMinutes: Int?,
    val priority: Priority
)
```

## Database Layer

### Migration 8 → 9

File: `app/src/main/kotlin/com/habit/data/HabitDatabase.kt`

```kotlin
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS track (
                id TEXT NOT NULL PRIMARY KEY,
                habitId TEXT NOT NULL,
                name TEXT NOT NULL,
                priority TEXT NOT NULL,
                dayOfWeek TEXT,
                archived INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (habitId) REFERENCES habit(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_track_habitId ON track(habitId)")

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS milestone (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                trackId TEXT NOT NULL,
                name TEXT NOT NULL,
                sortOrder INTEGER NOT NULL,
                completed INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (trackId) REFERENCES track(id) ON DELETE CASCADE
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_milestone_trackId ON milestone(trackId)")

        db.execSQL("ALTER TABLE activity ADD COLUMN trackId TEXT")
        db.execSQL("ALTER TABLE activity ADD COLUMN milestoneId INTEGER")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_activity_trackId ON activity(trackId)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS index_activity_milestoneId ON activity(milestoneId)"
        )

        db.execSQL("ALTER TABLE habit DROP COLUMN dailyTexts")
    }
}
```

Register `MIGRATION_8_9` in `AppContainer` and bump database version to 9. Add `Track` and
`Milestone` to the `@Database` entities list.

### TrackDao

File: `app/src/main/kotlin/com/habit/data/TrackDao.kt`

```kotlin
@Dao
interface TrackDao {
    @Query("SELECT * FROM track WHERE habitId = :habitId ORDER BY archived, name")
    fun tracksForHabit(habitId: String): Flow<List<Track>>

    @Query(
        "SELECT * FROM track WHERE habitId = :habitId AND archived = 0 " +
        "ORDER BY name"
    )
    suspend fun activeTracksForHabit(habitId: String): List<Track>

    @Query("SELECT * FROM track WHERE id = :id")
    suspend fun getById(id: String): Track?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(tracks: List<Track>)

    @Insert
    suspend fun insert(track: Track)

    @Update
    suspend fun update(track: Track)

    @Query("DELETE FROM track WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM activity WHERE trackId = :trackId")
    suspend fun activityCount(trackId: String): Int
}
```

### MilestoneDao

File: `app/src/main/kotlin/com/habit/data/MilestoneDao.kt`

```kotlin
@Dao
interface MilestoneDao {
    @Query("SELECT * FROM milestone WHERE trackId = :trackId ORDER BY sortOrder")
    suspend fun milestonesForTrack(trackId: String): List<Milestone>

    @Query(
        "SELECT * FROM milestone WHERE trackId = :trackId AND completed = 0 " +
        "ORDER BY sortOrder LIMIT 1"
    )
    suspend fun defaultMilestone(trackId: String): Milestone?

    @Query(
        "SELECT * FROM milestone WHERE trackId = :trackId AND completed = 0 " +
        "ORDER BY sortOrder"
    )
    suspend fun incompleteMilestones(trackId: String): List<Milestone>

    @Query("SELECT * FROM milestone WHERE id = :id")
    suspend fun getById(id: Long): Milestone?

    @Insert
    suspend fun insert(milestone: Milestone): Long

    @Update
    suspend fun update(milestone: Milestone)

    @Query("DELETE FROM milestone WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM activity WHERE milestoneId = :milestoneId")
    suspend fun activityCount(milestoneId: Long): Int

    @Query("SELECT MAX(sortOrder) FROM milestone WHERE trackId = :trackId")
    suspend fun maxSortOrder(trackId: String): Int?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(milestones: List<Milestone>)
}
```

### ActivityDao Changes

No changes to existing queries. The new `trackId` and `milestoneId` columns are nullable and
default to null, so existing queries continue to work. The activity update method already uses
`@Update` which handles all columns.

## Repository Layer

### TrackRepository

File: `app/src/main/kotlin/com/habit/data/TrackRepository.kt`

```kotlin
class TrackRepository(
    private val trackDao: TrackDao,
    private val milestoneDao: MilestoneDao
) {
    fun tracksForHabit(habitId: String): Flow<List<Track>> =
        trackDao.tracksForHabit(habitId)

    suspend fun activeTracksForHabit(habitId: String): List<Track> =
        trackDao.activeTracksForHabit(habitId)

    suspend fun getById(id: String): Track? = trackDao.getById(id)

    suspend fun insert(track: Track) = trackDao.insert(track)

    suspend fun update(track: Track) = trackDao.update(track)

    suspend fun canDelete(id: String): Boolean = trackDao.activityCount(id) == 0

    suspend fun deleteById(id: String) = trackDao.deleteById(id)

    suspend fun milestonesForTrack(trackId: String): List<Milestone> =
        milestoneDao.milestonesForTrack(trackId)

    suspend fun defaultMilestone(trackId: String): Milestone? =
        milestoneDao.defaultMilestone(trackId)

    suspend fun incompleteMilestones(trackId: String): List<Milestone> =
        milestoneDao.incompleteMilestones(trackId)

    suspend fun getMilestoneById(id: Long): Milestone? = milestoneDao.getById(id)

    suspend fun insertMilestone(milestone: Milestone): Long =
        milestoneDao.insert(milestone)

    suspend fun updateMilestone(milestone: Milestone) =
        milestoneDao.update(milestone)

    suspend fun canDeleteMilestone(id: Long): Boolean = milestoneDao.activityCount(id) == 0

    suspend fun deleteMilestone(id: Long) =
        milestoneDao.deleteById(id)

    suspend fun maxMilestoneSortOrder(trackId: String): Int =
        milestoneDao.maxSortOrder(trackId) ?: 0

    suspend fun loadFromConfig(tracks: List<Track>, milestones: Map<String, List<Milestone>>) {
        trackDao.insertAll(tracks)
        milestones.forEach { (_, ms) -> milestoneDao.insertAll(ms) }
    }
}
```

## ViewModel Layer

### AgendaViewModel Changes

The AgendaViewModel needs to support track selection on the current activity.

New state fields in `AgendaUiState`:

```kotlin
data class AgendaUiState(
    // ... existing fields ...
    val availableTracks: List<Track> = emptyList(),
    val selectedTrack: Track? = null,
    val selectedMilestone: Milestone? = null,
    val incompleteMilestones: List<Milestone> = emptyList()
)
```

New methods:

```kotlin
fun loadTracksForHabit(habitId: String) {
    viewModelScope.launch {
        val tracks = trackRepo.activeTracksForHabit(habitId)
        val today = dayBoundary.today().dayOfWeek
        val sorted = tracks.sortedWith(
            compareByDescending<Track> { it.dayOfWeek == today }
                .thenByDescending { priorityToScore(it.priority) }
        )
        _uiState.value = _uiState.value.copy(availableTracks = sorted)
    }
}

fun selectTrack(trackId: String?) {
    viewModelScope.launch {
        val activity = _uiState.value.activeActivity ?: return@launch
        val track = trackId?.let { trackRepo.getById(it) }
        val milestone = track?.let { trackRepo.defaultMilestone(it.id) }
        val incomplete = track?.let { trackRepo.incompleteMilestones(it.id) } ?: emptyList()

        val updated = activity.copy(
            trackId = trackId,
            milestoneId = milestone?.id
        )
        activityRepo.update(updated)

        _uiState.value = _uiState.value.copy(
            activeActivity = updated,
            selectedTrack = track,
            selectedMilestone = milestone,
            incompleteMilestones = incomplete
        )
    }
}

fun selectMilestone(milestoneId: Long) {
    viewModelScope.launch {
        val activity = _uiState.value.activeActivity ?: return@launch
        val milestone = trackRepo.getMilestoneById(milestoneId) ?: return@launch
        val updated = activity.copy(milestoneId = milestoneId)
        activityRepo.update(updated)

        _uiState.value = _uiState.value.copy(
            activeActivity = updated,
            selectedMilestone = milestone
        )
    }
}

fun completeMilestone() {
    viewModelScope.launch {
        val milestone = _uiState.value.selectedMilestone ?: return@launch
        val completed = milestone.copy(completed = true)
        trackRepo.updateMilestone(completed)

        val track = _uiState.value.selectedTrack ?: return@launch
        val next = trackRepo.defaultMilestone(track.id)
        val incomplete = trackRepo.incompleteMilestones(track.id)

        val activity = _uiState.value.activeActivity ?: return@launch
        val updated = activity.copy(milestoneId = next?.id)
        activityRepo.update(updated)

        _uiState.value = _uiState.value.copy(
            activeActivity = updated,
            selectedMilestone = next,
            incompleteMilestones = incomplete
        )
    }
}
```

When `selectHabit` is called, also call `loadTracksForHabit` to populate the dropdown. When
the habit has no tracks, `availableTracks` remains empty and the dropdown is hidden.

### HabitEditorViewModel Changes

Add track management to the existing habit editor state and methods.

New state fields in `HabitEditorState`:

```kotlin
data class HabitEditorState(
    // ... existing fields (minus dailyTexts) ...
    val tracks: List<TrackEditorItem> = emptyList()
)

data class TrackEditorItem(
    val id: String,
    val name: String,
    val priority: Priority,
    val dayOfWeek: DayOfWeek?,
    val archived: Boolean,
    val milestones: List<Milestone>,
    val isNew: Boolean = false,
    val expanded: Boolean = false
)
```

New methods:

```kotlin
fun addTrack() {
    val items = _state.value.tracks
    val newItem = TrackEditorItem(
        id = "", name = "", priority = Priority.MEDIUM,
        dayOfWeek = null, archived = false, milestones = emptyList(),
        isNew = true, expanded = true
    )
    _state.value = _state.value.copy(tracks = items + newItem, dirty = true)
}

fun updateTrack(index: Int, item: TrackEditorItem) {
    val items = _state.value.tracks.toMutableList()
    items[index] = item
    _state.value = _state.value.copy(tracks = items, dirty = true)
}

fun toggleTrackExpanded(index: Int) {
    val items = _state.value.tracks.toMutableList()
    items[index] = items[index].copy(expanded = !items[index].expanded)
    _state.value = _state.value.copy(tracks = items)
}

fun archiveTrack(index: Int) {
    val items = _state.value.tracks.toMutableList()
    items[index] = items[index].copy(archived = true, expanded = false)
    _state.value = _state.value.copy(tracks = items, dirty = true)
}

fun unarchiveTrack(index: Int) {
    val items = _state.value.tracks.toMutableList()
    items[index] = items[index].copy(archived = false)
    _state.value = _state.value.copy(tracks = items, dirty = true)
}

fun deleteTrack(index: Int) {
    val items = _state.value.tracks.toMutableList()
    items.removeAt(index)
    _state.value = _state.value.copy(tracks = items, dirty = true)
}

fun deleteMilestone(trackIndex: Int, milestoneIndex: Int) {
    val items = _state.value.tracks.toMutableList()
    val track = items[trackIndex]
    val milestones = track.milestones.toMutableList()
    milestones.removeAt(milestoneIndex)
    items[trackIndex] = track.copy(milestones = milestones)
    _state.value = _state.value.copy(tracks = items, dirty = true)
}

fun addMilestone(trackIndex: Int, name: String) {
    val items = _state.value.tracks.toMutableList()
    val track = items[trackIndex]
    val nextOrder = (track.milestones.maxOfOrNull { it.sortOrder } ?: 0) + 1
    val milestone = Milestone(
        trackId = track.id, name = name,
        sortOrder = nextOrder, completed = false
    )
    items[trackIndex] = track.copy(milestones = track.milestones + milestone)
    _state.value = _state.value.copy(tracks = items, dirty = true)
}
```

The `save()` method must persist track and milestone changes alongside the habit. On save:
1. Save the habit (insert or update).
2. For each track: insert new tracks, update existing tracks.
3. For each milestone on each track: insert new milestones, update existing milestones.

The `loadHabit()` method must load tracks and milestones from the database when editing an
existing habit.

Remove `dailyTexts` from the state, the `setDailyText` method, and the save logic.

## UI Layer

### Activity View Changes

File: `app/src/main/kotlin/com/habit/ui/ActivityView.kt`

Add a track dropdown between the habit name row and the timer/note section in
`CurrentActivityView`. See `tracks-ux-design.md` for layout details.

```kotlin
// after the habit name row, before the timer:
if (state.availableTracks.isNotEmpty()) {
    TrackSelector(
        tracks = state.availableTracks,
        selectedTrackId = state.activeActivity?.trackId,
        onSelect = onSelectTrack
    )
    if (state.incompleteMilestones.isNotEmpty() || state.selectedMilestone != null) {
        MilestoneSelector(
            selected = state.selectedMilestone,
            incomplete = state.incompleteMilestones,
            onSelect = onSelectMilestone,
            onComplete = onCompleteMilestone
        )
    }
}
```

`TrackSelector` is a dropdown using `ExposedDropdownMenuBox` with `ControlShape`, matching the
style guide. `MilestoneSelector` shows the selected milestone with a checkbox and a dropdown to
pick from incomplete milestones.

Thread `onSelectTrack: (String?) -> Unit`, `onSelectMilestone: (Long) -> Unit`, and
`onCompleteMilestone: () -> Unit` through the composable hierarchy from `PrimaryScreen` to
`CurrentActivityView`.

In `HistoryActivityView` and `CompletedActivityDetail`, show the track name and milestone (if
present) as read-only text below the habit name.

### Habit Editor Changes

File: `app/src/main/kotlin/com/habit/ui/HabitEditorScreen.kt`

- Rename the "Tracking" field group to "Timekeeping".
- Remove the "Daily texts" field group.
- Add a "Tracks" field group between Timekeeping and the delete button.
- The Tracks group contains the track list, inline editor, and add button as described in the
  UX design.

### Completed List Changes

File: `app/src/main/kotlin/com/habit/ui/CompletedList.kt`

Show the track name as a secondary line below the habit name when a track was selected for the
activity.

## Navigation

No new routes. Track management is inline in the habit editor. Track selection is inline in the
activity view.

## Dependency Wiring

Update `AppContainer`:

```kotlin
class AppContainer(context: Context) {
    // ... existing fields ...

    val trackRepo = TrackRepository(database.trackDao(), database.milestoneDao())
}
```

Add `trackDao()` and `milestoneDao()` abstract methods to `HabitDatabase`.

Update `AgendaViewModelFactory` and `HabitEditorViewModelFactory` to pass `trackRepo`.

## Config Loading Changes

### Seed-Only-When-Empty Strategy

Change `HabitApp.loadHabits()` to check if the database is empty before loading from JSON.
Replace the current "insert with IGNORE on every start" pattern.

File: `app/src/main/kotlin/com/habit/HabitApp.kt`

```kotlin
private fun loadHabits() {
    appScope.launch {
        val habitCount = container.habitRepo.count()
        if (habitCount == 0) {
            container.habitRepo.loadFromConfig(container.habits)
            container.tallyRepo.loadFromConfig(container.tallies)
            container.trackRepo.loadFromConfig(
                container.tracks,
                container.milestones
            )
        }
    }
}
```

Add `suspend fun count(): Int` to `HabitRepository` (backed by `SELECT COUNT(*) FROM habit`).

### JSON Config Changes

Add tracks and milestones to `habits.json`. Remove `dailyTexts` from habit entries.

File: `app/src/main/kotlin/com/habit/data/ConfigLoader.kt`

```kotlin
@Serializable
data class HabitJson(
    val id: String,
    val name: String,
    val timesOfDay: List<Int>,
    val sortOrder: Int,
    val daysActive: List<String>,
    val dailyTarget: Int,
    val dailyTargetMode: String,
    val timed: Boolean,
    val goalMinutes: Int? = null,
    val stopMinutes: Int? = null,
    val priority: String
    // dailyTexts removed
)

@Serializable
data class TrackJson(
    val id: String,
    val habitId: String,
    val name: String,
    val priority: String,
    val dayOfWeek: String? = null,
    val milestones: List<String> = emptyList()
)

@Serializable
data class AppConfigJson(
    val dayBoundaryHour: Int,
    val habits: List<HabitJson>,
    val tallies: List<TallyJson> = emptyList(),
    val tracks: List<TrackJson> = emptyList()
)

data class AppConfig(
    val dayBoundaryHour: Int,
    val habits: List<Habit>,
    val tallies: List<Tally>,
    val tracks: List<Track>,
    val milestones: Map<String, List<Milestone>>
)
```

`milestones` is keyed by track ID. Each milestone name in the JSON becomes a `Milestone` with
`sortOrder` derived from its list position.

### Converters

Remove `fromDailyTexts` and `toDailyTexts` converters from `Converters.kt`. Add a converter for
nullable `DayOfWeek`:

```kotlin
@TypeConverter
fun fromDayOfWeek(day: DayOfWeek?): String? = day?.name

@TypeConverter
fun toDayOfWeek(value: String?): DayOfWeek? = value?.let { DayOfWeek.valueOf(it) }
```

## Testing Plan

### DAO Tests

- **TrackDao**: insert, query by habit, query active only (excludes archived), update, delete,
  cascade delete when habit deleted.
- **MilestoneDao**: insert, query by track, `defaultMilestone` returns first incomplete,
  `incompleteMilestones` returns all incomplete, `maxSortOrder`, cascade delete when track
  deleted.
- **ActivityDao**: verify existing queries work with null `trackId`/`milestoneId`. verify
  activities with `trackId` set are returned correctly.

### Repository Tests

- **TrackRepository**: passthrough verification. `loadFromConfig` inserts tracks and milestones.
- **HabitRepository**: `count()` returns correct value. `loadFromConfig` still works.

### ViewModel Tests

- **AgendaViewModel**:
    - `loadTracksForHabit` populates `availableTracks` sorted by day default then priority.
    - `selectTrack` updates the activity's `trackId`, defaults to first incomplete milestone,
      and populates `incompleteMilestones`.
    - `selectTrack(null)` clears track, milestone, and incomplete list.
    - `selectMilestone` allows picking any incomplete milestone.
    - `completeMilestone` marks selected milestone done, advances to next default, refreshes
      incomplete list.
    - `completeMilestone` with last milestone leaves `selectedMilestone` null.
    - habits with no tracks have empty `availableTracks`.

- **HabitEditorViewModel**:
    - `loadHabit` loads tracks and milestones from repo.
    - `addTrack` adds an expanded new item.
    - `archiveTrack` / `unarchiveTrack` toggle the flag.
    - `deleteTrack` removes track from list (only when no activities reference it).
    - `addMilestone` appends with correct sort order.
    - `deleteMilestone` removes milestone from list (only when no activities reference it).
    - `save` persists new and modified tracks and milestones.
    - saving without `dailyTexts` works correctly.

### Compose Functional Tests

- **Activity view**: track dropdown appears for habits with tracks, hidden for habits without.
  selecting a track updates the display. milestone line appears for series tracks. tapping
  milestone checkbox advances the series.
- **Habit editor**: tracks field group appears. add track flow works. inline editor expands and
  collapses. archive/unarchive toggles. milestone add flow works.
- **Completed list**: track name appears as secondary line when present.
- **History view**: track name and milestone display correctly for past activities.

### Migration Test

- verify `MIGRATION_8_9` creates track and milestone tables, adds columns to activity, drops
  dailyTexts from habit.
- verify existing habit, activity, tally, and choice data is preserved.

### Config Loading Test

- verify seed data loads only when database is empty.
- verify seed data is not reloaded on subsequent starts.
- verify tracks and milestones from JSON are inserted correctly.
