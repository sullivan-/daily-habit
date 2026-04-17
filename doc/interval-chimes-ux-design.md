# Interval Chimes UX Design

## Overview

Interval chimes add a repeating audio cue during timed activities. The UI
is a progressive disclosure element in the activity view: a single button
that expands into an interval picker, then collapses into a running
countdown. It appears in both compact and expanded views. The design
keeps the chime controls out of the way for activities that don't use
them.

## Placement

The interval chime element appears between the timer controls and the
start time display (editable times). It is only visible for timed habits.
It does not appear on completed activities.

```
┌─────────────────────────────────────────┐
│  Qigong                    1/2   ⊟   ☐ │
│  ┌─────────────────────────────────┐    │
│  │ Foundational Qigong         ▼   │    │
│  └─────────────────────────────────┘    │
│  ☐ lesson 3                         ▼   │
│  ▶ 12:34         [Cancel] [Finish]      │
│                        [Interval Chime] │  ← chime element
│  started 11:02 AM                       │
│  [note field                      ]     │
│  [Skip] [Delete] [Edit Habit]           │
└─────────────────────────────────────────┘
```

## Three States

The interval chime element has three states: idle, selecting, and running.
Only one state is visible at a time. Transitions between states are
immediate — no animation.

### Idle

A single right-justified button labeled "Interval Chime." This is the
default state for any timed activity that doesn't have chimes running.

```
│                        [Interval Chime] │
```

Tapping the button transitions to the selecting state.

### Selecting

The button is replaced by two rows of interval chips and a close button.
The seconds row appears first, then the minutes row below it. A close
button sits right-justified below the rows.

```
│  6s  7s  8s  9s  10s  11s  12s          │
│  3m  4m  5m  8m  10m                    │
│                                [Close]  │
```

Each chip is a tappable button showing the interval value. The chips are
laid out in a horizontal row with even spacing. The visual gap between the
two rows reinforces that they serve different use cases (breathing rhythm
vs. work pacing).

- **Tapping a chip** starts chimes at that interval and transitions to
  the running state. If the timer is not already running, it starts
  automatically (per the func spec).
- **Tapping Close** returns to the idle state without starting chimes.

### Running

The selection rows are replaced by a single line showing the active
interval and a countdown to the next chime, with a cancel button.

```
│  every 8s — next in 0:04       [Cancel] │
```

The countdown updates in real time, counting down from the interval
duration to zero. When it reaches zero the chime fires and the countdown
resets to the full interval.

- **Seconds intervals** display as a single number (e.g., "next in 4").
- **Minutes intervals** display as minutes and seconds
  (e.g., "next in 2:47").

Tapping **Cancel** stops the chimes and returns to the idle state. The
timer keeps running — only the chimes stop.

## Compact and Expanded Views

The interval chime element appears in the same position — between the
timer and the start time — in both compact and expanded views. All
three states (idle, selecting, running) are accessible from either
view.

*Note:* the activity view currently shows nearly the same content in
compact and expanded views. A broader review of what belongs in each
view (and how that differs for active vs. completed activities) is
warranted but out of scope for this design.

## Interaction with Timer

Starting chimes auto-starts the timer if it isn't already running. This
means tapping a chip in the selecting state can trigger two actions at
once: starting the timer and starting chimes.

When the timer is cancelled, chimes stop as well (per the func spec).
When the activity is completed, chimes stop. In both cases the chime
element returns to idle — but since completed activities don't show the
chime element at all, the user only sees the return to idle on cancel.

## Changing the Interval

There is no direct way to change the interval while chimes are running.
The user taps Cancel to stop the current chimes (returning to idle),
then taps "Interval Chime" to pick a new interval. Two taps, clear
intent.

## States Summary

| Condition                          | Chime element          |
|------------------------------------|------------------------|
| Timed habit, no chimes running     | Idle (button)          |
| User tapped "Interval Chime"       | Selecting (chip rows)  |
| User picked an interval            | Running (countdown)    |
| User tapped Cancel while running   | Idle (button)          |
| User tapped Close while selecting  | Idle (button)          |
| Activity completed                 | Element hidden         |
| Activity cancelled                 | Element hidden         |
| Untimed habit                      | Element not shown      |
| Compact view, chimes running       | Running (countdown)    |
