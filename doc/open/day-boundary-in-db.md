# Day Boundary in Database

## Summary

store `dayBoundaryHour` in the database instead of reading it from habits.json on every start.
the "is database empty" check should be based on whether this value exists, not on the habit
count. habits.json is only read when the database has no config row.

## Changes

- new `app_config` table with a single row: `key TEXT PRIMARY KEY, value TEXT`
- store `dayBoundaryHour` as a row in this table on first load
- `DayBoundary` reads from the database, not from the config loader
- the seed check becomes: if `app_config` table is empty, load everything from JSON
- migration to create the `app_config` table and seed the current value
- remove `dayBoundaryHour` from `AppConfig` runtime usage (still parsed from JSON for seeding)
