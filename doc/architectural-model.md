# Habit — Architectural Model

this document describes the architectural model for the Habit Android app. the
app uses **MVVM** (Model-View-ViewModel) with Jetpack Compose, following
standard Android conventions.

## Overview

the architecture has three layers. data flows one direction: the database emits
state changes upward through the ViewModel, and the UI renders whatever the
ViewModel provides. user actions flow downward: the UI calls ViewModel methods,
which call repository methods, which write to the database.

```
┌─────────────────────────────────────┐
│  View (Jetpack Compose)             │
│  observes state, sends user actions │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│  ViewModel                          │
│  holds UI state as StateFlow,       │
│  calls repository on user actions   │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│  Model (Room + Repositories)        │
│  database, DAOs, domain entities    │
└─────────────────────────────────────┘
```

## View (Jetpack Compose)

the UI layer. Compose functions observe the ViewModel's `StateFlow` and redraw
automatically when state changes. the view has no business logic — it renders
what the ViewModel tells it and forwards user actions (taps, input) back to
the ViewModel.

Compose `@Preview` annotations allow previewing screens in Android Studio
without building or deploying.

## ViewModel

sits between the UI and the data. responsibilities:

- holds UI state as Kotlin `StateFlow` (reactive streams)
- calls repository methods in response to user actions
- survives configuration changes (screen rotation) — Android destroys and
  recreates the UI on rotation, but the ViewModel persists
- contains no Android UI code — it is pure Kotlin, easy to unit test

## Model

the data and business logic layer:

- **Room** — Android's SQLite abstraction. provides compile-time verified SQL
  queries, reactive `Flow` return types, and migration support
- **DAOs** (Data Access Objects) — interfaces that define database queries.
  Room generates the implementations at compile time
- **repositories** — classes that wrap DAOs and provide a clean API to the
  ViewModel layer. a repository may combine multiple DAOs or add business
  logic on top of raw queries
- **domain entities** — Kotlin data classes representing habits, activities,
  etc. (see `doc/habit-ubiquitous-language.md` for definitions)

## Data Flow Example

```
user taps "complete"
  → Compose UI calls viewModel.completeActivity(habitId)
    → ViewModel calls repository.createActivity(habitId, today)
      → repository calls activityDao.insert(activity)
        → Room writes to SQLite
        → Room's Flow emits updated activity list
      → repository's Flow updates
    → ViewModel's StateFlow updates
  → Compose redraws the screen with the new state
```

## Build and Run

the app is built with **Gradle** using the **Android Gradle Plugin (AGP)**.
Kotlin is the sole language.

```
./gradlew assembleDebug      # compile + package a debug APK
./gradlew installDebug       # build + install on connected device/emulator
./gradlew test               # run local JVM tests
./gradlew connectedCheck     # run instrumented tests on device/emulator
```

for local development, the **Android Emulator** (ships with Android Studio,
uses KVM on Linux) provides quick iteration. the physical device (Pixel 7,
GrapheneOS) is the ground truth for final validation.

## Testing

every new feature gets tests. CI runs tests and JaCoCo coverage on every
build. tests and coverage must pass before signing off on a new feature.

| layer        | test type    | runs on         | framework              |
|--------------|--------------|-----------------|------------------------|
| domain logic | unit         | JVM             | JUnit + Truth          |
| ViewModels   | unit         | JVM             | JUnit + MockK + Turbine|
| Room DAOs    | unit/integ   | JVM/device      | JUnit + Room helpers   |
| UI screens   | functional   | device/emulator | Compose Testing        |
| user flows   | functional   | device/emulator | Compose Testing + Nav  |

### Coverage

JaCoCo is configured on debug builds:

```kotlin
android {
    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }
}
```

reports land in `app/build/reports/jacoco/`. Compose UI code tends to show
lower coverage numbers due to framework internals — this is expected. domain
logic, ViewModels, and DAOs are where high coverage is both achievable and
valuable.

## Dependency Injection

manual DI via an `AppContainer` that holds the database, DAOs, and
repositories. ViewModels receive their dependencies from the container.

```kotlin
class AppContainer(context: Context) {
    val database = Room.databaseBuilder(
        context, HabitDatabase::class.java, "habit.db"
    ).build()
    val habitRepo = HabitRepository(database.habitDao())
}
```

if the wiring becomes unwieldy, migrate to Hilt.
