# Tracks Functional Specification

## Overview

a track is a sub-category within a habit. tracks let the user organize and label what they're
working on within a single habit, without overloading the notes field.

> John has a Work activity. within Work, he tracks specific focuses: Learning, Research,
> Engineering, Meetings, Documentation. when John starts a Work activity, he selects whichever
> track he's working on for that session.

track information is currently kept ad-hoc in the notes field. once this spec is implemented, the
notes field will be free of track-related concerns.

## Core Concept

a track is a named sub-category of a habit. all tracks share the same structure and lifecycle —
the differences between a long-lived category ("Meetings"), a short-lived task ("clean curtains"),
and a structured course ("Foundational Qigong") are differences in how quickly the track gets
archived and whether it has an ordered series, not differences in kind.

a track has:

- a **name**
- an optional **day-of-week default** (e.g., "sinus rinse" defaults to Monday)
- an optional **ordered series** of milestones (e.g., lesson 1, practice 1, lesson 2, ...)
- a **priority** for sorting in the suggestion list
- an **archived** flag

### Persistence Across Activities

a track is not consumed by a single activity. selecting a track for an activity does not close or
advance the track — it persists across as many activities as the user needs. "clean curtains"
might take three sessions over two days. "Research" might be selected hundreds of times over
months.

### Archival

any track can be archived at any time. archiving removes the track from the suggestion dropdown
but preserves its history — past activities that referenced it remain intact. archival is not
deletion. an archived track can be un-archived if the user wants to resume it.

the intent is to keep the dropdown clean, not to enforce lifecycle rules. some tracks ("Meetings")
may never be archived. others ("clean curtains") may be archived after one day. the app does not
distinguish between these — archival is always a manual user action.

## Day-of-Week Defaults

a track can be assigned a default day of the week. when the user opens the track selector on that
day, the day-default track appears at the top of the suggestion list.

this replaces the existing `dailyTexts` feature, which pre-populates the notes field based on the
day of week. `dailyTexts` will be removed from the habit model — no migration, the user recreates
any needed day-of-week defaults as tracks via the UI.

## Ordered Series

a track can optionally have an ordered series of milestones. each milestone has a name and a
completed flag.

> John has a Foundational Qigong track in his Qigong Training habit. it consists of 4 lesson
> videos and 4 practice videos:
>
> - lesson 1
> - practice 1
> - lesson 2
> - practice 2
> - lesson 3
> - practice 3
> - lesson 4
> - practice 4

when John selects the Foundational Qigong track, the activity defaults to the first incomplete
milestone. a selection list shows all remaining incomplete milestones, so the user can pick a
different one if needed. the order is a suggestion, not a constraint.

when the activity is done, John marks the milestone as complete (checkmark) or leaves it in
progress to pick up next time. a single milestone can span multiple activities (e.g., a long
lesson worked on over two days).

archiving a series track works the same as any other track. the user might archive it after
completing all milestones, or abandon it partway through.

## Track Selection

selecting a track for an activity should be easy — a simple dropdown in the activity view. the
dropdown shows non-archived tracks for the current habit, ordered by suggestion priority.

## Suggestion Order

when the user opens the track selector, tracks are ranked:

1. **day-of-week defaults** for today appear first
2. **high-priority tracks** appear next (user-assigned priority)

archived tracks do not appear in the suggestion list. least-recently-used sorting may be added
in a future iteration.

## UX Surfaces

the following UI areas need design (see `tracks-ux-design.md`):

- **habit editor: track management** — adding, archiving, deleting, and editing tracks within a
  habit. includes configuring day-of-week defaults, priority, and ordered series milestones.
- **activity view: track selection** — selecting a track for an active or completed activity.
  includes displaying the current milestone for series tracks.
- **activity view: milestone editing** — marking milestones complete or in-progress on active and
  completed activities.

## Seed Data

the `habits.json` config file should include example tracks to exercise the different
configurations:

*note: the examples below are rough sketches. the actual seed data will be designed interactively
during implementation.*

- **Badux** — simple tracks: "baduxgo server", "baduxgo web", "baduxgo bot". demonstrates basic
  sub-categories for a work habit.
- **Body Care** — day-of-week default tracks: "sinus rinse" (Monday), "ear drops" (Tuesday),
  "cholesterol" (Wednesday), "pull oil" (Thursday), "fast" (Friday), "trim beard" (Saturday),
  "scalp care" (Sunday). replaces the current `dailyTexts` entries.
- **Qigong Training** — series track: "Foundational Qigong" with milestones (lesson 1, practice
  1, lesson 2, practice 2, lesson 3, practice 3, lesson 4, practice 4). demonstrates ordered
  series.
- **Home / TODO** — task-style tracks: "tend plants", "backups", "cleaning". short-lived items
  that get archived when done. "tend plants" defaults to Monday, "backups" to Wednesday,
  "cleaning" to Friday, replacing the current `dailyTexts` entries.

## Out of Scope

- historical views of track activity (saved for a later version — for now, track info is visible
  when swiping through activity history)
- chime intervals tied to specific tracks
- track-level time tracking (duration is per-activity, not per-track)
