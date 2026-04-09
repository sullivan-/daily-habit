# Rename Sort Order to Tie Breaker

## Summary

rename `sortOrder` to `tieBreaker` on the Habit entity and reverse the sense: higher values sort
first (instead of lower values first). this aligns the field name with the UI label and makes the
semantics more intuitive — a higher tie breaker means "more important within the same priority."

## Changes

- rename `Habit.sortOrder` to `Habit.tieBreaker` in the entity, DAO, config loader, editor state,
  and all references
- reverse the sort direction in `DisplayOrdering` — `thenByDescending { it.tieBreaker }` instead
  of `thenBy { it.sortOrder }`
- migration: `ALTER TABLE habit RENAME COLUMN sortOrder TO tieBreaker`
- in habits.json, convert values: `tieBreaker = 5 - sortOrder`
- update ubiquitous language: rename Sort Order entry to Tie Breaker with updated description
