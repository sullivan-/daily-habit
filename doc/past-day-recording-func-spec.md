# Past-day Recording Functional Specification

## Overview

Lets the user navigate the Done view back to recent past days and record activities
they did but forgot to log. The Done view, which today shows only today's completed
list, gains a date selector and an "audit" mode for the last seven days. From any past
day in that window, the user can back-fill missed activities or add extras via the
existing "Other..." path.

*Motivation:* there is currently no way to record an activity for a day other than
today. Users miss logging things — a habit completed late at night, a vitamin taken
in the morning before opening the app, an extra block of work done over the weekend.
Forcing the user to remember everything in real time, or lose the record, undermines
the app's role as a faithful daily ledger.

## Scope of past-day navigation

The user can navigate from today back through the previous seven days, inclusive.
Beyond seven days the view does not navigate. Days older than seven days are
read-only via existing history surfaces (per-habit activity history) and are not
addressed by this spec.

*Motivation:* seven days covers the realistic "I forgot for a few days" case
(including a forgotten weekend) without inviting edits to ancient data. It also
keeps the UI to chevrons rather than a full date picker.

## The Done view, parameterized by date

The Done view (the Review layout — completed list plus agenda bar) is generalized
to display any single attributed date in the seven-day window. Its content is
defined entirely by that selected date.

### When the selected date is today

Behavior is unchanged from current: today's completed activities appear in
chronological order, the Other... entry is available for adding habits not on the
agenda, and the bottom bar shows remaining count for today.

### When the selected date is a past day

The screen contents are:

1. **Completed activities for that date** — every activity with attributedDate
   equal to the selected day, in chronological order. Same row presentation as
   today's completed list. Tapping a row opens the activity in detail and supports
   the existing edit / delete / note / time-picker actions.

2. **Missed rows** — for each habit that was active that weekday, had completions
   below its daily target on that date, and was not filtered out by an Easy Day
   level set for that date, a single greyed-out row. The row shows the habit name
   and the running count out of target on that date (e.g., "Badux 1/3" or
   "Reading 0/1"). Tapping records one new activity on that date (see Back-fill
   action below).

3. **Other... entry** — opens the same "Other" picker used today, but scoped to
   the selected date. The picker lists every habit not already represented as a
   missed row, including: habits already at or above target on that date, habits
   not active on that weekday, and habits filtered out by Easy Day for that date.
   Selecting a habit performs a back-fill in the same way as tapping a missed row.

The bottom bar shows remaining count computed against the selected date's
expected target.

*Motivation:* the missed rows handle the common forgot-something case directly
and discoverably. Anything else — extras, days where the habit wasn't expected,
back-filling something that was Easy-Day'd off — is still reachable through
Other..., which already exists today as the escape hatch for adding any habit
to the day.

## Back-fill action

When the user taps a missed row (or picks a habit through Other... on a past
day), a new activity is created with:

- `habitId` = the selected habit
- `attributedDate` = the selected past date
- `completedAt` = the last instant of that attributed date, defined as the
  moment just before the day boundary closes on the selected date
- `skipped` = false
- `note` = empty
- `trackId` = null
- `milestoneId` = null

*Motivation for the completedAt default:* the app does not know when the
activity actually happened. The end-of-attributed-day timestamp is unambiguous,
internally consistent with the day boundary, and easy to spot as a "default" if
the user wants to fix it. The existing inline time picker on the activity row
lets the user adjust it later.

Immediately after a back-fill, the new activity appears in the completed list
for the selected date. If this completion brings the habit to or above its
daily target, the corresponding missed row disappears. The user can keep
back-filling more activities for the same habit by tapping the row again (until
the missed row disappears, or by going through Other...).

### No timed sessions

Back-fill does not start a timer, even for timed habits. The activity is
recorded without a `startTime`. Duration is unknown for back-filled timed
activities.

*Motivation:* a back-fill is by definition retroactive — there is nothing live
to time. If the user wants to record an elapsed duration, they can edit
`startTime` via the existing time picker on the completed activity row.

## Editing past-day activities

All existing edit / delete / note / time-picker / track-and-milestone actions
on a completed activity work the same on past-day views. Notably:

- Editing `completedAt` to a time outside the selected date moves the activity
  to its new attributed date. The activity then disappears from the current
  view (it now lives on a different day). This is the existing behavior of the
  completed-time picker; nothing special is needed here.
- Deleting a back-filled activity is the same as deleting any other completed
  activity.

## Returning to today

There is always a one-tap path back to today from a past-day view. Exact
control is defined by the UX design; functionally, "back to today" snaps the
selected date to today's attributed date and the view reverts to its normal
today behavior.

The selected date does not persist across app launches. Opening the app always
starts on today.

## Day boundary crossing

If the user has the view open and the day boundary crosses (e.g., it's 1:59 AM
and ticks to 2:00 AM with `dayBoundaryHour = 2`), the date semantics shift
naturally: yesterday becomes the day before yesterday, today becomes yesterday,
etc. The viewmodel already refreshes today on the day boundary; the same
refresh repositions the past-day window. If the user is viewing a date that
falls out of the seven-day window after the shift, the view snaps back to today.

## Interaction with Easy Day

Easy Day is stored per attributed date. When a past day is selected:

- The missed rows respect the Easy Day level that was set for *that date*.
- The bottom-bar remaining count uses that same filter.
- Other... is not filtered by Easy Day, so the user can still back-fill an
  excluded habit if they actually did it.

The user cannot change Easy Day for a past day from this view. Easy Day is a
forward-looking knob; back-filling does not retroactively rewrite the plan.

## Out of scope

These are explicitly deferred:

- Navigation back more than seven days.
- A calendar or date-picker UI for jumping to an arbitrary past date.
- Past-day navigation on the Day Plan (main / agenda) view. The agenda remains
  fixed on today.
- Bulk back-fill (e.g., "record Reading for the last 4 missed days at once").
- Recording activities for future dates.
- Editing the Easy Day level for past days.
- Logging an actual elapsed duration as part of the back-fill action (the user
  can still set `startTime` after the fact via the existing time picker).
