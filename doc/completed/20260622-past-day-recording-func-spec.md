# Past-day Recording Functional Specification

## Overview

Lets the user navigate the Done view back to recent past days and record activities they did but
forgot to log. The Done view, which today shows only today's completed list, gains a date selector
and an "audit" mode for the last seven days. From any past day in that window, the user can
back-fill missed activities or add extras via the existing "Other..." path.

*Motivation:* there is currently no way to record an activity for a day other than today. Users miss
logging things — a habit completed late at night, an extra block of work done over the weekend.
Forcing the user to remember everything in real time, or lose the record, undermines the app's role
as a faithful daily ledger.

## Scope of past-day navigation

The selectable window covers today plus the seven days immediately before it — eight positions in
total, so today's same weekday last week is the earliest reachable date. Beyond that the view does
not navigate. Days older than the window are read-only via existing history surfaces (per-habit
activity history) and are not addressed by this spec.

*Motivation:* seven days covers the realistic "I forgot for a few days" case (including a forgotten
weekend) without inviting edits to ancient data. It also keeps the UI to chevrons rather than a full
date picker.

## The Done view, parameterized by date

The Done view (the Review layout — completed list plus agenda bar) is generalized to display any
single attributed date in the window. Its content is defined entirely by that selected date.

### When the selected date is today

Behavior is unchanged from current: today's completed activities appear in chronological order and
the bottom bar shows remaining count for today. The Done view has no Other... entry; adding an
activity for today is done from the main agenda view, which carries its own Other... entry.

### When the selected date is a past day

The screen contents are:

1. **Completed activities for that date** — every activity with attributedDate equal to the selected
   day, in chronological order. Same row presentation as today's completed list. Tapping a row opens
   the activity in detail and supports the existing edit / delete / note / time-picker actions.

2. **Missed rows** — for each habit that was active that weekday and had completions below its
   daily target on that date, a single greyed-out row. The row shows the habit name and the running
   count out of target on that date (e.g., "Badux 1/3" or "Reading 0/1"). Tapping records one new
   activity on that date (see Back-fill action below).

3. **Other... entry** — the same "Other" picker the main agenda uses for today, surfaced here
   because the past-day Done view is the only entry point for adding a past-day activity. Scoped to
   the selected date, it lists every habit not already represented as a missed row — including
   at-least habits already at or above target on that date and habits not active on that weekday —
   except exactly-mode habits already at or above target, which are suppressed just as they are in
   today's Other... picker (adding a second instance would violate the habit's exactly-once mode).
   Selecting a habit performs a back-fill in the same way as tapping a missed row.

The bottom bar shows remaining count for the selected date — the sum of daily targets across
habits active that weekday, minus completions on that date.

*Motivation:* the missed rows handle the common forgot-something case directly and discoverably.
Anything else — extras, or days where the habit wasn't expected — is still reachable through
Other..., the same escape hatch the agenda uses for today.

## Back-fill action

When the user taps a missed row (or picks a habit through Other... on a past day), a new activity is
created with:

- `habitId` = the selected habit
- `attributedDate` = the selected past date
- `completedAt` = the last instant of that attributed date, defined as the moment just before the
  day boundary closes on the selected date
- `skipped` = false
- `note` = empty
- `trackId` = null
- `milestoneId` = null

*Motivation for the completedAt default:* the app does not know when the activity actually happened.
The end-of-attributed-day timestamp is unambiguous, internally consistent with the day boundary, and
easy to spot as a "default" if the user wants to fix it. The existing inline time picker on the
activity row lets the user adjust it later.

Immediately after a back-fill, the new activity appears in the completed list for the selected date.
If this completion brings the habit to or above its daily target, the corresponding missed row
disappears. The user can keep back-filling more activities for the same habit by tapping the row
again (until the missed row disappears, or by going through Other...).

### Auto-open for editing

After the back-fill activity is created, the view immediately auto-expands into the existing
completed-activity detail with the new activity selected. The user can adjust `completedAt`, note,
track/milestone, or any other editable field right there; dismissing the detail returns to the
past-day list and leaves the back-fill in place with whatever values remain. This applies whether
the back-fill came from tapping a missed row or from picking a habit through Other....

*Motivation:* the end-of-day `completedAt` is a reasonable default but rarely the actual completion
time. Auto-opening the detail makes a one-tap correction possible without forcing a follow-up step
to find the new row and tap it.

### No timed sessions

Back-fill does not start a timer, even for timed habits. The activity is recorded without a
`startTime`. Duration is unknown for back-filled timed activities.

*Motivation:* a back-fill is by definition retroactive — there is nothing live to time. If the user
wants to record an elapsed duration, they can edit `startTime` via the existing time picker on the
completed activity row.

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
falls out of the window after the shift, the view snaps back to today.

## Habit configuration drift

Missed-row eligibility, the daily-target count shown on a missed row, and the bottom-bar remaining
count all use the current habit configuration — today's `daysActive`, `dailyTarget`, `priority`,
etc. — even when the selected date is in the past. If a habit's config has changed since the past
date, the past-day view reflects the *current* shape, not the shape on the date itself. A habit
that no longer exists contributes neither a missed row nor any share of the remaining count,
although any activities it recorded before deletion still appear in the completed list if they
remain in the database.

*Motivation:* habit configs are not versioned, so snapshotting historical values is out of scope for
this feature. The back-fill use case — recording activities the user actually did but forgot to log
— does not depend on knowing the historical config; the user knows what they did. Reflecting
current config keeps the data model simple and avoids the false precision of a snapshot the schema
cannot actually provide.

## Interaction with Easy Day

Easy Day applies only to today. Past-day views ignore it entirely: missed rows and the bottom-bar
remaining count are computed from each habit's raw `dailyTarget`, regardless of any Easy Day level
that may have been set on that date or any carry-over level in effect now.

*Motivation:* Easy Day is a forward-looking knob — it shapes what the user plans to do today, not a
historical record of what they were supposed to do on some past day. Carry-over compounds this: it
has no per-day history, so there is no reliable answer to "what was Easy Day on Tuesday." Ignoring
Easy Day on past days keeps the back-fill view a faithful ledger of what was actually scheduled.

## Out of scope

These are explicitly deferred:

- Navigation back more than seven days.
- A calendar or date-picker UI for jumping to an arbitrary past date.
- Past-day navigation on the Day Plan (main / agenda) view. The agenda remains
  fixed on today.
- Bulk back-fill (e.g., "record Reading for the last 4 missed days at once").
- Recording activities for future dates.
- Editing the Easy Day level for past days.
- Applying Easy Day filtering or degradation to past-day views in any form.
- Logging an actual elapsed duration as part of the back-fill action (the user
  can still set `startTime` after the fact via the existing time picker).
