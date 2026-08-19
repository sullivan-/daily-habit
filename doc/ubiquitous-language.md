# Habit — Ubiquitous Language

This is the domain glossary for the Habit app, following Domain-Driven Design
practice. These terms should be used consistently in all specs, code, and
conversation. When a term here conflicts with informal usage elsewhere, update
the other document. If a term needs to change, change it here first, then
propagate.

## Core Domain

### Habit

The persistent definition of something the user does regularly. A habit has a
name, configuration (days active, daily target, timer settings, priority,
priority weight), and is not tied to a specific date — it is the template from
which daily work is derived.

Examples: "Qigong", "Home", "Badux", "Vitamins"

### Activity (also: Task)

A single recorded instance of performing a habit. An activity belongs to one
habit and one calendar date. It captures when the work happened and an optional
note. An activity may optionally be associated with a track and a milestone
within that track. An activity is the actual accomplishment — the real thing
that was done.

For timed habits, an activity also records: start time (first start tap), end
time (done tap), and elapsed duration (accumulated running time, excluding
pauses). The timer can be paused and resumed any number of times within an
activity.

An activity is the atomic unit of "I did this." Checking off an item or tapping
done creates an activity.

### Daily Target

A property of a habit: how many activities the user aims to complete per day,
with a mode of **exactly** or **at least**. "Exactly 1" means do it once and no
more (e.g., Vitamins). "At least 3" means do it three or more times, with extra
activities available via "do again" (e.g., Badux). The target number controls
when the habit leaves the agenda; the mode controls whether extras are allowed.

### Daily Progress

A derived value: the number of activities logged for a habit on a given date,
compared to its daily target. E.g., "2/3" means two activities completed out of
a target of three. This is not a stored entity — it is computed from activities.

### Skip

Dismissing a pending activity for the day without completing it — recording
that the user consciously chose not to do it. A skipped activity is stamped
with a completedAt timestamp (so it leaves the agenda) but carries a skipped
flag, so it counts as neither pending nor done: it is excluded from daily
progress and from the completed list.

### Day Boundary

The hour at which one day ends and the next begins (default: 2:00 AM). Activities
completed before the day boundary are attributed to the previous calendar date.
The app's concept of "today" is defined by this boundary, not midnight. An
activity records both its actual timestamp and its attributed date, which may
differ.

## Easy Day

### Easy Day

A per-day planning setting that lightens the day's expected workload by
dropping lower-priority habits. It is forward-looking: it shapes what the
user plans to do today (and future days, via carry-over), and never
applies to past days — past-day views always use raw daily targets.

### Easy Day Level

The chosen tier. `OFF` means no lightening. The other tiers each name a
priority threshold (from low up to high). A habit is on the day if and
only if its priority is strictly above the threshold; included habits keep
their full daily target, and habits at or below the threshold are dropped
entirely, regardless of daily target. A higher level lightens the day
more.

### Carry-over

When enabled, the chosen Easy Day level persists across day rollovers
until the user turns it off, instead of resetting to `OFF` each new day.
Carry-over keeps no per-day history — there is no record of what the level
was on any particular past day.

## Habit Configuration

### Days Active

Which days of the week a habit appears on the agenda. E.g., Badux is Mon-Fri.
Qigong is every day. A habit with no days active never appears (effectively
disabled).

### Timed / Untimed

Whether a habit involves a timer. **Timed** habits (Qigong, Badux, Read) show
an elapsed timer, support chime intervals, and record duration. **Untimed**
habits (Vitamins, Waterpick) are simple checkboxes — tap done, no timer, no
duration recorded.

### Goal Time

For timed habits: an optional duration milestone meaning "you've done enough."
when the timer reaches the goal time, a chime plays. e.g., 15 min for reading,
30 min for qigong.

### Stop Time

For timed habits: an optional duration milestone meaning "wrap up now." when the
timer reaches the stop time, a chime plays. e.g., 50 min for a work session.
a habit can have both a goal time and a stop time.

### Priority

The importance of a habit. Five tiers: high, medium-high, medium, medium-low,
low. Affects both agenda ordering (higher priority habits appear earlier at the
same activity level) and progress bar color calculation (missing a low-priority
habit has less impact on daily status than missing a high-priority one). Within
the same priority tier, tie breaker breaks ties.

### Time of Day

The hour when a habit is typically performed (e.g., 7, 8, 14). Used to position
the habit's **first** activity in the agenda. Subsequent activities (activity 2,
3, etc.) are positioned by priority weight, not by time of day.

### Tie Breaker

Ordering among habits with the same time of day. Higher values appear first in
the agenda. Distinct from priority, which controls ordering of subsequent
activities.

### Daily Text (removed)

Replaced by track day-of-week defaults. Previously, this was optional
day-specific default text for a habit that pre-populated the notes field.
Now, day-of-week behavior is handled by assigning a default day to a track,
which appears at the top of the track selector on that day.

## Chimes

### Threshold Chime

A one-shot chime that fires when a timed activity's elapsed time crosses a
configured boundary — the habit's goal time or stop time. Each fires once
per activity.

### Interval Chime

A repeating chime that fires at a fixed cadence throughout a timed
activity: the first fires immediately, then one every interval until
stopped. The interval is ephemeral session state chosen when chimes are
started, not part of the habit definition. Independent of threshold chimes
— both can fire in the same activity. Short "seconds" intervals suit
rhythmic exercises; longer "minutes" intervals pace work blocks.

## Choices Domain

### Tally

The persistent definition of something the user is tempted to do but is trying
to avoid. A tally has a name and a priority. It is the counterpart to a habit —
where a habit tracks something positive to cultivate, a tally tracks something
to resist.

Examples: "Nicotine", "Alcohol", "Video Games", "Sweets"

### Choice

A single recorded event on a tally. The user was tempted and chose to abstain
(resist the impulse) or indulge (give in). A choice captures only a timestamp
and the decision — no notes or additional data. A tally accumulates many
choices over time.

### Streak

A continuous run of abstention on a tally, measured as elapsed wall-clock time rather than a count
of choices. The streak starts at the first No after the most recent Yes — or, if the tally has never
had a Yes, at the very first No ever recorded — and runs to the present. There is no current streak
if the most recent choice was a Yes or the tally has no choices. Displayed rounded down (days, then
months, then years) and only once it reaches a full day.

### Choices Screen

A separate screen accessed from the menu. Displays the user's tallies as a list. Each row shows the
tally name (tappable, opening the tally's Details screen), the current streak if any (where the
abstain-ratio indicator used to sit), and Yes/No buttons for recording choices. Tallies are sorted
by a blend of priority and recency of activity.

### Choices Bar

A status bar at the bottom of the choices screen, following the same pattern as the progress bar
and agenda bar. Contains the menu button, the "Choices" label, a weekly running total showing the
abstain-to-total ratio across all tallies for the past seven days, and a + button for creating
new tallies.

### Tally Details Screen

A full screen reached by tapping a tally name, replacing the former edit button. Combines the tally
editing fields (name, priority, delete) with the current streak and its start date, choice stats
(abstain count out of the last ten, plus today's count when there are three or more today), and the
Record First No action.

### Record First No

An action on the Tally Details screen that inserts an ordinary No choice at a date predating all of
the tally's existing choices. It seeds a streak that began before the user started logging — for
example, recording the date someone quit drinking years ago. Repeatable; each use adds another,
earlier No.

## Tracks Domain

### Track

A named sub-category within a habit. A track has a name, optional day-of-week
default, optional ordered milestone series, a priority, and an archived flag.
Tracks persist across activities — selecting a track for an activity does not
consume it.

### Milestone

An ordered item within a track's series. Milestones have a name, sort order,
and completed flag. The sort order suggests which milestone to work on next,
but the user can pick any incomplete milestone.

### Archival

Soft removal of a track from the suggestion dropdown. Preserves history — past
activities that referenced the track remain intact. Reversible via un-archive.

## UX Context

### Activity View

The top element on the primary screen. Appears in two forms:

- **Compact activity view** — the top section in main and review layouts.
  Shows the current habit with timer controls, or a completed activity for
  review. When nothing is selected, shows a contextual summary. Has an
  unfold icon to expand.
- **Expanded activity view** — the full-screen view in the activity focused
  layout. Shows all activity detail including history swipe, start/completion
  times, duration, and editable notes. Has an unfold-less icon to collapse
  back to compact. Supports swiping left/right to browse past activities
  for the same habit.

### Agenda

The forward-looking queue in the middle section of the main layout. Shows only
incomplete items — completed activities move to the completed list. For habits
with daily target > 1, only the next pending activity appears; the following
activity enters the queue once the current one is completed.

### Completed List

The backward-looking list in the middle section of the review layout. Shows all
activities completed today in chronological order, with timestamps, durations,
and note previews.

### Progress Bar

A compact bar at the bottom of the main layout showing overall daily progress
(e.g., "9/14 activities complete"). Color-coded by status (blue = on track,
green = ahead, red = behind). Tapping it switches to the review layout. Swiping
left switches to review.

### Agenda Bar

A compact bar at the bottom of the review and activity focused layouts. Shows
remaining activity count. Tapping it switches to the main layout. Swiping right
switches to main; swiping left navigates to the choices screen.

### Layout

One of three arrangements of the primary screen: **main** (forward-looking,
agenda + progress bar), **review** (backward-looking, completed list + agenda
bar), or **activity focused** (expanded detail + agenda bar). The user switches
between layouts by tapping or swiping the bars. The choices screen is a separate
destination reachable by swiping left from the review/activity focused bars, and
swiping right from the choices bar returns to the primary screen.

### Display Ordering

The algorithm that sorts the agenda. Considers time of day (for first
activities), activity level (first activities before second activities), and
priority weight (within the same activity level). The ordering is a suggestion —
the user can always tap any item to work on it out of order.

### Track Selector

A dropdown in the activity view for selecting a track. Appears between the
habit name row and the timer. Only visible when the habit has tracks
configured.

### Milestone Selector

A checkbox and dropdown below the track selector for series tracks. Defaults
to the first incomplete milestone; the user can pick any incomplete one.

## Past-day Recording

*Specced, not yet implemented — see `past-day-recording-func-spec.md`,
`-ux-design.md`, and `-tech-spec.md`.*

### Selected Date

The attributed date the Review view is currently showing. Defaults to today
and can be stepped back within a seven-day window. Distinct from the real
today, which still governs the day boundary, the navigable window, and
whether Easy Day applies.

### Date Strip

The control at the top of the Review layout for choosing the selected date:
a back chevron, a centered date label (which doubles as a one-tap return to
today), and a forward chevron. Present on today as well as on past days.

### Missed Row

On a past day's Review list, a greyed row for a habit that was active that
weekday but finished below its daily target on that date. Shows the habit
name and, when the target exceeds one, the running count out of target.
Tapping it back-fills an activity for that habit on that date.

### Back-fill

Recording an activity for a past day that the user forgot to log at the
time. The activity is created with that day's attributed date and a default
timestamp at the end of that day, ready for the user to adjust. Reached by
tapping a missed row or picking a habit through the past-day Other... picker.
