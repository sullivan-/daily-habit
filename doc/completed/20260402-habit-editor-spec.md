# Habit Editor

## Overview

A dedicated screen for creating and editing habits. Reached via a menu
button on the progress/agenda bar, or via an edit button on the expanded
activity view.

## Navigation

### Menu Button

A ☰ icon on the left side of the progress bar (main layout) and agenda
bar (review/activity focused layouts). Tapping it opens a dropdown menu
that drops *upward* from the bar. Menu items:

- **New Habit** — opens the habit editor with empty fields.

The rest of the bar (right of the ☰) retains its current tap behavior
(switching layouts).

### Edit Button

In the expanded activity view (activity focused layout), an edit button
opens the habit editor pre-filled with the currently selected habit's
configuration.

## Habit Editor Screen

A full-screen form that replaces the primary screen. Contains fields for
all habit properties:

| Field              | Type                       | Notes                          |
|--------------------|----------------------------|--------------------------------|
| Name               | text                       | required                       |
| Times of day       | list of hours (0-23)       | at least one required          |
| Sort order         | number                     |                                |
| Days active        | day-of-week checkboxes     | at least one required          |
| Daily target       | number                     | default 1                      |
| Daily target mode  | EXACTLY / AT_LEAST toggle  | default AT_LEAST               |
| Timed              | checkbox                   |                                |
| Chime interval     | seconds (number)           | only shown when timed          |
| Threshold minutes  | number                     | only shown when timed          |
| Threshold type     | GOAL / TIME_TO_STOP toggle | only shown when timed+threshold|
| Priority           | dropdown or segmented      | 5 tiers                        |
| Daily texts        | day → text entries         | optional                       |

### Behavior

- **Save button**: validates required fields, writes to database, returns
  to the previous screen. For new habits, inserts a new row. For edits,
  updates the existing row.
- **Back/cancel**: discards all changes and returns to the previous screen.
  If fields have been modified, a confirmation prompt appears ("discard
  changes?").
- **Auto-generated ID**: for new habits, the ID is derived from the name
  (lowercased, spaces replaced with hyphens). If it conflicts with an
  existing ID, a suffix is appended.

### Validation

- Name must not be empty.
- At least one time of day.
- At least one day active.
- Daily target must be >= 1.

Validation errors are shown inline next to the relevant field. The save
button is disabled until all required fields are valid.

## Data Changes

### HabitDao

New methods:

```
@Insert
suspend fun insert(habit: Habit)

@Update
suspend fun update(habit: Habit)
```

The existing `insertAll` with IGNORE remains for config loading.

### Navigation

This is the first feature that adds a second screen. Introduce Compose
Navigation with two routes:

- `main` — the primary screen (agenda/review/activity focused)
- `habit-editor/{habitId}` — edit existing habit
- `habit-editor/new` — create new habit

## Out of Scope

- Delete habit.
- Habit list screen.
- Reordering habits.
