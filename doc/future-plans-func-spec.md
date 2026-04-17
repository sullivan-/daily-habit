
# future plans functional spec

post-MVP features, ideas, and enhancements. this is a living document — add ideas as
they come up. items here are not committed to, just captured.

## planned features

### ephemeral chime interval

the chime interval (e.g., every 10 seconds) is not a property of the
habit — it's ephemeral state associated with an active activity. in the
expanded view for an active activity, the user can choose a chime
interval and start the chimes. the interval runs alongside the normal
timer. the habit definition no longer stores chimeIntervalSeconds;
instead the user sets it per-session in the activity view.

### in-app configuration

*habit editor (create/edit/delete), habit list, and menu are
implemented. remaining:*

- search/filter habits by name
- manage task backlogs (add, edit, reorder, complete tasks)
- config import/export (replace static JSON bootstrapping)

### history views

- view completed items for the last 7 days
- view has a big visual sense (heatmap) but also shows some content and drills
  into details
- see what was checked off each day, with notes
- weekly summary: how many days each habit was completed
- trend indicators: improving, steady, declining

### reflection and planning

- periodic review prompts (weekly?)
- space for brainstorming improvements to habits and routines
- diet tracking / improvement notes
- ability to journal about what's working and what isn't

### quick track creation from activity view

- add a way to create a simple track (no day-of-week default, no
  milestones) directly from the activity view, both compact and expanded
- avoids the round-trip to the habit editor just to add a new track name
- the track is created with default priority and immediately selected on
  the current activity

### activity day reassignment

- allow the user to manually move an activity from one day to another
- handles edge cases where the automatic day boundary doesn't match the
  user's intent (e.g., a very late night that crosses the 2 AM boundary)
- the activity keeps its actual timestamp but its attributed date changes

### skip habit for the day

- mark a habit as "skip" from the agenda list, excluding it from the
  agenda for that day only
- skipped habits don't count toward the daily target or progress bar
- the skip is per-day, not permanent — the habit reappears the next day
- useful for days when a habit genuinely doesn't apply (sick, traveling,
  equipment unavailable)
- skipped habits could appear in a separate "skipped" section or just
  vanish from the agenda entirely
- history should record that the habit was skipped (not just missing)

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
