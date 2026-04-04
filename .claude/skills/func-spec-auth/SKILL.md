---
name: func-spec-auth
description: co-author a functional specification for a new feature. use when
  the user wants to define what a feature does from the user's perspective
  before writing a tech spec.
argument-hint: [feature name or description]
effort: high
---

Co-author a functional specification with me for: $ARGUMENTS

If no feature was specified, ask what feature to spec.

## Preparation

Before drafting, do two things:

1. Read project context: `CLAUDE.md` for conventions and design principles,
   and `doc/habit-ubiquitous-language.md` for domain terminology.

2. Read 1-2 completed func specs from `doc/completed/` that are most relevant
   to the feature being specced. Use them to calibrate tone, depth, and
   structure — not as rigid templates.

   Available completed specs:
   !`ls doc/completed/*func* 2>/dev/null`

   If no completed specs exist yet, read any active specs in `doc/` for
   reference:
   !`ls doc/*func* 2>/dev/null`

Then explore specific code relevant to the feature to understand current
state.

## Relationship to UX design

the func spec and UX design are companion documents. both should be
developed before moving to a tech spec, but they can be written in either
order or interleaved. the func spec is the source of truth for behavior
(what happens). the UX design is the source of truth for presentation
(how it looks and flows). if the UX design reveals that a behavioral rule
needs to change, update this spec accordingly.

## Format

- use regular English capitalization in spec documents.
- line limit in markdown files is 100 characters.
- write in clear, direct prose — not implementation language.
- italicize opening labels for asides, e.g., *motivation:*, *note:*.
- active working specs go directly in `doc/`. specs not actively being
  worked on go in `doc/open/`. implemented specs move to `doc/completed/`
  with a date prefix (e.g., `20260505-feature-func-spec.md`).
- align columns in markdown tables (all `|` symbols in a column should
  line up).

## Structure

Organize the spec by feature area, not by a rigid template. Most specs
include an overview, then sections for each distinct aspect of the feature
(UI flows, behavior, rules). Common elements:

- a short overview of what the feature does and why.
- UI walkthroughs organized by screen or entry point.
- behavioral descriptions for user actions, including error states.
- a "future enhancements (out of scope)" section for things explicitly
  deferred.

when the reasoning behind a functional choice isn't obvious, include a
short motivation blurb explaining why.

## Domain language

use terms from `doc/habit-ubiquitous-language.md` consistently. if the
feature introduces new concepts, names, or terms — or changes how existing
ones are used — update the ubiquitous language doc as part of the spec
work. name choices shape how the code reads later. when a naming decision
is non-obvious, briefly note alternatives considered and why the chosen
term won.

## Collaboration

- start by asking clarifying questions about the feature before drafting.
- when seeking clarification or input on multiple points, work through
  them one at a time to maintain a good collaborative flow.
- present a first draft, then iterate based on feedback.
- keep scope tight — if something feels like a separate feature, call it
  out as out of scope.

## Quality

- think carefully about edge cases and how they should be handled.
- look for potential gaps or holes in the spec — missing states, unhandled
  transitions, ambiguous wording — and flag them.
- the goal is a spec clear enough to hand off for tech spec writing.
