# Editable Activity Times

## Summary

start and completed times on activities are tappable and editable via a standard Android time
picker. just time, not date — the activity's attributed date is fixed.

## Rules

- **start time** — editable on started (in-progress) and completed activities
- **completed time** — editable on completed activities only
- **constraints**: start < now, completed < now, start < completed
- if the user picks a time that violates a constraint, clamp to the nearest valid value

## UI

- start and completed times display in primary color to indicate they're tappable
- tapping opens a Material 3 `TimePicker` dialog, pre-filled with the current value
- OK confirms, Cancel dismisses, tap-outside dismisses
- duration (shown only on completed activities) updates automatically after editing either time

## Implementation Note

this is already partially implemented — `EditableActivityTimes` composable and
`ActivityTimePicker` exist in `ActivityView.kt`, wired to `updateActivityStartTime` and
`updateActivityCompletedAt` on the ViewModel. the time picker and clamping logic are in place.
what remains is verifying edge cases and adding test coverage.
