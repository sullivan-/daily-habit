## Streaks Functional Specification ##

### Overview ###

Streaks track how long a user has been continuously abstaining from a
tally. A streak is the time period from the first No after the most
recent Yes, up to the present moment. If the tally has never had a
Yes, the streak starts from the very first No ever recorded.

This feature also introduces a **Details screen** for tallies,
replacing the current edit button. The Details screen combines the
existing editing capabilities with streak information and choice
statistics.

*Motivation:* streaks give the user a concrete, growing measure of
their discipline. Seeing "4 year streak" on Alcohol is more
motivating than "10/10."

### Streak Definition ###

A **streak** is a continuous run of abstention (No choices), measured
as elapsed time.

- Find the most recent Yes (indulge) choice for the tally.
- The streak starts at the first No that comes after that Yes.
- If there has never been a Yes, the streak starts at the very
  first No ever recorded.
- The streak ends at the present moment.
- If the most recent choice is a Yes, there is no current streak.
- If the tally has no choices at all, there is no current streak.

The streak is measured in wall-clock time from the start date to now,
not by counting individual No choices.

### Streak Display ###

The current streak is shown in the tally list row, replacing the
N/10 indicator that previously occupied that space.

#### Display format ####

Streaks are displayed as a count and unit followed by "streak":

- **Days** for streaks under 2 months (e.g., "1 day streak",
  "43 day streak")
- **Months** for streaks from 2 months up to 12 months (e.g.,
  "3 month streak", "11 month streak")
- **Years** for streaks of 12 months or more (e.g., "1 year streak",
  "4 year streak")

Values always round down. 23 hours and 59 minutes is not a day.
11 months and 29 days is not a year. The streak must never be
overstated.

#### No current streak ####

If the most recent choice was a Yes, or if the tally has no choices,
the row shows nothing in the indicator space — just the tally name
and the No/Yes buttons.

#### Minimum display threshold ####

Streaks under 1 day are not displayed. The streak appears in the
list row once it reaches 1 full day.

### Tally List Row (revised) ###

Each row in the tally list shows:

- **Tally name** — tappable, navigates to the Details screen
- **Current streak** — if any (e.g., "7 day streak"), displayed
  where the N/10 indicator used to be
- **No button** — record an abstain choice
- **Yes button** — record an indulge choice

The edit icon button is removed. Navigation to the Details screen
is through tapping the tally name.

### Details Screen ###

The Details screen is a full screen accessed by tapping a tally name
in the list. A back button in the top bar returns to the Choices
screen.

#### Contents ####

The Details screen contains, in order:

1. **Name field** — editable, same as current edit screen
2. **Priority selector** — editable, same as current edit screen
3. **Current streak** — if any, displayed as the streak duration
   with the precise start date (e.g., "43 day streak — since
   Mar 11, 2026")
4. **Choice stats**:
   - Abstain count out of the last 10 choices (e.g., "8/10
     last 10"). Always shown if there are any choices.
   - Today's count (e.g., "3/5 today"). Shown only when there
     are 3 or more choices today.
5. **Record First No** — button that lets the user insert a No
   choice at a historical date (see below)
6. **Save button**
7. **Delete button** — same behavior as current edit screen
   (confirmation dialog)

### Record First No ###

This action lets the user record a No choice at a date that
predates all existing choices for the tally. This is useful for
tallies where the user has been abstaining for a long time before
they started using the app.

*Example:* the user quit drinking alcohol 4 years ago. They create
an Alcohol tally and tap Record First No, selecting the date they
quit. A No choice is inserted at that date. The streak now reflects
the full duration of their abstention.

#### Behavior ####

- Tapping the button opens a date picker.
- The date picker constrains the selectable range to dates before
  the earliest existing choice for this tally.
- Selecting a date inserts a regular No choice at that date.
- The inserted choice is not a special entity — it is an ordinary
  choice with `abstained = true` and a timestamp on the selected
  date.
- The user can use Record First No multiple times. Each use adds
  a new No choice, always constrained to be before all existing
  choices. Previous uses are not modified or replaced.

#### When to show ####

Record First No is always available on the Details screen, as long
as it is possible to select a date before the earliest existing
choice. If the earliest choice is already at the earliest
representable date, the button is hidden (an edge case unlikely to
occur in practice).

### Future Enhancements (out of scope) ###

These are deferred to future work:

- **Longest streak** — the longest streak ever recorded for a tally,
  displayed on the Details screen alongside the current streak
- **Streak history** — a list or calendar view of all past streaks
- **Streak notifications** — milestone alerts (e.g., "You've
  reached 100 days!")
