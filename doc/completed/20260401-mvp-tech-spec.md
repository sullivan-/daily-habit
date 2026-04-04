# MVP Tech Spec

## Overview

Technical specification for the MVP habit tracking app, implementing the
behavior defined in `mvp-func-spec.md` and the UI described in
`mvp-ux-design.md`. The app uses MVVM with Jetpack Compose, Room, and manual
DI as described in `doc/architectural-model.md`.

## Build Infrastructure

This is a greenfield project. The following must be set up before any feature
code is written.

### Project Structure

```
habit/
├── build.gradle.kts          # root build file (plugin versions)
├── settings.gradle.kts       # project name, module includes
├── gradle.properties          # JVM args, AndroidX flags
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradlew                    # Unix wrapper script
├── gradlew.bat                # Windows wrapper script
├── app/
│   ├── build.gradle.kts      # module build file (dependencies, config)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── assets/
│       │   │   └── habits.json
│       │   ├── kotlin/
│       │   │   └── com/habit/
│       │   │       ├── HabitApp.kt
│       │   │       ├── data/         # entities, DAOs, database, repos
│       │   │       ├── ui/           # Compose screens, components
│       │   │       ├── viewmodel/    # ViewModels, UI state
│       │   │       └── service/      # TimerService
│       │   └── res/
│       │       ├── values/
│       │       │   └── strings.xml
│       │       └── ...
│       ├── test/                     # local JVM tests
│       │   └── kotlin/
│       │       └── com/habit/
│       └── androidTest/              # instrumented tests
│           └── kotlin/
│               └── com/habit/
├── .gitignore
└── doc/                              # specs and design docs
```

### Root build.gradle.kts

```kotlin
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.1.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.10" apply false
}
```

### settings.gradle.kts

```kotlin
rootProject.name = "habit"
include(":app")
```

### app/build.gradle.kts

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    jacoco
}

android {
    namespace = "com.habit"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.habit"
        minSdk = 33        // Pixel 7 ships with API 33
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2025.01.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation(
        "androidx.lifecycle:lifecycle-runtime-compose:2.8.7"
    )

    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    annotationProcessor("androidx.room:room-compiler:$roomVersion")

    // JSON parsing (for config loading)
    implementation(
        "org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3"
    )

    // Local tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.google.truth:truth:1.4.4")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("app.cash.turbine:turbine:1.2.0")
    testImplementation("androidx.room:room-testing:$roomVersion")
    testImplementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0"
    )

    // Instrumented tests
    androidTestImplementation(composeBom)
    androidTestImplementation(
        "androidx.compose.ui:ui-test-junit4"
    )
    debugImplementation(
        "androidx.compose.ui:ui-test-manifest"
    )
    androidTestImplementation(
        "androidx.test.ext:junit:1.2.1"
    )
}
```

### gradle.properties

```properties
org.gradle.jvmargs=-Xmx2048m
android.useAndroidX=true
kotlin.code.style=official
```

### AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission
        android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission
        android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:name=".HabitApp"
        android:label="Habit"
        android:theme="@style/Theme.Material3.DayNight">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action
                    android:name="android.intent.action.MAIN" />
                <category
                    android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".service.TimerService"
            android:foregroundServiceType="mediaPlayback" />
    </application>
</manifest>
```

### .gitignore

```
.gradle/
build/
local.properties
*.iml
.idea/
.DS_Store
app/build/
```

## Domain Entities

### Habit

The persistent habit definition, loaded from static JSON config at app startup.

```kotlin
@Entity(tableName = "habit")
data class Habit(
    @PrimaryKey val id: String,
    val name: String,
    val timeOfDay: Int,               // hour (0-23)
    val sortOrder: Int,
    val daysActive: Set<DayOfWeek>,   // stored via type converter
    val dailyTarget: Int,             // default 1
    val dailyTargetMode: TargetMode,  // EXACTLY or AT_LEAST
    val timed: Boolean,
    val chimeIntervalSeconds: Int?,   // null if no chime
    val thresholdMinutes: Int?,       // null if no threshold
    val thresholdType: ThresholdType?, // null if no threshold
    val priority: Priority,
    val dailyTexts: Map<DayOfWeek, String> // stored via type converter
)

enum class TargetMode { EXACTLY, AT_LEAST }

enum class ThresholdType { MINIMUM_DONE, TIME_TO_STOP, GOAL_MET }

enum class Priority {
    HIGH, MEDIUM_HIGH, MEDIUM, MEDIUM_LOW, LOW
}
```

### Activity

A single recorded instance of performing a habit.

```kotlin
@Entity(
    tableName = "activity",
    foreignKeys = [ForeignKey(
        entity = Habit::class,
        parentColumns = ["id"],
        childColumns = ["habitId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("habitId"), Index("attributedDate")]
)
data class Activity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: String,
    val attributedDate: LocalDate,    // respects day boundary
    val startTime: Instant?,          // null for untimed
    val endTime: Instant?,            // null until done (or untimed)
    val elapsedMs: Long,              // accumulated running time
    val note: String,
    val completedAt: Instant?         // null while in progress
)
```

### AppConfig

Top-level config loaded from the bundled JSON.

```kotlin
data class AppConfig(
    val dayBoundaryHour: Int,         // default 2
    val habits: List<Habit>
)
```

### Type Converters

Room needs type converters for non-primitive types:

```kotlin
class Converters {
    // Set<DayOfWeek> ↔ String (comma-separated)
    // Map<DayOfWeek, String> ↔ String (JSON)
    // LocalDate ↔ Long (epoch day)
    // Instant ↔ Long (epoch millis)
    // enums ↔ String (name)
}
```

## Database Layer

### HabitDatabase

```kotlin
@Database(
    entities = [Habit::class, Activity::class],
    version = 1
)
@TypeConverters(Converters::class)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun activityDao(): ActivityDao
}
```

### HabitDao

```kotlin
@Dao
interface HabitDao {
    @Query("SELECT * FROM habit")
    fun allHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habit WHERE id = :id")
    suspend fun getById(id: String): Habit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(habits: List<Habit>)
}
```

### ActivityDao

```kotlin
@Dao
interface ActivityDao {
    @Query(
        "SELECT * FROM activity WHERE attributedDate = :date"
    )
    fun activitiesForDate(date: LocalDate): Flow<List<Activity>>

    @Query(
        "SELECT * FROM activity " +
        "WHERE habitId = :habitId AND attributedDate = :date"
    )
    fun activitiesForHabitOnDate(
        habitId: String,
        date: LocalDate
    ): Flow<List<Activity>>

    @Insert
    suspend fun insert(activity: Activity): Long

    @Update
    suspend fun update(activity: Activity)
}
```

## Repository Layer

### HabitRepository

```kotlin
class HabitRepository(private val habitDao: HabitDao) {
    fun allHabits(): Flow<List<Habit>> = habitDao.allHabits()

    suspend fun getById(id: String): Habit? = habitDao.getById(id)

    suspend fun loadFromConfig(habits: List<Habit>) {
        habitDao.insertAll(habits)
    }
}
```

### ActivityRepository

```kotlin
class ActivityRepository(private val activityDao: ActivityDao) {
    fun activitiesForDate(date: LocalDate): Flow<List<Activity>> =
        activityDao.activitiesForDate(date)

    fun activitiesForHabitOnDate(
        habitId: String,
        date: LocalDate
    ): Flow<List<Activity>> =
        activityDao.activitiesForHabitOnDate(habitId, date)

    suspend fun create(activity: Activity): Long =
        activityDao.insert(activity)

    suspend fun update(activity: Activity) =
        activityDao.update(activity)
}
```

## ViewModel Layer

### AgendaViewModel

The primary ViewModel, backing the main screen in all three layouts.

```kotlin
data class AgendaUiState(
    val layout: Layout = Layout.MAIN,
    val habits: List<Habit> = emptyList(),
    val todayActivities: List<Activity> = emptyList(),
    val selectedHabitId: String? = null,
    val activeActivity: Activity? = null,
    val timerRunning: Boolean = false,
    val elapsedMs: Long = 0
) {
    val agendaItems: List<AgendaItem>
        get() = // derived: incomplete habits sorted by display ordering

    val completedItems: List<CompletedItem>
        get() = // derived: completed activities sorted chronologically

    val progressCount: Int
        get() = todayActivities.count { it.completedAt != null }

    val totalTarget: Int
        get() = habits.sumOf { it.dailyTarget }
}

enum class Layout { MAIN, REVIEW, ACTIVITY_FOCUSED }
```

Key actions:

```kotlin
class AgendaViewModel(
    private val habitRepo: HabitRepository,
    private val activityRepo: ActivityRepository,
    private val dayBoundary: DayBoundary
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgendaUiState())
    val uiState: StateFlow<AgendaUiState> = _uiState.asStateFlow()

    // layout navigation
    fun switchToReview() { ... }
    fun switchToMain() { ... }
    fun expandActivity() { ... }

    // habit selection
    fun selectHabit(habitId: String) { ... }
    fun selectCompletedActivity(activityId: Long) { ... }
    fun clearSelection() { ... }

    // timer actions (timed habits)
    fun startTimer() { ... }
    fun stopTimer() { ... }

    // completing activities
    fun completeActivity(note: String) { ... }
    fun completeUntimed(habitId: String, note: String) { ... }

    // notes
    fun updateNote(note: String) { ... }

    // extra activities
    fun doAgain(habitId: String) { ... }
}
```

### DayBoundary

Utility that computes the attributed date from the current time and the
configured day boundary hour.

```kotlin
class DayBoundary(private val boundaryHour: Int) {
    fun today(): LocalDate {
        val now = LocalDateTime.now()
        return if (now.hour < boundaryHour) {
            now.toLocalDate().minusDays(1)
        } else {
            now.toLocalDate()
        }
    }

    fun attributedDate(instant: Instant): LocalDate {
        val local = instant.atZone(ZoneId.systemDefault())
        return if (local.hour < boundaryHour) {
            local.toLocalDate().minusDays(1)
        } else {
            local.toLocalDate()
        }
    }
}
```

## UI Layer

All UI is Jetpack Compose. The primary screen is a single composable that
switches its content based on the current layout in `AgendaUiState`.

### Screen Structure

```kotlin
@Composable
fun PrimaryScreen(viewModel: AgendaViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column {
        ActivityView(
            state = uiState,
            onStart = viewModel::startTimer,
            onStop = viewModel::stopTimer,
            onComplete = viewModel::completeActivity,
            onNoteChange = viewModel::updateNote,
            onExpand = viewModel::expandActivity
        )

        when (uiState.layout) {
            Layout.MAIN -> {
                AgendaList(
                    items = uiState.agendaItems,
                    onSelect = viewModel::selectHabit
                )
                ProgressBar(
                    completed = uiState.progressCount,
                    total = uiState.totalTarget,
                    onClick = viewModel::switchToReview
                )
            }
            Layout.REVIEW -> {
                CompletedList(
                    items = uiState.completedItems,
                    onSelect = viewModel::selectCompletedActivity,
                    onDoAgain = viewModel::doAgain
                )
                AgendaBar(
                    remaining = uiState.totalTarget
                        - uiState.progressCount,
                    onClick = viewModel::switchToMain
                )
            }
            Layout.ACTIVITY_FOCUSED -> {
                ActivityDetail(state = uiState)
                AgendaBar(
                    remaining = uiState.totalTarget
                        - uiState.progressCount,
                    onClick = viewModel::switchToMain
                )
            }
        }
    }
}
```

### Composable Breakdown

| Composable         | Purpose                                     |
|--------------------|---------------------------------------------|
| `PrimaryScreen`    | top-level layout switcher                   |
| `ActivityView`     | top section: habit focus, timer, note field  |
| `AgendaList`       | scrollable list of upcoming activities      |
| `CompletedList`    | scrollable list of completed activities     |
| `ProgressBar`      | bottom bar with progress count + color      |
| `AgendaBar`        | bottom bar with remaining count             |
| `ActivityDetail`   | expanded detail for activity focused layout |
| `TimerDisplay`     | elapsed time, start/stop/done buttons       |
| `NoteField`        | editable text field with auto-save          |

## Navigation

The MVP is a single-screen app with layout switching handled by ViewModel
state (no Compose Navigation needed). If settings or history screens are added
later, Compose Navigation will be introduced at that point.

## Timer Foreground Service

When the app is backgrounded with a running timer, a foreground service keeps
the timer and chimes alive.

```kotlin
class TimerService : Service() {
    // starts when timer begins and app is backgrounded
    // shows persistent notification with habit name + elapsed time
    // plays chime sounds at configured intervals
    // plays threshold chime when threshold is reached
    // stops when activity is completed or timer is stopped
}
```

The service communicates with the ViewModel via a bound service pattern or
a shared `StateFlow`. Details:

- Notification channel created at app startup.
- Notification shows habit name and live elapsed time, updated every second.
- Tapping the notification returns to the app in the same layout state.
- Chime audio uses `MediaPlayer` or `SoundPool` for short sounds.
- The service must request `FOREGROUND_SERVICE` permission in the manifest.

## Dependency Wiring

### AppContainer

```kotlin
class AppContainer(context: Context) {
    private val database = Room.databaseBuilder(
        context, HabitDatabase::class.java, "habit.db"
    ).build()

    val habitRepo = HabitRepository(database.habitDao())
    val activityRepo = ActivityRepository(database.activityDao())
    val dayBoundary = DayBoundary(boundaryHour = 2) // from config
}
```

### HabitApp

```kotlin
class HabitApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
```

The ViewModel accesses the container via the `Application` instance. A
`ViewModelProvider.Factory` bridges the gap:

```kotlin
class AgendaViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AgendaViewModel(
            container.habitRepo,
            container.activityRepo,
            container.dayBoundary
        ) as T
    }
}
```

## Static Configuration

Habits are defined in `app/src/main/assets/habits.json`, loaded at first
launch and inserted into Room. The JSON structure:

```json
{
  "dayBoundaryHour": 2,
  "habits": [
    {
      "id": "qigong",
      "name": "Qigong",
      "timeOfDay": 7,
      "sortOrder": 1,
      "daysActive": ["MONDAY", "TUESDAY", "WEDNESDAY",
                      "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"],
      "dailyTarget": 2,
      "dailyTargetMode": "AT_LEAST",
      "timed": true,
      "chimeIntervalSeconds": 10,
      "thresholdMinutes": 30,
      "thresholdType": "GOAL_MET",
      "priority": "HIGH",
      "dailyTexts": {}
    }
  ]
}
```

A `ConfigLoader` reads this on first launch (or when the config version
changes) and calls `habitRepo.loadFromConfig()`. The `OnConflictStrategy
.REPLACE` on the DAO insert handles config updates cleanly.

## Display Ordering Algorithm

The agenda is sorted by the rules in the func spec. Implementation:

```kotlin
fun sortAgenda(
    habits: List<Habit>,
    activities: List<Activity>,
    today: LocalDate
): List<AgendaItem> {
    // 1. for each habit active today, compute how many activities
    //    are completed and how many remain
    // 2. generate one AgendaItem per next-pending activity
    // 3. sort by:
    //    a. activity number (first activities before second, etc.)
    //    b. within first activities: time of day, then sort order
    //    c. within subsequent activities: priority, then sort order
    // 4. return sorted list
}
```

*Note:* Per the func spec, this algorithm is intentionally loosely defined
for the MVP. Start simple and refine based on real use.

## Progress Bar Color

The progress bar color is computed by comparing completed activities to what
"should" be done by the current time. A simple initial approach:

```kotlin
fun progressStatus(
    completed: Int,
    habits: List<Habit>,
    now: LocalDateTime
): ProgressColor {
    // count activities whose time-of-day slot is at or before now
    // compare to completed count
    // blue = within 1, green = ahead, red = behind
}

enum class ProgressColor { BLUE, GREEN, RED }
```

*Note:* Per the func spec, refine this heuristic based on real use.

## Testing Plan

### DAO Tests

- `HabitDao`: insert habits, query all, query by ID.
- `ActivityDao`: insert activity, query by date, query by habit + date,
  update activity note, update elapsed time.

### Repository Tests

- `HabitRepository`: load from config populates database, subsequent loads
  update via replace.
- `ActivityRepository`: create activity, update note, update timer fields.

### DayBoundary Tests

- `today()` returns previous date when before boundary hour.
- `today()` returns current date when at or after boundary hour.
- `attributedDate()` handles midnight-to-boundary window correctly.

### ViewModel Tests

- initial state loads habits and today's activities.
- `selectHabit()` updates selected state.
- `startTimer()` / `stopTimer()` toggles timer state.
- `completeActivity()` creates activity record, advances to next habit.
- `completeUntimed()` creates activity, removes habit from agenda if target
  met.
- `doAgain()` adds a new activity for an "at least" habit.
- layout switching updates `layout` field.
- agenda items are correctly sorted by display ordering.
- progress count updates as activities are completed.

### Display Ordering Tests

- first activities sort by time of day, then sort order.
- first activities of all habits appear before second activities.
- subsequent activities sort by priority, then sort order.
- habits not active today are excluded.
- habits with all target activities complete are excluded.

### Compose Functional Tests

- main layout: agenda shows incomplete habits, progress bar shows count.
- tap agenda item: activity view shows habit details.
- tap done on timed habit: activity moves to completed, next habit focused.
- tap checkbox on untimed habit: same behavior.
- tap progress bar: switches to review layout.
- tap agenda bar: switches back to main layout.
- review layout: completed list shows finished activities with timestamps.
- tap completed item: activity view shows details, note is editable.
- do again: returns to main layout with habit ready to start.
- note field: text entry saves, daily text pre-fills when configured.

### Timer Tests

- start records start time, elapsed increases.
- stop pauses elapsed, resume continues from where it left off.
- done while running stops and completes.
- done while paused completes with current elapsed.
- switching habits while timer running prompts confirmation.
