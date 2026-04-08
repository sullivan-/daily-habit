# Tracks UX Design

## Overview

Tracks add sub-category selection to the activity view and track management to the habit editor.
The design touches two screens: the primary screen (activity view) and the habit editor.

## Activity View: Track Selection

### Track Dropdown

When a habit has tracks configured, a dropdown appears between the habit name row and the timer
(or note field for untimed habits). The dropdown shows the currently selected track name, or
a placeholder "Select track..." if none is selected.

```
┌─────────────────────────────────────────┐
│  Badux                    1/2   ⊟   ☐  │
│  ┌─────────────────────────────────┐    │
│  │ baduxgo server              ▼   │    │
│  └─────────────────────────────────┘    │
│  ▶ 12:34         [Cancel] [Finish]      │
│  started 11:02 AM                       │
│  [note field                      ]     │
│  [Skip] [Delete] [Edit Habit]           │
└─────────────────────────────────────────┘
```

For habits with no tracks, the dropdown does not appear — nothing changes from the current
layout.

*motivation:* placing the dropdown above the timer keeps it visible in the compact view. the user
sees the track selection immediately when starting an activity, without needing to expand.

### Dropdown Behavior

- Tapping the dropdown opens a scrollable list of non-archived tracks, ordered by suggestion
  priority (day-of-week defaults first, then by priority).
- Tapping a track selects it and closes the list.
- The track selection is saved on the activity record. Changing the track on an active or
  completed activity updates the record.
- The dropdown uses the same `ControlShape` (8dp rounded) as other form controls.

### Current Milestone Display

When the selected track has an ordered series, a milestone selector appears directly below the
track dropdown:

```
│  ┌─────────────────────────────────┐    │
│  │ Foundational Qigong         ▼   │    │
│  └─────────────────────────────────┘    │
│  ☐ lesson 3                         ▼   │
```

- Defaults to the first incomplete milestone in the series.
- Tapping the milestone opens a selection list of all remaining incomplete milestones, so the
  user can pick a different one. the series order is a suggestion, not a constraint.
- Tapping the checkbox marks the selected milestone complete. the selector advances to the next
  incomplete milestone.
- For tracks without a series, the milestone selector does not appear.
- On completed activities in history view, the milestone shows as checked if it was completed
  during that activity.

### Milestone Editing on Completed Activities

When browsing activity history, the milestone checkbox reflects what happened during that
activity. The user can check or uncheck it freely.

### Empty State

When a habit has tracks but none are selected for the current activity, the dropdown shows
"Select track..." in a muted style. The activity can still be completed without selecting a
track — track selection is not required.

## Habit Editor: Track Management

### Tracks Field Group

A new **Tracks** field group appears in the habit editor, between the Timekeeping group (renamed
from Tracking) and the Delete button. The Daily texts group is removed — day-of-week defaults on
tracks replace it.

Field group order on the habit editor:

1. Name
2. Schedule
3. Priority
4. Timekeeping (was Tracking)
5. **Tracks**
6. Delete Habit

### Tracks List

The Tracks group shows the habit's tracks as a vertical list. Each track row shows:

- Track name
- Day-of-week default badge (e.g., "Mon") if configured, otherwise blank
- An archive/un-archive toggle icon

```
┌──────────────── Tracks ─────────────────┐
│  baduxgo server                         │
│  baduxgo web                            │
│  baduxgo bot                            │
│                                         │
│  [+ Add track]                          │
│                                         │
│  ── Archived ──                         │
│  old-project                      ↩     │
└─────────────────────────────────────────┘
```

- Active tracks appear at the top. Archived tracks appear at the bottom under a dim "Archived"
  divider, with an un-archive icon.
- Tapping a track row expands it inline for editing.
- Tapping "+ Add track" opens an inline form at the bottom of the active list.

### Inline Track Editor

When a track is expanded for editing, it shows:

```
│  ┌ baduxgo server ──────────────────┐   │
│  │ Name: [baduxgo server        ]   │   │
│  │ Day:  [None ▼]                   │   │
│  │ Priority: [Medium ▼]            │   │
│  │ Series: (none)     [+ Add]       │   │
│  │              [Archive] [Done]    │   │
│  └──────────────────────────────────┘   │
```

- **Name** — text field, uses `ControlShape`.
- **Day** — dropdown with None + the seven days of the week.
- **Priority** — dropdown matching the habit priority selector style.
- **Series** — if the track has milestones, they appear as an ordered list with checkboxes. A
  "+ Add" button appends a new milestone to the end. Individual milestones can be deleted if no
  activity references them. If no series exists, shows "(none)" with a "+ Add" button to start
  one.
- **Archive / Delete** — if the track has no associated activities, a "Delete" button appears
  (permanently removes the track). If the track has associated activities, an "Archive" button
  appears instead (soft removal from the suggestion list, preserves history). For archived
  tracks, this button reads "Un-archive."
- **Done** — collapses the inline editor.

### Series Milestone Editor

When "+ Add" is tapped on the series, a text field appears for entering the milestone name.
Pressing enter or tapping a confirm button adds it to the end of the list.

```
│  │ Series:                          │   │
│  │   1. ☑ lesson 1                  │   │
│  │   2. ☑ practice 1                │   │
│  │   3. ☐ lesson 2                  │   │
│  │   4. ☐ practice 2                │   │
│  │   [new milestone name    ] [+]   │   │
│  └──────────────────────────────────┘   │
```

Completed milestones show checked. The series is numbered for clarity.

Individual milestones can be deleted if no activity references them (the user is still setting
up). Once an activity references a milestone, it becomes permanent (deletable only via deleting
the parent track or habit).

## History View

When swiping through activity history, the track name appears below the habit name if a track
was selected for that activity. If the track had a series, the milestone for that activity
appears with its completion state.

```
┌─────────────────────────────────────────┐
│  Qigong Training             1/1   ⊟   │
│  Tue, Apr 7                             │
│  Foundational Qigong                    │
│  ☑ lesson 2                             │
│  started 9:05 AM                        │
│  completed 9:38 AM                      │
│  duration: 33m                          │
│  [note field                      ]     │
│  [Again] [Back to start] [Edit Habit]   │
└─────────────────────────────────────────┘
```

## Completed List

In the review layout's completed list, the track name appears as a secondary line below the
habit name if a track was selected:

```
│  ☑ Badux (1/2)       11:02a            │
│    baduxgo server                       │
│  ☑ Read              1:15p             │
│  ☑ Badux (2/2)       3:10p            │
│    baduxgo bot                          │
```

## States

### Habit with No Tracks

No change from current behavior. The track dropdown does not appear. The activity view looks
exactly as it does today.

### Habit with Tracks, No Selection

The dropdown shows "Select track..." in muted text. The user can complete the activity without
selecting a track.

### All Tracks Archived

The dropdown shows "Select track..." but when opened, the list is empty with a message: "all
tracks archived." The user can still complete the activity without a track, or go to the habit
editor to un-archive or create tracks.

### Series Track, All Milestones Complete

The milestone line shows the last completed milestone as checked. No new milestone appears. The
user may want to archive the track at this point, but the app does not prompt — it's a manual
action.

## Ubiquitous Language Updates

When this design is finalized, update `doc/ubiquitous-language.md` with:

- **Track** — new term in the core domain (sub-category of a habit)
- **Milestone** — new term (ordered item within a track series)
- **Archival** — new term (soft removal of a track from the suggestion list)
- **Daily Text** — remove (replaced by day-of-week default tracks)
- **Timekeeping** — rename of the "Tracking" field group in the habit editor
- Update the Activity definition to note optional track association
