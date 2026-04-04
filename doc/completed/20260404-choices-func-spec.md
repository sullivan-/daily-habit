# Choices Functional Specification

the purpose of this feature is to track and correct unwanted habits. but we
don't call them habits, as in the mindset of this app, a habit is something
that we want to cultivate. we want to track the positive without avoiding
the negative. with a bad habit, we have a _choice_; or rather, a series of
choices that span through hours and days. if we choose to abstain, that is a
win, and we take note of it. if we choose to indulge, that's a loss, and
that's a data point we can work with.

## Terminology

a **tally** is the persistent category of something the user is tempted to do
but is trying to avoid. examples: Nicotine, Alcohol, Video Games, Sweets.

a **choice** is an individual event — the user was tempted and chose to abstain
or indulge. a tally has many choices over time.

## The Choices Screen

the choices screen is a separate screen accessed from the menu. a tap-back
menu allows the user to return to the main screen.

the screen displays:

- a list of tallies, sorted by a blend of priority and recency (see below)
- a create button for adding new tallies

### Tally Row

each row in the tally list shows:

- **tally name**
- **No button** — record an abstain choice
- **Yes button** — record an indulge choice
- **indicator** — abstain count out of the last 10 choices (e.g., "7/10").
  when more than 10 choices have been made in a single day, show actual
  daily counts instead (M/N where N = total choices today)
- **edit button** — navigate to the edit screen

the indicator is color-coded on a green-to-red gradient based on the abstain
ratio. higher abstain ratio trends green; lower trends red.

### Recording a Choice

tapping No or Yes on a tally row immediately records a choice. a choice
captures only:

- timestamp
- abstain or indulge

no notes or additional data. this is designed to be a quick check-in.

### Edit Screen

the edit screen allows the user to modify a tally's name and priority, and
to delete the tally.

### Priority

tallies use the same five-tier priority as habits: high, medium-high, medium,
medium-low, low.

### Sort Order

tallies are sorted by a score that blends priority and recency roughly
equally:

- **priority component** (0.2–1.0): low = 0.2, medium-low = 0.4,
  medium = 0.6, medium-high = 0.8, high = 1.0
- **recency component** (0.0–1.0): this tally's choice count over the last
  7 days, divided by the max choice count of any tally over the same period.
  if no choices have been made by any tally, the recency component is 0
- **score** = priority + recency (range 0.2–2.0). higher scores sort first

## Out of Scope

these features are planned for the future but are not part of the initial
implementation:

- streak counting (days since last indulge)
- visual calendar view of choice history
- tags for grouping habits and tallies in historical analysis features
