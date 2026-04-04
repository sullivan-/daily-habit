# MVP UX Design

## Overview

The app has a single primary screen with three layouts. The user switches
between layouts by tapping compact bar elements. All three layouts share the
same **activity view** at the top, which adapts its content based on context.

## Layouts

The primary screen is composed of three elements stacked vertically:

1. **Activity view** (top) — shows the focused habit or activity when one is
   selected. Collapses to a compact summary when nothing is selected.
2. **Middle section** — either the agenda or the completed list, depending
   on the layout.
3. **Bar** (bottom) — either the progress bar or the agenda bar, depending
   on the layout.

The three layouts are:

| Layout               | Activity view             | Middle section     | Bar           |
|----------------------|---------------------------|--------------------|---------------|
| **Main**             | Next habit (or summary)   | Agenda items       | Progress bar  |
| **Review**           | Selected item (or summary)| Completed items    | Agenda bar    |
| **Activity focused** | Expanded detail           | (more detail)      | Agenda bar    |

### Main Layout

The default, forward-looking layout. Shows what's coming next.

```
┌─────────────────────────┐
│  activity view          │  ← next habit, or forward-looking summary
│                         │     if nothing is selected
│                         │
├─────────────────────────┤
│  agenda                 │  ← upcoming activities, scrollable
│  ☐ Vitamins             │
│  ☐ Badux (1/3)          │
│  ☐ Read                 │
│  ☐ Home                 │
│  ...                    │
├─────────────────────────┤
│  ■■■■■■■░░░░ 9/14       │  ← progress bar (tap to switch to review)
└─────────────────────────┘
```

### Review Layout

The backward-looking layout. Shows what's been accomplished today.

```
┌─────────────────────────┐
│  activity view          │  ← selected completed item, or
│                         │     backward-looking summary
│                         │
├─────────────────────────┤
│  completed              │  ← completed activities, scrollable
│  ☑ Kegel         7:02a  │
│  ☑ Qigong        7:15a  │
│  ☑ Prayer        7:48a  │
│  ☑ Badux (1/3)   8:30a  │
│  ...                    │
├─────────────────────────┤
│  ☐☐☐☐☐ 5 remaining      │  ← agenda bar (tap to switch to main)
└─────────────────────────┘
```

### Activity Focused Layout

Expanded view of a single activity. The activity view takes most of the
screen, with additional detail below it.

```
┌─────────────────────────┐
│  activity view          │
│  Qigong (1/2)           │
│  ▶ 12:34 elapsed        │
│  ♪ every 10s            │
│  goal: 30:00            │
│                         │
│  [note: form 3 review   │
│   standing postures]     │
│                         │
├─────────────────────────┤
│  (additional detail)    │
│  previous activities    │
│  for this habit today   │
│                         │
├─────────────────────────┤
│  ☐☐☐☐☐ 5 remaining     │  ← agenda bar (tap to switch to main)
└─────────────────────────┘
```

## Navigation

### Switching Between Layouts

- **Progress bar → review**: Tap the progress bar to switch from main to
  review layout. The completed list replaces the agenda in the middle
  section.
- **Agenda bar → main**: Tap the agenda bar to switch from review (or
  activity focused) back to main layout.
- The app remembers which layout the user was in when returning from
  background or notification tap.

### Selecting Items

- **Tap an agenda item** (main layout): That habit becomes the focus in the
  activity view. If there's a running timer on another habit, the user is
  prompted to confirm before switching (stop the timer and save, or cancel).
- **Tap a completed item** (review layout): That completed activity appears
  in the activity view for review and editing.
- **Expand to activity focused**: A dedicated expand button (▼▼▼) at the bottom
  edge of the activity view. Tapping it expands the activity view to take most
  of the screen (activity focused layout). Tapping the agenda bar returns to the
  main layout.

## Activity View

The activity view is the top element in all three layouts. Its content
adapts based on what's selected and which layout is active.

### Showing a Habit (Main Layout, Active)

When a habit is selected from the agenda:

For **timed** habits:
- Habit name
- Activity progress (e.g., "2/3" if daily target > 1)
- Elapsed timer display (accumulated running time, excluding pauses)
- Start/stop button: first tap marks activity start time and begins the
  timer. Stop pauses the timer but keeps the activity open. Subsequent
  start taps resume the timer.
- Done button: finalizes the activity and records the end time. Can be
  tapped while running (acts as stop + done) or while paused.
- Chime interval indicator
- Threshold progress indicator
- Note field for free-text entry (saves immediately on edit). Pre-filled
  with daily text if configured for this day.

For **untimed** habits:
- Habit name
- Activity progress (e.g., "2/3" if daily target > 1)
- Checkbox to mark the activity complete
- Note field for free-text entry (saves immediately on edit). Pre-filled
  with daily text if configured for this day.

### Showing a Completed Activity (Review Layout)

When a completed activity is selected from the completed list:
- Habit name
- Timestamp and duration (if timed)
- Note field (editable — the user can update notes after completion)
- A "do again" action to start a new activity for the same habit. This is
  the path for extra activities beyond the daily target. Tapping it switches
  to the main layout with the habit loaded in the activity view, ready to
  start.

### No Selection (Collapsed)

When no specific habit or activity is selected, the activity view collapses
to a compact summary, giving more vertical space to the agenda or completed
list below. The summary adapts to the current layout:

- **Main layout**: Brief forward-looking summary (e.g., remaining count,
  next time slot).
- **Review layout**: Brief backward-looking summary (e.g., total completed,
  total time tracked).

Tapping any item in the list below expands the activity view back to full
size with that item focused.

*Note:* The exact content and visual design of the collapsed summary is TBD.

## Agenda

A forward-looking, scrollable list of upcoming activities. Appears as the
middle section in the main layout.

- Shows only incomplete items — completed activities leave the agenda.
- For habits with daily target > 1, only the next pending activity appears;
  the following activity enters the agenda once the current one is completed.
- Sorted by display ordering (see func spec).

Each agenda entry shows:
- Habit name
- Activity progress (e.g., "1/3") if daily target > 1
- Brief note preview (if any)

## Completed List

A backward-looking, scrollable list of today's completed activities. Appears
as the middle section in the review layout.

- Shows all activities completed today, in chronological order.
- Each entry shows: habit name, timestamp, duration (if timed), note preview.
- Totals at the top or bottom: activities completed vs total target, total
  time tracked.

## Progress Bar

A compact bar at the bottom of the main layout. Shows daily progress at a
glance. Tapping it switches to the review layout.

- Displays count: "9/14 activities complete."
- Color-coded to indicate status relative to time of day:
  - **Blue**: on track — progress is roughly where it should be.
  - **Green**: going well — ahead of pace.
  - **Red**: falling behind — fewer activities completed than expected by now.
- May include a visual fill element (the bar itself fills proportionally).

## Agenda Bar

A compact bar at the bottom of the review and activity focused layouts.
Shows remaining work at a glance. Tapping it switches to the main layout.

- Displays count of remaining activities.
- Provides a quick way to return to the forward-looking view.

## Background Timer

When the app is backgrounded with a running timer:
- The timer and chimes continue via a foreground service.
- A persistent notification shows the habit name and elapsed time.
- Tapping the notification returns to the app in the same layout state.

## First Launch

On first launch with no recorded activities, the app opens in the main
layout. The agenda is populated from the habit definitions in the static
JSON. The activity view shows the forward-looking summary. The progress bar
shows "0/N activities complete."
