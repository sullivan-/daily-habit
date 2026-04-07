# Habit — UI Style Guide

reference for visual consistency across all screens. when adding or modifying UI elements,
follow these conventions.

## theme

- dark color scheme (Material 3 `darkColorScheme()` defaults)
- no custom color palette — use the built-in Material 3 dark tokens

## buttons

all buttons (filled, outlined, text) use `elevation = buttonElevation()` for a subtle
shadow (2dp default, 4dp pressed). this gives depth and separates them from the surface.

### primary actions (filled Button)

use for the main action the user is most likely to take: Start, Finish, Save, Skip, Edit.
filled background with on-primary text. this is the default button style.

examples: Start timer, Finish timer, Save habit, Skip activity, Edit habit

### secondary actions (TextButton)

use for contextual or less-prominent actions: Again, Back to start, dialog options
(Cancel, Discard, Keep editing, Delete confirm).

examples: Again, Back to start, Cancel (in timer), dialog buttons

### destructive actions (OutlinedButton, error color)

use for delete operations. error-colored border and text on transparent background.

examples: Delete Habit, Delete Tally

### choice recording (OutlinedButton)

use for the Yes/No buttons on the choices screen. outlined style distinguishes them from
primary actions since they're repeated per row.

## form controls

### shape

all form controls use a consistent rounded bevel matching the FilterChip default
(`RoundedCornerShape(8.dp)`):

- **text fields** — `OutlinedTextField` with `RoundedCornerShape(8.dp)`
- **dropdowns** — `ExposedDropdownMenuBox` backed by an `OutlinedTextField` with the same shape
- **segmented buttons** — use `segmentedShape(index, count)` so the group reads as one
  continuous control: 8dp rounding on the outer ends only, flat inner edges where buttons meet
- **filter chips** — days active (S M T W T F S) — default Material 3 FilterChip shape
- **input chips** — times of day (7:00, 14:00) — default Material 3 InputChip shape

### field groups (FieldGroup composable)

on editor screens, related fields are grouped inside a bordered box with a legend-style
title overlapping the top border. groups use `RoundedCornerShape(8.dp)` and
`outlineVariant` border color.

current groups on Edit Habit: Schedule, Importance, Tracking, Daily texts.

## status bars

all three bottom bars (ProgressBar, AgendaBar, ChoicesBar) share the same structure:

- 48dp tall, full width
- menu button on the left
- status text centered
- optional action button on the right (+ on ChoicesBar)
- horizontal swipe to navigate between views (Main → Review → Choices)

ProgressBar uses a gradient background; AgendaBar and ChoicesBar use `Color.DarkGray`.

## icon buttons

use `IconButton` for compact icon-only actions: menu (hamburger), back (arrow), expand/collapse
(unfold), edit (pencil on choices rows), add (+ FAB or + in bar).

## layout

- screens use `Scaffold` with either a `TopAppBar` or a `bottomBar`
- editor screens use TopAppBar with back button + Save action
- list screens use TopAppBar with back button + FAB for new items
- the primary screen and choices screen use bottom bars instead of top bars
