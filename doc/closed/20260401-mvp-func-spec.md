# MVP Functional Spec

## Overview

A native Android app that replaces the printed daily habit checklist. The user opens the
app and sees today's agenda, sorted by time of day. They work through items one at a
time, optionally tracking time, and check them off with brief notes.

## Habit Model

Every item on the agenda is a **habit**. All habits share the same model — the
differences are just configuration.

### Habit Properties

- **name**: Display name (e.g., "Qigong", "Home", "Badux").
- **time of day**: The hour this habit is typically performed (e.g., 7, 8, 14).
  Used for sort order of first activities.
- **sort order**: Ordering among habits with the same time of day. Lower values
  appear first.
- **days active**: Which days of the week this habit appears (e.g., Badux is
  Mon-Fri, Qigong is every day).
- **daily target**: Number of activities to aim for today (default: 1), with a
  mode of either **exactly** or **at least**:
  - **exactly N**: The user should do this habit exactly N times. No "do again"
    option is offered after reaching the target (e.g., Vitamins: exactly 1).
  - **at least N**: The user should do this habit N or more times. Extra
    activities can be started via "do again" in the review layout (e.g.,
    Badux: at least 3, Home: at least 1).
- **timed**: Whether this habit has a time component at all. Some habits are
  simple checkboxes (e.g., "took my vitamins") with no timer. Others are timed
  (e.g., Qigong, Badux, Read).
- **timer settings** (only for timed habits):
  - Chime interval (e.g., every 10 seconds, every 5 minutes)
  - Time threshold and threshold type:
    - "minimum done" — gentle chime at 15 min meaning "you've done enough"
    - "time to stop" — firm chime at 50 min meaning "wrap up"
    - "goal met" — celebratory chime at 30 min meaning "you hit your target"
- **priority**: The importance of this habit. Five tiers: high, medium-high,
  medium, medium-low, low. Affects both agenda ordering (higher priority
  habits appear earlier at the same activity level) and progress bar color
  calculation (missing a low-priority habit has less impact on daily status
  than missing a high-priority one). Within the same priority tier, sort
  order breaks ties.
- **daily text** (optional): Day-specific default text that pre-fills the note
  field (e.g., Body Care: Monday = "sinus rinse", Wednesday = "cholesterol").
  The user can edit or replace this before checking off the activity.

### How Daily Target Works

The daily target controls how the habit behaves after an activity is completed:

- **target = 1** (e.g., Kegel): Check it off, it's done for the day.
- **target > 1** (e.g., Qigong x2, Badux x3): After completing activity 1, the
  habit reappears for activity 2. Shows progress like "1/2".

Once all target activities are complete, the habit leaves the agenda. For
habits with an "at least" target, the user can start extra activities by
finding the habit in the completed list (review layout) and using "do again."
For habits with an "exactly" target, no further activities are allowed.

## Display Ordering

*Note:* The display ordering and progress bar color algorithm are intentionally
loosely defined. The initial implementation should be simple (e.g., compare
completed activities to the number of activities whose time slot is at or before
the current time) and refined based on real use.

The agenda is sorted to surface what the user should do next. The ordering
considers:

- **Time of day** (first activities only): Each habit has an hour (e.g., 7, 8,
  14). First activities are sorted by hour, then by sort order within the same
  hour.
- **Activity number**: First activities of all habits generally sort above second
  activities of any habit. Do everything once before repeating anything.
- **Priority** (subsequent activities): When multiple habits have remaining
  activities at the same level (e.g., several second activities), priority
  determines the order. A second high-priority activity ranks above a second
  medium-priority activity. Sort order breaks ties within the same tier.

The result: open the app at any point in the day and the top of the list is a
reasonable suggestion for what to do next. The user can always tap any item to
override.

## Daily Interaction

The primary screen has three layouts (main, review, activity focused)
described in detail in `mvp-ux-design.md`. The key behavioral rules are:

### Agenda Behavior

The agenda is a forward-looking queue of upcoming activities. It shows only
incomplete items — completed activities move to the completed list. For
habits with a daily target > 1, only the next pending activity appears in
the queue; subsequent activities appear after the current one is completed.

### Timer Behavior

For timed habits, the timer supports pause/resume:
- First start tap marks the activity start time and begins the timer.
- Stop pauses the timer but keeps the activity open.
- Subsequent start taps resume the timer.
- Done finalizes the activity and records the end time (can be tapped while
  running or while paused).
- Elapsed duration = accumulated running time, excluding pauses.

When the app is backgrounded with a running timer, the timer and chimes
continue via a foreground service with a persistent notification.

If the user tries to switch to a different habit while a timer is running,
they are prompted to confirm (stop and save, or cancel).

### Completing an Activity

When the user completes an activity (tapping done for timed, checkbox for
untimed):
- The activity is recorded with a timestamp and the note.
- If activities remain (current < daily target), the habit stays in the
  agenda, ready for the next activity.
- If all target activities are complete, the habit is done for the day.
- Focus advances to the next incomplete item.

### Notes

- The note field is always editable whether the activity is unstarted,
  active, or complete.
- Daily text (if configured) pre-fills the note field as an editable default.
- Notes save immediately on edit.

## Configuration

For the MVP, habit definitions are loaded from a static JSON file bundled with
the app. This file defines all habits and their properties (name, time of day,
days active, daily target, timed, timer settings, priority, priority weight,
daily text). Changes to the habit list require editing the JSON and rebuilding
the app.

The initial JSON will be built based on the current `sched/schedule.pdf` habit
list, adapted to the app's habit model.

*Motivation:* This avoids building configuration UI for the MVP. The user (who
is also the developer) can iterate on the habit list by editing a file, which
is fast and flexible during early use. In-app editing is a future enhancement.

## Day Boundary

The app's concept of "today" does not follow midnight. A **day boundary hour**
(default: 2:00 AM) defines when one day ends and the next begins. Activities
completed before the day boundary are attributed to the previous calendar date.
This handles the not uncommon case of being up past midnight.

The day boundary hour is configured in the static JSON alongside habit
definitions.

## Data Persistence

- All data stored locally on device (Room database).
- Daily entries persist indefinitely.
- Activities record both a timestamp and an attributed date (which may differ
  when the activity falls before the day boundary).
- No cloud sync, no accounts, no network access required.
