# Streaks UX Design

## Overview

Streaks add abstention duration tracking to the tally list and introduce a
Details screen that replaces the current tally editor. The design touches
two views: the tally list row (revised indicator) and the new Details
screen.

## Tally List Row (revised)

### Current Layout

```
┌─────────────────────────────────────────┐
│  ✎  Alcohol                 7/10  No Yes│
│  ✎  Nicotine                9/10  No Yes│
│  ✎  Sweets                  3/10  No Yes│
└─────────────────────────────────────────┘
```

### New Layout

The edit icon is removed. The N/10 indicator is replaced with the current
streak. The tally name becomes tappable and navigates to the Details
screen.

```
┌─────────────────────────────────────────┐
│  Alcohol          4 year streak   No Yes│
│  Nicotine         43 day streak   No Yes│
│  Sweets                           No Yes│
└─────────────────────────────────────────┘
```

- The streak text uses `bodyMedium` style in green, matching the space
  the N/10 indicator occupied. Green reinforces that a streak is a win.
- When there is no current streak (most recent choice was Yes, or no
  choices at all), the space is blank.
- Tapping the tally name navigates to the Details screen.

### Streak Text Formatting

The streak is displayed as a number, unit, and the word "streak":

| Duration                | Display               |
|-------------------------|-----------------------|
| Under 1 day             | *(not shown in list)* |
| 1–59 days               | "N day streak"        |
| 60 days – 11 months     | "N month streak"      |
| 12+ months              | "N year streak"       |

All values floor — never round up. 89 days is "89 day streak", not
"3 month streak."

## Details Screen

The Details screen is a full screen that replaces the tally editor. It
combines editing with streak and stats display.

### Layout

```
┌─────────────────────────────────────────┐
│  ← Alcohol                       [Save]│
├─────────────────────────────────────────┤
│                                         │
│  Name                                   │
│  ┌─────────────────────────────────┐    │
│  │ Alcohol                         │    │
│  └─────────────────────────────────┘    │
│                                         │
│  Priority                               │
│  ┌─────────────────────────────────┐    │
│  │ Medium                      ▼   │    │
│  └─────────────────────────────────┘    │
│                                         │
│  ── Streak ──                           │
│  4 year streak                          │
│  since Jun 15, 2022                     │
│                                         │
│  ── Choices ──                          │
│  8/10 last 10                           │
│  3/5 today                              │
│                                         │
│  [Record First No]                      │
│                                         │
│  [Delete Tally]                         │
│                                         │
└─────────────────────────────────────────┘
```

### Top Bar

- Back arrow (←) returns to the Choices list.
- Title shows the tally name (reflects edits in real time as the user
  types in the name field).
- Save button on the right, enabled only when the name is non-blank.
  Same behavior as the current editor.

### Editing Fields

Name and priority fields are identical to the current tally editor. Same
styling, same `ControlShape`, same priority dropdown options.

### Streak Section

Appears below the editing fields under a "Streak" section divider.

- Shows the streak duration in green, same format as the list row
  (e.g., "4 year streak").
- Below the duration, shows the precise start date formatted as a
  readable date (e.g., "since Jun 15, 2022").
- If the streak is under 1 day, shows the start time with "today" or
  "yesterday" (e.g., "since 13:54 today") using 24-hour time. No
  duration line.
- If there is no current streak, this section shows "no current streak"
  in muted text.
- If the tally has no choices at all, this section is hidden.

### Choices Section

Appears under a "Choices" section divider.

- **Last 10** — always shown if the tally has any choices. Format:
  "M/N last 10" where M is abstain count and N is total (up to 10).
  Color-coded with the existing red-to-green gradient based on ratio.
- **Today** — shown only when the tally has 3 or more choices today.
  Format: "M/N today". Same color coding.
- If the tally has no choices at all, this section is hidden.

### Record First No

A button that lets the user insert a historical No choice before all
existing choices.

- Always visible on the Details screen, unless there is no valid date
  that precedes all existing choices (essentially never in practice).
- Tapping the button opens a date picker.
- The date picker's maximum selectable date is the day before the
  earliest existing choice for this tally. If the tally has no choices,
  the maximum selectable date is today.
- After selecting a date, a No choice is inserted at that date. The
  streak and stats sections update immediately.

### Delete Button

Same as the current editor — red-styled outlined button at the bottom.
Tapping it shows a confirmation dialog: "Delete [name]? This will delete
the tally and all its choice history. This cannot be undone." Confirm
deletes and navigates back to the Choices list.

### Discard Changes Dialog

If the user has unsaved changes (name or priority modified) and taps the
back button, a dialog appears: "Discard changes?" with "Discard" and
"Keep editing" options. Same behavior as the current editor.

## States

### New Tally (no choices)

The Details screen shows the editing fields. The Streak and Choices
sections are hidden. Record First No is available with dates up to today.

```
┌─────────────────────────────────────────┐
│  ← New Tally                     [Save]│
├─────────────────────────────────────────┤
│                                         │
│  Name                                   │
│  ┌─────────────────────────────────┐    │
│  │                                 │    │
│  └─────────────────────────────────┘    │
│                                         │
│  Priority                               │
│  ┌─────────────────────────────────┐    │
│  │ Medium                      ▼   │    │
│  └─────────────────────────────────┘    │
│                                         │
│  [Record First No]                      │
│                                         │
└─────────────────────────────────────────┘
```

### Tally with Choices, No Current Streak

The most recent choice was a Yes. The streak section shows "no current
streak" in muted text. The choices section shows stats.

```
│  ── Streak ──                           │
│  no current streak                      │
│                                         │
│  ── Choices ──                          │
│  3/10 last 10                           │
```

### Tally with Current Streak, Sub-day

The streak exists but is under 1 day. The streak section shows the
start as a time with "today" or "yesterday" instead of a date.
Uses 24-hour (military) time.

```
│  ── Streak ──                           │
│  since 13:54 today                      │
```

Or if the streak started the previous calendar day:

```
│  ── Streak ──                           │
│  since 5:04 yesterday                   │
```

### Tally with Long Streak via Record First No

After using Record First No to backdate a streak start:

```
│  ── Streak ──                           │
│  4 year streak                          │
│  since Jun 15, 2022                     │
│                                         │
│  ── Choices ──                          │
│  10/10 last 10                          │
```

## Design Decisions

- **Sub-day streaks:** on the Details screen, show "since 13:54 today"
  or "since 5:04 yesterday" using 24-hour time, without a duration
  line. On the list row, show nothing (same as no streak).
- **Streak text color:** always green. A streak is a win.
- **Bottom bar weekly indicator:** remains as-is (weekly abstain/total
  count). Unchanged by this feature.
