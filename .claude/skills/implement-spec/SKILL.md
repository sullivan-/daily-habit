---
name: implement-spec
description: implement a technical spec end-to-end, including code and tests.
  use when there is a tech spec ready and the user wants to build the feature.
argument-hint: [feature name or path to tech spec]
effort: high
---

Implement the technical spec for: $ARGUMENTS

If no feature was specified, ask what feature to implement.

## Preparation

Before writing code:

1. Read project context: `CLAUDE.md` for conventions and design principles,
   `doc/habit-ubiquitous-language.md` for domain terminology, and
   `doc/architectural-model.md` for the app's architecture.

2. Find and read the technical spec for this feature. Check `doc/` first,
   then `doc/open/`.

   Active specs:
   !`ls doc/*tech* 2>/dev/null`
   Paused/future:
   !`ls doc/open/ 2>/dev/null`

3. Read the functional spec and UX design as well — they provide the "why"
   behind decisions that the tech spec may not fully explain.

4. Read the existing code that will be modified. Understand current state
   before changing anything.

5. Verify the existing tests pass before starting. Run:
   ```
   ./gradlew test
   ./gradlew connectedCheck
   ```
   to establish a green baseline. If something is already broken, flag it
   before proceeding.

## Implementation

- Stay on spec. If anything in the spec is unclear or seems wrong, ask
  before improvising.
- Work through the spec layer by layer: domain entities first, then
  database (DAOs), repository, ViewModel, UI.
- Compile after each layer — don't accumulate changes across multiple
  layers before verifying they compile:
  ```
  ./gradlew assembleDebug
  ```
- If the spec includes database schema changes (new migration), write and
  verify the migration before writing DAO code that depends on it. Never
  modify existing migrations that have run on a device.
- Follow existing patterns in the codebase — match the style of neighboring
  code.
- Every new class goes in its own file (except parts of sealed
  interfaces).
- For large specs, implement the whole thing autonomously rather than
  checking in between phases. But stop and ask if the situation on the
  ground requires a non-trivial deviation from the tech spec.

## Testing

After the implementation compiles, write the tests described in the tech
spec's testing section. The spec defines which tests to write and at which
level — follow it the same way you follow the implementation sections.

If the spec's testing section doesn't fully cover what was built, fall
back to these defaults:

- Every new DAO method needs a DAO-level test.
- Every new ViewModel needs unit tests for state transitions and actions.
- Every new screen needs Compose functional tests for key interactions.
- Multi-screen flows need navigation tests.

Then run the tests — **both old and new tests must pass**:

1. Run `./gradlew test` (local JVM tests). Fix any failures.
2. Run `./gradlew connectedCheck` (instrumented tests on device/emulator).
   Fix any failures.
3. Run `./gradlew testDebugUnitTestCoverage` and verify coverage is
   reasonable for the new code.

Do not consider the implementation complete until all tests pass.

## Completion

- Summarize what was implemented and any spec deviations or open questions.
- Offer to move the spec from `doc/` to `doc/completed/` with a date prefix
  (e.g., `20260505-mvp-tech-spec.md`).

## Process

- When seeking clarification or input on multiple points, work through them
  one at a time to maintain a good collaborative flow.
- If a test fails, diagnose the root cause — don't just retry.
