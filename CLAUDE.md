# habit

a native Android app (Kotlin) for daily habit tracking, time management, and personal
improvement. designed for a Pixel 7 running GrapheneOS with mock Google Play Services.
all features must work fully offline with no Google API dependencies.

## project structure

- `doc/` — active works in progress (specs, designs)
- `doc/open/` — planned docs not actively being worked on
- `doc/completed/` — implemented docs, prefixed with date stamp (e.g., `20260505-doc-name.md`)
- `doc/habit-ubiquitous-language.md` — DDD domain glossary, canonical term definitions
- `.claude/skills/` — claude code skills for authoring specs and implementing features
- `sched/` — legacy printed schedule generator (Python/ReportLab)

## document types

- **functional spec** (`*-func-spec.md`) — what the feature does, from the user's perspective
- **ux design** (`*-ux-design.md`) — screens, flows, interaction patterns
- **tech spec** (`*-tech-spec.md`) — implementation details, data models, architecture

## document lifecycle

1. docs start in `doc/` while actively being worked on
2. docs not currently being worked on go in `doc/open/`
3. once a doc has been fully implemented, move to `doc/completed/` with a date prefix
   (e.g., `20260505-mvp-func-spec.md`)

## conventions

- comments and commit messages use lowercase English (capitals only for proper names and
  code snippets)
- every new class/object companion goes in its own Scala/Kotlin file, except parts of
  sealed traits/interfaces
- prefer "extract method" refactoring over adding comments
- line limit in markdown files is 100 chars

## the printed checklist

the app replaces the printed weekly checklist on the clipboard. `sched/sched.py` is the
legacy tool that generated the printed PDF. the app needs to be good enough that the
printed sheet is no longer necessary.

## domain language

see `doc/habit-ubiquitous-language.md` for full glossary. key terms:
- **habit** — the persistent definition/template (e.g., "Qigong" with its config)
- **activity** — one recorded instance of doing a habit (the actual accomplishment)
- **agenda** — the forward-looking queue of upcoming activities
- **daily target** — how many activities per day for a given habit

## app design principles

- the phone app replaces the clipboard — show the current habit in focus, with the
  agenda a tap away
- one unified **habit** model — differences are just configuration (daily target, timed
  vs untimed, priority weight)
- daily target controls activities per day (1 for simple habits, 2-3 for work blocks)
- timer and interval chimes are tied to individual habits
- all data is local (Room database)
