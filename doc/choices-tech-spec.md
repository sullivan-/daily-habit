# Choices Tech Spec

## Overview

Technical specification for the choices feature, implementing the behavior defined in
`choices-func-spec.md`. Adds a new screen for tracking unwanted habits as tallies, where
each tally accumulates binary choices (abstain or indulge). Uses the same MVVM + Room +
manual DI architecture as the existing app.

No UX design doc exists for this feature. The func spec defines the screen layout
directly: a list of tally rows with inline Yes/No buttons, an M/N indicator, and edit
navigation.

## Domain Entities

### Tally

The persistent definition of something the user is tracking. Analogous to `Habit` for
positive behaviors.

File: `app/src/main/kotlin/com/habit/data/Tally.kt`

```kotlin
@Entity(tableName = "tally")
data class Tally(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val priority: Priority
)
```

Uses the existing `Priority` enum (HIGH, MEDIUM_HIGH, MEDIUM, MEDIUM_LOW, LOW).

### Choice

A single recorded event on a tally — the user was tempted and chose to abstain or
indulge.

File: `app/src/main/kotlin/com/habit/data/Choice.kt`

```kotlin
@Entity(
    tableName = "choice",
    foreignKeys = [ForeignKey(
        entity = Tally::class,
        parentColumns = ["id"],
        childColumns = ["tallyId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("tallyId"), Index("timestamp")]
)
data class Choice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tallyId: Long,
    val timestamp: Instant,
    val abstained: Boolean              // true = No (abstain), false = Yes (indulge)
)
```

## Database Layer

### Migration

Add migration from version 5 to 6 in `HabitDatabase.kt`. Register both new entities.

```kotlin
@Database(
    entities = [Habit::class, Activity::class, Tally::class, Choice::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun activityDao(): ActivityDao
    abstract fun tallyDao(): TallyDao
    abstract fun choiceDao(): ChoiceDao

    companion object {
        // existing migrations ...

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS tally (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        priority TEXT NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS choice (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        tallyId INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL,
                        abstained INTEGER NOT NULL,
                        FOREIGN KEY (tallyId) REFERENCES tally(id)
                            ON DELETE CASCADE
                    )
                """)
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_choice_tallyId ON choice(tallyId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_choice_timestamp ON choice(timestamp)"
                )
            }
        }
    }
}
```

Register `MIGRATION_5_6` in `AppContainer` when building the database.

### TallyDao

File: `app/src/main/kotlin/com/habit/data/TallyDao.kt`

```kotlin
@Dao
interface TallyDao {
    @Query("SELECT * FROM tally")
    fun allTallies(): Flow<List<Tally>>

    @Query("SELECT * FROM tally WHERE id = :id")
    suspend fun getById(id: Long): Tally?

    @Insert
    suspend fun insert(tally: Tally): Long

    @Update
    suspend fun update(tally: Tally)

    @Query("DELETE FROM tally WHERE id = :id")
    suspend fun deleteById(id: Long)
}
```

### ChoiceDao

File: `app/src/main/kotlin/com/habit/data/ChoiceDao.kt`

```kotlin
@Dao
interface ChoiceDao {
    @Insert
    suspend fun insert(choice: Choice): Long

    @Query(
        "SELECT * FROM choice WHERE tallyId = :tallyId " +
        "ORDER BY timestamp DESC LIMIT :limit"
    )
    suspend fun recentChoices(tallyId: Long, limit: Int): List<Choice>

    @Query(
        "SELECT * FROM choice WHERE tallyId = :tallyId " +
        "AND timestamp >= :since ORDER BY timestamp DESC"
    )
    suspend fun choicesSince(tallyId: Long, since: Long): List<Choice>

    @Query(
        "SELECT tallyId, COUNT(*) as count FROM choice " +
        "WHERE timestamp >= :since GROUP BY tallyId"
    )
    suspend fun choiceCountsSince(since: Long): List<TallyChoiceCount>

    @Query(
        "SELECT * FROM choice WHERE tallyId = :tallyId " +
        "AND timestamp >= :since AND timestamp < :until " +
        "ORDER BY timestamp DESC"
    )
    suspend fun choicesInRange(tallyId: Long, since: Long, until: Long): List<Choice>
}
```

### TallyChoiceCount

A projection for the grouped count query used in sort scoring.

File: `app/src/main/kotlin/com/habit/data/TallyChoiceCount.kt`

```kotlin
data class TallyChoiceCount(
    val tallyId: Long,
    val count: Int
)
```

## Repository Layer

### TallyRepository

File: `app/src/main/kotlin/com/habit/data/TallyRepository.kt`

```kotlin
class TallyRepository(private val tallyDao: TallyDao) {
    fun allTallies(): Flow<List<Tally>> = tallyDao.allTallies()
    suspend fun getById(id: Long): Tally? = tallyDao.getById(id)
    suspend fun insert(tally: Tally): Long = tallyDao.insert(tally)
    suspend fun update(tally: Tally) = tallyDao.update(tally)
    suspend fun deleteById(id: Long) = tallyDao.deleteById(id)
}
```

### ChoiceRepository

File: `app/src/main/kotlin/com/habit/data/ChoiceRepository.kt`

```kotlin
class ChoiceRepository(private val choiceDao: ChoiceDao) {
    suspend fun record(choice: Choice): Long = choiceDao.insert(choice)

    suspend fun recentChoices(tallyId: Long, limit: Int = 10): List<Choice> =
        choiceDao.recentChoices(tallyId, limit)

    suspend fun choiceCountsSince(since: Instant): List<TallyChoiceCount> =
        choiceDao.choiceCountsSince(since.toEpochMilli())

    suspend fun choicesToday(tallyId: Long, dayStart: Instant, dayEnd: Instant): List<Choice> =
        choiceDao.choicesInRange(tallyId, dayStart.toEpochMilli(), dayEnd.toEpochMilli())
}
```

## ViewModel Layer

### ChoicesViewModel

File: `app/src/main/kotlin/com/habit/viewmodel/ChoicesViewModel.kt`

```kotlin
data class TallyDisplayItem(
    val tally: Tally,
    val abstainCount: Int,              // M in M/N
    val totalCount: Int,                // N in M/N
    val ratio: Float,                   // abstainCount / totalCount, for color
    val sortScore: Float                // priority + recency
)

data class ChoicesUiState(
    val tallies: List<TallyDisplayItem> = emptyList()
)

class ChoicesViewModel(
    private val tallyRepo: TallyRepository,
    private val choiceRepo: ChoiceRepository,
    private val dayBoundary: DayBoundary
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChoicesUiState())
    val uiState: StateFlow<ChoicesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            tallyRepo.allTallies().collect { tallies ->
                refreshDisplay(tallies)
            }
        }
    }

    fun recordChoice(tallyId: Long, abstained: Boolean) {
        viewModelScope.launch {
            choiceRepo.record(
                Choice(
                    tallyId = tallyId,
                    timestamp = Instant.now(),
                    abstained = abstained
                )
            )
            refreshDisplay(tallyRepo.allTallies().first())
        }
    }

    private suspend fun refreshDisplay(tallies: List<Tally>) {
        val sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS)
        val weeklyCounts = choiceRepo.choiceCountsSince(sevenDaysAgo)
            .associateBy { it.tallyId }
        val maxWeeklyCount = weeklyCounts.values.maxOfOrNull { it.count } ?: 0

        val today = dayBoundary.today()
        val dayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val dayEnd = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        val items = tallies.map { tally ->
            val recent = choiceRepo.recentChoices(tally.id, 10)
            val todayChoices = choiceRepo.choicesToday(tally.id, dayStart, dayEnd)

            val useDaily = todayChoices.size > 10
            val displayChoices = if (useDaily) todayChoices else recent
            val abstainCount = displayChoices.count { it.abstained }
            val totalCount = displayChoices.size

            val priorityScore = priorityToScore(tally.priority)
            val weeklyCount = weeklyCounts[tally.id]?.count ?: 0
            val recencyScore = if (maxWeeklyCount > 0) {
                weeklyCount.toFloat() / maxWeeklyCount
            } else 0f

            TallyDisplayItem(
                tally = tally,
                abstainCount = abstainCount,
                totalCount = totalCount,
                ratio = if (totalCount > 0) abstainCount.toFloat() / totalCount else 1f,
                sortScore = priorityScore + recencyScore
            )
        }.sortedByDescending { it.sortScore }

        _uiState.value = ChoicesUiState(tallies = items)
    }
}
```

### Priority Score Mapping

```kotlin
fun priorityToScore(priority: Priority): Float = when (priority) {
    Priority.LOW -> 0.2f
    Priority.MEDIUM_LOW -> 0.4f
    Priority.MEDIUM -> 0.6f
    Priority.MEDIUM_HIGH -> 0.8f
    Priority.HIGH -> 1.0f
}
```

This function should live in `Priority.kt` as an extension or companion method, since it
may be reused across features.

### TallyEditorViewModel

File: `app/src/main/kotlin/com/habit/viewmodel/TallyEditorViewModel.kt`

Follows the same pattern as `HabitEditorViewModel`.

```kotlin
data class TallyEditorState(
    val id: Long = 0,
    val name: String = "",
    val priority: Priority = Priority.MEDIUM,
    val isNew: Boolean = true,
    val dirty: Boolean = false,
    val saved: Boolean = false,
    val deleted: Boolean = false
) {
    val isValid: Boolean get() = name.isNotBlank()
}

class TallyEditorViewModel(
    private val tallyRepo: TallyRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TallyEditorState())
    val state: StateFlow<TallyEditorState> = _state.asStateFlow()

    fun loadTally(tallyId: Long) {
        viewModelScope.launch {
            tallyRepo.getById(tallyId)?.let { tally ->
                _state.value = TallyEditorState(
                    id = tally.id,
                    name = tally.name,
                    priority = tally.priority,
                    isNew = false
                )
            }
        }
    }

    fun setName(name: String) {
        _state.value = _state.value.copy(name = name, dirty = true)
    }

    fun setPriority(priority: Priority) {
        _state.value = _state.value.copy(priority = priority, dirty = true)
    }

    fun save() {
        val s = _state.value
        if (!s.isValid) return
        viewModelScope.launch {
            if (s.isNew) {
                tallyRepo.insert(Tally(name = s.name, priority = s.priority))
            } else {
                tallyRepo.update(Tally(id = s.id, name = s.name, priority = s.priority))
            }
            _state.value = s.copy(saved = true)
        }
    }

    fun delete() {
        viewModelScope.launch {
            tallyRepo.deleteById(_state.value.id)
            _state.value = _state.value.copy(deleted = true)
        }
    }
}
```

### ViewModel Factories

File: `app/src/main/kotlin/com/habit/viewmodel/ChoicesViewModelFactory.kt`

```kotlin
class ChoicesViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ChoicesViewModel(
            container.tallyRepo,
            container.choiceRepo,
            container.dayBoundary
        ) as T
    }
}
```

File: `app/src/main/kotlin/com/habit/viewmodel/TallyEditorViewModelFactory.kt`

```kotlin
class TallyEditorViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return TallyEditorViewModel(container.tallyRepo) as T
    }
}
```

## UI Layer

### ChoicesScreen

File: `app/src/main/kotlin/com/habit/ui/ChoicesScreen.kt`

```kotlin
@Composable
fun ChoicesScreen(
    viewModel: ChoicesViewModel,
    onEditTally: (Long) -> Unit,
    onNewTally: () -> Unit,
    onBack: () -> Unit
)
```

Structure:
- `TopAppBar` with back arrow and title "Choices"
- `LazyColumn` of `TallyRow` items, sorted by `sortScore` descending
- `FloatingActionButton` for creating a new tally

### TallyRow

```kotlin
@Composable
fun TallyRow(
    item: TallyDisplayItem,
    onNo: () -> Unit,
    onYes: () -> Unit,
    onEdit: () -> Unit
)
```

Each row displays:
- tally name (start-aligned)
- M/N indicator text, colored on a green-to-red gradient based on `ratio`
- No button (abstain)
- Yes button (indulge)
- edit icon button

### Indicator Color

```kotlin
fun indicatorColor(ratio: Float): Color {
    // ratio 1.0 (all abstain) = green
    // ratio 0.0 (all indulge) = red
    // linear interpolation between
    return lerp(Color.Red, Color.Green, ratio)
}
```

When `totalCount` is 0 (no choices yet), display no indicator text.

### TallyEditorScreen

File: `app/src/main/kotlin/com/habit/ui/TallyEditorScreen.kt`

```kotlin
@Composable
fun TallyEditorScreen(
    viewModel: TallyEditorViewModel,
    tallyId: Long?,
    onBack: () -> Unit
)
```

Structure:
- `TopAppBar` with back arrow and title "Edit Tally" / "New Tally"
- name text field
- priority selector (same pattern as habit editor)
- save button
- delete button (only for existing tallies, with confirmation)

## Navigation

Add three new routes to `AppNavigation.kt`:

```kotlin
composable("choices") {
    val choicesVm: ChoicesViewModel = viewModel(
        factory = ChoicesViewModelFactory(container)
    )
    ChoicesScreen(
        viewModel = choicesVm,
        onEditTally = { tallyId -> navController.navigate("tally-editor/$tallyId") },
        onNewTally = { navController.navigate("tally-editor/new") },
        onBack = { navController.popBackStack() }
    )
}

composable("tally-editor/{tallyId}") { backStackEntry ->
    val tallyIdStr = backStackEntry.arguments?.getString("tallyId")
    val editorVm: TallyEditorViewModel = viewModel(
        factory = TallyEditorViewModelFactory(container)
    )
    TallyEditorScreen(
        viewModel = editorVm,
        tallyId = if (tallyIdStr == "new") null else tallyIdStr?.toLongOrNull(),
        onBack = { navController.popBackStack() }
    )
}
```

### Menu Update

Add "Choices" to `MenuButton.kt`:

```kotlin
DropdownMenuItem(
    text = { Text("Choices", style = MaterialTheme.typography.bodyLarge) },
    onClick = { expanded = false; onChoices() }
)
```

Thread `onChoices` callback through `PrimaryScreen` → `ProgressBar`/`AgendaBar` →
`MenuButton`, same pattern as `onHabitList`. Wire it in `AppNavigation` to navigate to
the `"choices"` route.

## Dependency Wiring

Update `AppContainer`:

```kotlin
class AppContainer(context: Context) {
    // ... existing fields ...

    val tallyRepo = TallyRepository(database.tallyDao())
    val choiceRepo = ChoiceRepository(database.choiceDao())
}
```

Register `MIGRATION_5_6` in the database builder alongside the existing migrations.

## Testing Plan

### DAO Tests

- `TallyDao`: insert, query all, query by id, update, delete. verify cascade deletes
  choices when tally is deleted.
- `ChoiceDao`: insert, `recentChoices` returns correct limit and order,
  `choiceCountsSince` groups correctly, `choicesInRange` filters by time window.

### Repository Tests

- `TallyRepository`: passthrough verification for all DAO operations.
- `ChoiceRepository`: verify `record` inserts, `recentChoices` delegates with default
  limit, `choiceCountsSince` converts `Instant` to millis correctly.

### ViewModel Tests

- `ChoicesViewModel`:
  - initial state loads tallies with indicators.
  - `recordChoice` with abstain updates indicator correctly.
  - `recordChoice` with indulge updates indicator correctly.
  - sort order reflects priority + recency blend.
  - high-priority tally with no recent activity sorts below low-priority tally with
    high recent activity (when recency outweighs priority).
  - indicator switches to daily counts when > 10 choices in a day.
  - indicator shows no text when tally has zero choices.
  - ratio is 1.0 (green) when all choices are abstain.
  - ratio is 0.0 (red) when all choices are indulge.
- `TallyEditorViewModel`:
  - new tally: `isNew` is true, `isValid` is false with blank name.
  - `setName` / `setPriority` mark state dirty.
  - `save` new tally inserts and sets `saved` flag.
  - `loadTally` populates state from existing tally.
  - `save` existing tally updates and sets `saved` flag.
  - `delete` removes tally and sets `deleted` flag.

### Sort Algorithm Tests

- tally with priority HIGH (1.0) and no recent choices scores 1.0.
- tally with priority LOW (0.2) and max recent choices scores 1.2.
- tally with priority HIGH and max recent choices scores 2.0.
- all tallies with zero choices in the last 7 days: sort by priority alone.
- recency is relative — only the most active tally gets 1.0, others are proportional.

### Compose Functional Tests

- choices screen shows empty state when no tallies exist.
- create flow: tap FAB → editor → save → tally appears in list.
- tapping No records an abstain choice and updates the indicator.
- tapping Yes records an indulge choice and updates the indicator.
- edit flow: tap edit → editor populated → change name → save → list updated.
- delete flow: tap edit → delete → confirmation → tally removed from list.
- back navigation returns to main screen.

### Migration Test

- verify `MIGRATION_5_6` creates both tables with correct schema.
- verify existing habit and activity data is preserved after migration.
