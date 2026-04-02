# Activity History Swipe

## Overview

When viewing a habit in the activity focused (expanded) layout, the user can
swipe left and right to browse the complete history of activities for that
habit. This lets the user review past notes, durations, and timestamps
without leaving the current screen.

## Expand/Collapse Control

The "More detail" / "Less detail" text button is replaced with an icon
button in the top-right corner of the activity view:

- **UnfoldMore** icon: expands to activity focused layout.
- **UnfoldLess** icon: collapses back to normal activity view.

The icon is always visible when a habit is selected, in both the normal
and expanded layouts. This replaces the text button that previously sat
in the middle of the activity content.

## Back to Current

When browsing history in the expanded layout, a "back to current" control
appears to return to today's in-progress or most recent activity. This
could be a button or a swipe-to-end gesture — the important thing is that
the user can get back without tapping the agenda bar and re-selecting the
habit.

*Implementation note:* a simple text button "Back to today" or a
right-pointing chevron at the edge of the swipe area. Exact form TBD
during implementation.

## Entry Point

The swipe feature is only available in the **activity focused layout**
(reached by tapping the UnfoldMore icon). The normal activity view (top
section in main layout) remains focused on today's activity and does not
support swiping.

## Swipe Behavior

- **Swipe left**: navigate to the previous (older) activity for this habit.
- **Swipe right**: navigate to the next (newer) activity.
- Activities are ordered chronologically by completion time.
- The history includes all completed activities for the habit, across all
  dates, with no limit.
- Today's in-progress activity (if any) is the newest item in the
  sequence.

### Boundary Behavior

- **At the newest activity** (today's current): swiping right shows a
  bump animation indicating there's nothing newer.
- **At the oldest activity** (first ever recorded): swiping left shows the
  same bump animation.

## Display

When viewing a historical activity, the activity view shows:

- Habit name
- Attributed date
- Completion time
- Duration (for timed habits)
- Note field (**editable** — the user can update past notes)

Timer controls (start/stop) are **not shown** for completed activities.
Editing start/stop times is a future feature with dedicated datetime
editors.

## Data Requirements

A new DAO query to fetch all completed activities for a given habit,
ordered chronologically:

```
SELECT * FROM activity
WHERE habitId = :habitId AND completedAt IS NOT NULL
ORDER BY completedAt ASC
```

Today's in-progress activity (completedAt IS NULL) is appended as the
last item when present.

## Navigation

- Swiping through history does not change the current layout — the user
  remains in the activity focused layout.
- Tapping the agenda bar returns to the main layout with today's state,
  regardless of which historical activity was being viewed.
- Selecting a different habit (from the agenda) exits history browsing.

## Out of Scope

- Editing activity start/end times (future feature with datetime editors).
- Filtering or searching history.
- Viewing history from the normal (non-expanded) activity view.
