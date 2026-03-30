---
name: tech-spec-auth
description: co-author a technical specification for a feature. use when the
  func spec and UX design are ready and the user wants to plan the
  implementation before writing code.
argument-hint: [feature name or path to func spec]
effort: high
---

Co-author a technical specification with me for: $ARGUMENTS

If no feature was specified, ask what feature to spec.

## Preparation

Before drafting:

1. Read project context: `CLAUDE.md` for conventions and design principles,
   `doc/habit-ubiquitous-language.md` for domain terminology, and
   `doc/architectural-model.md` for the app's architecture (MVVM, Compose,
   Room, manual DI).

2. Find and read the functional spec and UX design for this feature. Check
   `doc/` first (active working docs), then `doc/open/` (paused or future).
   If either is missing, discuss whether it should be written first.

   Active specs and designs:
   !`ls doc/*spec* doc/*ux* doc/*design* 2>/dev/null`
   Paused/future:
   !`ls doc/open/ 2>/dev/null`

3. Read 1-2 completed tech specs from `doc/closed/` that are most relevant
   to the feature. Use them to calibrate tone, depth, and structure — not
   as rigid templates.

   Completed tech specs:
   !`ls doc/closed/*tech* 2>/dev/null`

4. Read the actual code that will be modified — the relevant entities, DAOs,
   repositories, ViewModels, and Compose screens. A tech spec must be
   grounded in the current state of the code, not assumptions about it.

## Format

- use regular English capitalization in spec documents.
- line limit in markdown files is 100 characters.
- active working specs go directly in `doc/`. paused or future specs go in
  `doc/open/`. implemented specs move to `doc/closed/` with a date prefix
  (e.g., `20260505-mvp-tech-spec.md`).
- align columns in markdown tables.
- include Kotlin code blocks for entity definitions, DAO signatures, ViewModel
  state classes, and key data structures.

## Structure

Organize the spec by implementation layer, not by a rigid template. Most tech
specs include:

- **overview** — a short summary referencing the func spec and UX design.
- **domain entities** — Room `@Entity` data classes, enums, type converters.
  specify table names and file placement.
- **database layer** — Room database class, DAO interfaces with query
  signatures, migration scripts if modifying an existing schema. never modify
  existing migrations that have run on a device; always add new ones.
- **repository layer** — repository classes that wrap DAOs and expose `Flow`
  return types. any business logic that sits between the DAO and ViewModel.
- **ViewModel layer** — ViewModel classes, `StateFlow` definitions for UI
  state, action methods the UI calls. specify what state each ViewModel
  holds and how it reacts to user actions.
- **UI layer** — Compose screens and components. describe the composable
  structure, how it observes ViewModel state, and what actions it dispatches.
  reference the UX design for layout details rather than duplicating them.
- **navigation** — Compose Navigation routes and transitions between screens.
- **dependency wiring** — updates to `AppContainer` for new repositories,
  DAOs, or other dependencies. if the wiring is getting unwieldy, flag it
  as a candidate for Hilt migration.
- **static configuration** — changes to the bundled JSON config, new config
  fields, parsing logic.
- **services** — Android services (e.g., foreground service for timer),
  broadcast receivers, notification channels.
- **testing plan** — a dedicated section covering all test levels (see
  Testing below).

when the reasoning behind a technical choice isn't obvious, include a short
motivation blurb explaining why.

## Testing

every new feature gets tests. CI runs tests and JaCoCo coverage on every
build. tests and coverage must pass before a feature is considered done.
the tech spec author is responsible for specifying what tests are needed.

the testing plan should cover:

| layer        | test type  | runs on         | framework               |
|--------------|------------|-----------------|-------------------------|
| domain logic | unit       | JVM             | JUnit + Truth           |
| ViewModels   | unit       | JVM             | JUnit + MockK + Turbine |
| Room DAOs    | unit/integ | JVM/device      | JUnit + Room helpers    |
| UI screens   | functional | device/emulator | Compose Testing         |
| user flows   | functional | device/emulator | Compose Testing + Nav   |

for each layer touched by the feature:
- every new DAO method needs a DAO-level test.
- every new ViewModel needs unit tests for its state transitions and actions.
- every new screen needs Compose functional tests for its key interactions.
- multi-screen flows need navigation tests.

Compose UI code tends to report low JaCoCo coverage due to framework
internals — this is expected. domain logic, ViewModels, and DAOs are where
high coverage is both achievable and valuable.

## Technology Stack

- **language**: Kotlin
- **UI**: Jetpack Compose
- **architecture**: MVVM (ViewModel + StateFlow + Room)
- **database**: Room (SQLite)
- **dependency injection**: manual (AppContainer). flag if it's getting
  unwieldy — Hilt is the planned upgrade path.
- **build**: Gradle with Android Gradle Plugin
- **test frameworks**: JUnit, Truth, MockK, Turbine, Compose Testing
- **coverage**: JaCoCo
- **target device**: Pixel 7, GrapheneOS, no Google Play Services

## Collaboration

- start by asking clarifying questions before drafting.
- when seeking clarification or input on multiple points, work through them
  one at a time to maintain a good collaborative flow.
- present a first draft, then iterate based on feedback.

## Quality

- follow existing patterns in the codebase — check how similar features were
  built before proposing new patterns.
- flag any func spec or UX design ambiguities before baking them into the
  tech spec.
- think carefully about edge cases, error handling, and migration concerns.
- look for gaps — missing state transitions, unhandled user actions, implicit
  assumptions — and flag them.
- before finalizing, verify that the patterns and signatures proposed in the
  spec match the current codebase. re-read key files if needed.
- the tech spec should be self-contained enough to implement without
  constantly cross-referencing the func spec. behavioral details from the
  func spec (defaults, edge cases, toggle behaviors) should be explicit in
  the tech spec where they affect implementation.
- the goal is a spec detailed enough to implement without guesswork.
