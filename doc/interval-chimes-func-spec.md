# Interval Chimes Functional Specification

## Overview

Interval chimes are repeating audio cues that fire at a fixed cadence during a
timed activity. The user picks an interval when starting work, and the chime
repeats until the activity is finished or cancelled. This supports rhythmic
activities (breathing exercises, reps) at short intervals and work-block pacing
at longer ones.

*Motivation:* The existing threshold chimes (goal, stop) fire once. Interval
chimes fill a different role — they provide ongoing rhythm or periodic reminders
throughout a session. The two systems are independent.

## Interval Selection

The interval is **ephemeral state** — it belongs to the active session, not the
habit definition. Each time the user starts chimes, they pick from a fixed list.
Nothing is saved to the habit or the activity record.

### Available Intervals

| Seconds                        | Minutes               |
|--------------------------------|-----------------------|
| 6s, 7s, 8s, 9s, 10s, 11s, 12s  | 3m, 4m, 5m, 8m, 10m   |

*Motivation:* The seconds range (6–12) is for longevity breathing exercises. The
minutes range (3–10) serves focus blocks where the user wants periodic awareness
of time passing. The gap between 12s and 3m is intentional — intervals in
between don't fit either use case well.

## Setting the Interval

The interval selector appears in the expanded activity view for timed habits.
The user picks an interval and starts the chimes. Exact presentation is defined
in the UX design.

### Auto-Start Timer

If the timer is not already running when the user starts chimes, the timer
starts automatically. Starting chimes implies starting the activity.

*Motivation:* Chimes without a running timer make no sense — the user clearly
intends to begin. Requiring a separate tap to start the timer first would be
unnecessary friction.

### Stopping Chimes

Chimes stop when any of the following happens:

- The user **completes** the activity (finish button or checkbox).
- The user **cancels** the timer.
- The user explicitly **stops the chimes** without ending the activity.

The third case lets the user silence chimes mid-session while keeping the timer
running — useful when interrupted or switching context briefly.

## Chime Behavior

### Cadence

The first chime fires immediately. Chimes then repeat at the selected interval
until stopped.

> Example: The user selects 3m and starts chimes at 2:00. Chimes fire at 2:00,
> 2:03, 2:06, and so on.

### Sound

Interval chimes do **not** vibrate. The audio differs by interval type:

- **Seconds intervals** (6s–12s): A short, subtle chime. The sound needs to be
  noticeable without being jarring at rapid repetition.
- **Minutes intervals** (3m–10m): The standard system notification sound, same
  as the existing threshold chimes.

### Background Behavior

When the app is backgrounded with chimes running, they continue via the
foreground service alongside the existing timer notification. The user hears
chimes whether the app is in the foreground or background.

### Independence from Threshold Chimes

Interval chimes and threshold chimes (goal minutes, stop minutes) are completely
independent. Both can fire during the same activity:

- Interval chimes repeat at their cadence.
- Threshold chimes fire once when their time is reached.

If a threshold chime and an interval chime would fire at the same moment, both
fire. No suppression or deduplication.

## Changing the Interval Mid-Session

There is no direct way to change the interval while chimes are running. The
user cancels the current chimes and starts new ones with a different interval.
This is simple and the intent is unambiguous.

## Domain Language

This feature introduces one new term:

- **interval chime** — a repeating audio cue at a fixed cadence during an
  activity session. Contrast with **threshold chime**, which fires once when
  elapsed time crosses a configured boundary.

## Out of Scope

- Persisting the last-used interval per habit (could be a future convenience,
  but keeping it ephemeral is the right starting point).
- Custom intervals beyond the fixed list.
- Visual pulse or animation synchronized to the chime.
- Different chime sounds per interval (all seconds intervals share one
  sound; all minutes intervals share another).
