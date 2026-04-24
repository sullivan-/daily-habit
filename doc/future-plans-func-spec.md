
# future plans functional spec

post-MVP features, ideas, and enhancements. this is a living document — add ideas as
they come up. items here are not committed to, just captured.

## planned features

### navigate to previous days

allow the user to go back to yesterday (or earlier) and check off items
they forgot. currently there is no obvious way to record activities for
a day that has already passed.

### in-app configuration

*habit editor (create/edit/delete), habit list, and menu are
implemented. remaining:*

- search/filter habits by name
- manage task backlogs (add, edit, reorder, complete tasks)
- config import/export (replace static JSON bootstrapping)

### history views

*per-habit track history is implemented (TrackHistoryView shows
completed activities with dates, tracks, milestones, and notes).
today's completed items appear in the CompletedList. remaining:*

- view completed items across multiple days (not just today)
- heatmap or visual overview that drills into details
- weekly summary: how many days each habit was completed
- trend indicators: improving, steady, declining

### reflection and planning

- periodic review prompts (weekly?)
- space for brainstorming improvements to habits and routines
- diet tracking / improvement notes
- ability to journal about what's working and what isn't

### quick track creation from activity view

- add a way to create a simple track (no day-of-week default, no
  milestones) directly from the activity view, both compact and
  expanded
- avoids the round-trip to the habit editor just to add a new
  track name
- the track is created with default priority and immediately
  selected on the current activity

### activity day reassignment

*attributedDate exists on Activity and is automatically
recalculated when completedAt changes, but there is no manual UI
to reassign an activity to a different day. remaining:*

- expose a UI control to manually change an activity's attributed
  date
- handles edge cases where the automatic day boundary doesn't
  match the user's intent (e.g., a very late night that crosses
  the 2 AM boundary)
- the activity keeps its actual timestamp but its attributed date
  changes

### time tracking history

- log of time spent per activity over days/weeks
- aggregate stats: average time per activity, trends
- export capability

### export / print view

- optional: generate a weekly PDF summary from the app's data
- useful for review or archival, not as a primary interaction tool

### notifications and reminders

- optional daily reminder to open the app at a configured time
- gentle nudge if a habit hasn't been checked off by its usual time
- be careful not to become annoying — the user should feel pulled toward the app,
  not nagged by it

### widget

- home screen widget showing today's progress (e.g., "7/12 done")
- quick-tap to check off simple items without opening the app

### longest streak

track and display the longest streak ever recorded for each tally.
shown on the Details screen alongside the current streak. requires
scanning full choice history rather than just finding the most recent
Yes.

### streak history

a list or calendar view of all past streaks for a tally, showing when
they started, ended, and how long they lasted.

### streak milestones

notifications or visual celebrations when a streak reaches a milestone
(e.g., 30 days, 100 days, 1 year).

## ideas and notes

*capture interesting ideas here as they come up during development*

### day type / busy day mode

- let the user set a day type (e.g., "busy", "normal", "light") that filters
  the agenda by priority — on a busy day, only show high-priority habits
- could be manual (set in the morning) or automatic (based on day of week)
- the progress bar and daily status would adjust expectations to match

### "good enough" days

- avoid the "ruined day" effect where missing one thing makes you give up on the rest
- maybe track partial completion positively rather than showing failures
