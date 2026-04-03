# future plans functional spec

post-MVP features, ideas, and enhancements. this is a living document — add ideas as
they come up. items here are not committed to, just captured.

## planned features

### multiple thresholds per habit

a habit should be able to have both a Goal threshold and a Time to Stop
threshold. e.g., Qigong: goal at 30 minutes, time to stop at 45 minutes.
each triggers its own chime. the current model only supports one
threshold — this needs a schema change to support a list.

### ephemeral chime interval

the chime interval (e.g., every 10 seconds) is not a property of the
habit — it's ephemeral state associated with an active activity. in the
expanded view for an active activity, the user can choose a chime
interval and start the chimes. the interval runs alongside the normal
timer. the habit definition no longer stores chimeIntervalSeconds;
instead the user sets it per-session in the activity view.

### timer value editing

- allow the user to manually adjust an activity's start time, end time,
  or elapsed duration after the fact
- handles cases where the user forgot to start/stop the timer

### task backlogs

activities can optionally have a prioritized list of tasks. when present, each
session prompts the user to pick (or continue) a task from the backlog.

- each task has a name and optional notes
- tasks can span multiple sessions and multiple days — a task stays in the
  backlog until explicitly marked complete
- user can reorder tasks to adjust priority
- daily target interacts with the backlog: "Home" with target=1 means "do at
  least one home task today," picking from the backlog
- habits like "Badux" with target=3 can continue the same task across
  activities or pick different ones

### progression tracking

track position in multi-step sequences. examples:
- qigong: position in two parallel recorded courses
- reading: current book in each reading track (Peter Kingsley, The Bible, Greek
  History, New Science, Health)
- work: position in ongoing work streams

a progression has:
- a name (e.g., "Old Testament reading")
- an ordered list of steps (e.g., Genesis, Exodus, Leviticus...)
- a current position
- optional linkage to a daily habit (e.g., "Read" habit auto-shows current
  progression state)

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

### abstinence / avoidance tracking

track what you're staying away from, ported from `sched/absta.py`:
- daily tracking of substances/behaviors to avoid (alcohol, nicotine, sugar, etc.)
- streak counting (days since last)
- visual calendar view

### reflection and planning

- periodic review prompts (weekly?)
- space for brainstorming improvements to habits and routines
- diet tracking / improvement notes
- ability to journal about what's working and what isn't

### activity day reassignment

- allow the user to manually move an activity from one day to another
- handles edge cases where the automatic day boundary doesn't match the
  user's intent (e.g., a very late night that crosses the 2 AM boundary)
- the activity keeps its actual timestamp but its attributed date changes

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

### habit difficulty and flexibility

- some habits are "hard" (must do daily) vs "soft" (aim for 5/7 days)
- allow the user to set realistic targets per habit rather than all-or-nothing

### "good enough" days

- avoid the "ruined day" effect where missing one thing makes you give up on the rest
- maybe track partial completion positively rather than showing failures

### diet evolution tracking

- diet improvement is not a checkbox — it's a gradual process
- could track specific dietary changes being attempted and how they're going
- weekly notes on what dietary experiments are in progress
