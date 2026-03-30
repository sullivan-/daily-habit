---
name: ux-design-auth
description: co-author a UX design document for a feature. use when the user
  wants to define how a feature looks, feels, and flows before writing a tech
  spec.
argument-hint: [feature name or description]
effort: high
---

Co-author a UX design document with me for: $ARGUMENTS

If no feature was specified, ask what feature to design.

## Preparation

Before drafting, do three things:

1. Read project context: `CLAUDE.md` for conventions and design principles,
   and `doc/habit-ubiquitous-language.md` for domain terminology (especially
   the UX bounded context).

2. Read the functional spec for the feature being designed. the func spec
   defines *what* happens; this doc defines *how it looks and feels*. do not
   redefine behavior — present it.

   Active func specs:
   !`ls doc/*func* 2>/dev/null`

3. Read 1-2 completed UX designs from `doc/closed/` that are most relevant
   to the feature. use them to calibrate tone, depth, and structure — not
   as rigid templates.

   Available completed UX designs:
   !`ls doc/closed/*ux* 2>/dev/null`

   If no completed UX designs exist yet, read any active ones in `doc/`:
   !`ls doc/*ux* 2>/dev/null`

Then explore specific code relevant to the feature to understand current
state.

## Relationship to functional spec

the func spec and UX design are companion documents. both should be
developed before moving to a tech spec, but they can be written in either
order or interleaved. the func spec is the source of truth for behavior
(what happens). the UX design is the source of truth for presentation
(how it looks and flows). if the UX design reveals that a behavioral rule
needs to change, update the func spec — don't silently override it.

## Format

- use regular English capitalization in spec documents.
- line limit in markdown files is 100 characters.
- write in clear, direct prose — not implementation language.
- italicize opening labels for asides, e.g., *motivation:*, *note:*.
- active working docs go directly in `doc/`. paused or future docs go in
  `doc/open/`. implemented docs move to `doc/closed/` with a date prefix
  (e.g., `20260505-feature-ux-design.md`).
- align columns in markdown tables.

## Content

organize the doc by screen or layout, not by a rigid template. most UX
designs include:

- **layouts**: the distinct arrangements of the screen. describe what
  elements are visible in each layout, where they sit, and how they
  relate to each other.
- **visual layouts**: include ASCII wireframes or diagrams to illustrate
  layouts. embedded images can be referenced when ASCII isn't sufficient.
- **navigation and transitions**: how the user moves between layouts.
  what gestures or taps trigger transitions, what animations occur,
  whether the app remembers layout state.
- **element behavior**: how individual UI elements respond to interaction
  (tap, long press, swipe). what visual feedback they provide.
- **states**: how the screen looks in different conditions — first launch,
  empty, in-progress, complete, error. especially important: what the user
  sees on initial launch.
- **information hierarchy**: what's most prominent, what's secondary, what's
  hidden until tapped.

when the reasoning behind a UX choice isn't obvious, include a short
motivation blurb explaining why.

## Domain language

use terms from `doc/habit-ubiquitous-language.md` consistently, especially
the UX bounded context section. if the design introduces new UI concepts
or terms, update the UX section of the ubiquitous language doc as part of
the design work.

## Collaboration

- start by asking clarifying questions about the feature before drafting.
  if the user provides sketches or layout ideas, use those as a starting
  point.
- when seeking clarification or input on multiple points, work through
  them one at a time to maintain a good collaborative flow.
- present a first draft, then iterate based on feedback.
- keep scope tight — if something feels like a separate feature, call it
  out as out of scope.

## Quality

- think carefully about edge cases in presentation — empty states, very
  long text, many items, zero items.
- consider how the design works across the user's actual daily flow, not
  just in isolation.
- look for gaps — screens with no way out, states with no visual
  representation, transitions with no trigger.
- the goal is a design clear enough to hand off for tech spec writing.
