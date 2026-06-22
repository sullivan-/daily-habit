# Past-day Recording UX Design

## Overview

Past-day recording extends the existing Review layout. The Done view becomes parameterized by a
selected date — today by default — and the user steps between days with a thin date strip at the
top of the activity view. The completed list grows missed rows and an Other... entry when the
selected date is in the past; the agenda bar continues to live at the bottom. No new screens, no
new layouts.

*Motivation:* the back-fill flow is fundamentally a variation on reviewing a day's work, not a new
kind of journey. Keeping it inside the existing Review layout makes the date selector the only
genuinely new visual element.

## Date selector

A persistent strip at the top of the activity view on the Review layout, present on today and on
past days alike. The strip is a row of three controls:

```
◀  Tue Jun 2  ▶
```

- **◀** and **▶** are icon buttons (Material `ChevronLeft` / `ChevronRight`). Each step moves the
  selected date by one day.
- **Date label** is the centered tap target:
  - `Today` — when the selected date is today.
  - `Yesterday` — when the selected date is one day before today.
  - `<weekday> <month> <day>` otherwise (e.g., `Tue Jun 2`, `Fri May 29`). No year, because the
    window never reaches back more than seven days.
- Tapping the date label when the selected date is not today jumps the selector back to today in
  one tap.
- **▶** is disabled (greyed, no ripple) when the selected date is today.
- **◀** is disabled when the selected date is the oldest reachable day (today minus seven).

*Motivation:* keeping the strip persistent — including on today, with a disabled ▶ — makes the
feature discoverable and gives the user a constant date context without a special past-day mode
indicator. The date label doubling as the return-to-today control avoids adding a fourth element
to the strip.

## Review layout — today

Unchanged from current behavior, with the date selector added at the top of the activity view:

```
┌──────────────────────────────┐
│  ◀   Today    ▶              │  ◀ enabled (yesterday), ▶ disabled
│  (backward-looking summary)  │  ← compact activity view
├──────────────────────────────┤
│  ☑ Kegel             7:02a   │
│  ☑ Qigong            7:15a   │  ← completed list, chronological
│  ☑ Badux (1/3)       8:30a   │
│  ...                         │
├──────────────────────────────┤
│  ☐☐☐☐☐  5 incomplete         │  ← agenda bar (menu + count)
└──────────────────────────────┘
```

No missed rows and no Other... entry — adding activities for today still happens on the main
agenda view, which has its own Other... entry.

## Review layout — past day

```
┌──────────────────────────────┐
│  ◀   Tue Jun 2   ▶           │  both chevrons enabled mid-window
│  (summary for Jun 2)         │  ← compact activity view
├──────────────────────────────┤
│  ☑ Kegel             7:02a   │
│  ☑ Qigong            7:15a   │  ← completed activities first,
│  ☑ Prayer            7:48a   │     chronological
│  ─────                       │  ← thin divider
│  ☐ Badux             1/3     │  ← missed rows, greyed
│  ☐ Read                      │
│  ☐ Home                      │
│  Other…                      │  ← Other... entry, bottom of list
├──────────────────────────────┤
│  ☐☐☐☐☐  11 incomplete        │  ← agenda bar (Easy Day entry
└──────────────────────────────┘     hidden on past days)
```

The activity view's collapsed summary reflects the *selected date* — totals, time tracked, etc. —
not today's. The bottom-bar incomplete count is computed against the selected date's raw daily
target sum, per the func spec.

### Missed row

A single greyed-out row per habit that meets the missed-row criteria. Each row shows:

- An empty checkbox icon at the left edge (`☐`), echoing the agenda's checkbox affordance but in
  the muted `onSurfaceVariant` colour.
- The habit name, also in `onSurfaceVariant`.
- The running count out of target right-aligned (e.g., `1/3`). When the daily target is 1 the
  count is omitted, since `0/1` adds nothing — the empty checkbox already conveys "not done."

Tapping anywhere on the row performs a back-fill (see *Back-fill flow* below).

### Divider between sections

A thin horizontal rule separates the completed activities from the missed rows. The Other... row
sits below all missed rows with no extra divider — it is part of the same "what's still possible
on this date" group. If there are no missed rows, the divider is omitted; only completed
activities and Other... show.

### Other... entry

Reuses the existing `Other…` row style from the agenda list — `bodyLarge`, muted colour,
end-of-list. Tap opens the same Other-habit picker the main agenda uses, but the back-fill on
selection targets the selected date rather than today.

## Back-fill flow

Tapping a missed row (or selecting a habit through the Other... picker) does three things in
sequence:

1. A new activity is created with the defaults defined in the func spec.
2. The new activity becomes the selected item in the activity view, which expands into the
   existing completed-activity detail. The user lands in the same detail surface they would reach
   by tapping any completed row.
3. The completed list updates underneath: the new activity joins the completed group; the missed
   row's count updates (or the row disappears if the back-fill brought completions up to the
   habit's daily target).

The user adjusts `completedAt` via the existing time picker, edits the note, picks a
track/milestone — all in the detail — or dismisses to accept the defaults. Dismissing collapses
the activity view back to its summary, and the past-day list is the foreground again.

*Motivation:* the auto-expand turns the common "fix the timestamp before I forget what time it
actually was" pattern into one continuous gesture: tap, edit, dismiss. Defaulting to the
end-of-day timestamp is a clear "this is a default" signal that invites correction without
demanding it.

## Activity detail on a past day

The compact activity view's detail surface is unchanged from today; the only addition is a small
date marker as a subtitle under the habit name when the selected date is not today (e.g.,
`Tue Jun 2`). It uses `bodySmall` in `onSurfaceVariant`. This keeps the user oriented while the
detail occupies the top section and the date strip is the only other date cue on screen.

The existing `Do again` button on a past-day completed activity behaves as a back-fill on the
*same* past date — creating another activity for that day and following the same auto-expand
pattern. This mirrors the spec's principle that any addition to a past day is a back-fill.

## Agenda bar on a past day

The agenda bar persists at the bottom on past days with two small differences from today:

- The Easy Day menu entry and the Easy Day sub-label are both hidden, because Easy Day applies
  only to today and a control that would silently affect today from a past-day context would be
  confusing.
- The `Done Today` menu entry is shown and serves as a backup one-tap return to today (tapping
  it snaps the selector to today the same way tapping the date label does).

All other menu entries — `New habit`, `Habit list`, `Choices`, `Day Plan` — behave as they do
today. `Day Plan` returns to the main agenda, which is always today.

The incomplete count in the centre of the bar reflects the selected date.

## Selection on date change

Stepping to a new date via either chevron, or jumping to today via the date label, clears any
selected activity. The activity view collapses back to its summary on the new date. The user is
never left looking at "yesterday's activity detail on top of today's list."

## Day boundary crossing

If the day boundary crosses while the view is open, the calendar date the user is looking at
stays the same; only its label shifts (e.g., `Today` becomes `Yesterday`). If the now-stale
selection falls out of the eight-position window after the shift, the selector snaps to the new
today and the activity view collapses.

## States

### At today, anything done

`◀` enabled, `▶` disabled. Identical to current Review behaviour aside from the strip itself.

### At a past day with completions and missed rows

The mixed list shown in the wireframe above.

### At a past day with no missed rows

Every habit active that weekday hit its daily target. The list shows the completed activities
followed by Other...; no divider, no missed rows.

### At a past day with no completed activities

For example, a day on which the user never opened the app. The list shows the missed rows (if
any habits were active that weekday) and Other..., with no completed group above. If there are
also no missed rows — a day with no active habits at all, like a barren weekend — the list shows
only Other... and the activity-view summary reads `Nothing recorded on <date>`.

### At the oldest reachable day

`◀` disabled, `▶` enabled. Tapping the date label still jumps to today.

### Activity selected mid-past-day

The activity view expands into detail with the past-day subtitle. The date strip remains visible
above the detail. Dismissing detail collapses back to the summary; the date selector did not move
and the underlying list is unchanged.

## Design decisions

- **Persistent date strip on today as well.** Costs one row of vertical space in exchange for
  discoverability and a uniform structure. Hiding the strip on today would have meant the
  feature was reachable only through gesture or menu, which the func spec's chevron framing
  argues against.
- **Date label doubles as return-to-today.** Avoids a fourth strip element. The behaviour is
  consistent with menu's `Done Today` entry, which is the backup path.
- **Easy Day controls suppressed on past days, not disabled.** A disabled Easy Day entry would
  invite the question "what would it do?" — hiding it removes the question.
- **Greyed missed row with empty checkbox.** Visually distinct from the strongly-rendered
  completed-row check, but uses the same row format and column alignment, so the list reads as
  one continuous stream of "the day's habit work."
- **Date marker under the habit name in detail.** Cheap insurance against the user losing track
  of which day's data they are editing once the activity view fills the top.

## Out of scope

- Calendar or date-picker UI for jumping to an arbitrary date.
- Past-day navigation on the main (agenda) layout — the agenda stays fixed on today.
- Swipe-to-change-date gestures on the list area. Could be added later without disturbing this
  design.
- Visual styling of the disabled-chevron state beyond "muted, no ripple" — leave the exact
  treatment to the implementation against current Material 3 conventions.
