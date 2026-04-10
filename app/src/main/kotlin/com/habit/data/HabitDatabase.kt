package com.habit.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Habit::class, Activity::class, Tally::class, Choice::class,
        Track::class, Milestone::class, AppConfigEntity::class
    ],
    version = 10,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun activityDao(): ActivityDao
    abstract fun tallyDao(): TallyDao
    abstract fun choiceDao(): ChoiceDao
    abstract fun trackDao(): TrackDao
    abstract fun milestoneDao(): MilestoneDao
    abstract fun appConfigDao(): AppConfigDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE activity DROP COLUMN endTime")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE activity DROP COLUMN elapsedMs")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habit DROP COLUMN chimeIntervalSeconds")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS tally (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        priority TEXT NOT NULL
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS choice (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        tallyId INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL,
                        abstained INTEGER NOT NULL,
                        FOREIGN KEY (tallyId) REFERENCES tally(id)
                            ON DELETE CASCADE
                    )
                """)
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_choice_tallyId ON choice(tallyId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_choice_timestamp ON choice(timestamp)"
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS tally_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        priority TEXT NOT NULL
                    )
                """)
                db.execSQL("""
                    INSERT INTO tally_new (id, name, priority)
                    SELECT CAST(id AS TEXT), name, priority FROM tally
                """)
                db.execSQL("DROP TABLE tally")
                db.execSQL("ALTER TABLE tally_new RENAME TO tally")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS choice_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        tallyId TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        abstained INTEGER NOT NULL,
                        FOREIGN KEY (tallyId) REFERENCES tally(id)
                            ON DELETE CASCADE
                    )
                """)
                db.execSQL("""
                    INSERT INTO choice_new (id, tallyId, timestamp, abstained)
                    SELECT id, CAST(tallyId AS TEXT), timestamp, abstained FROM choice
                """)
                db.execSQL("DROP TABLE choice")
                db.execSQL("ALTER TABLE choice_new RENAME TO choice")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_choice_tallyId ON choice(tallyId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_choice_timestamp ON choice(timestamp)"
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE habit RENAME COLUMN thresholdMinutes TO goalMinutes"
                )
                db.execSQL("ALTER TABLE habit ADD COLUMN stopMinutes INTEGER")
                db.execSQL("""
                    UPDATE habit SET stopMinutes = goalMinutes, goalMinutes = NULL
                    WHERE thresholdType = 'TIME_TO_STOP'
                """)
                db.execSQL("ALTER TABLE habit DROP COLUMN thresholdType")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS track (
                        id TEXT NOT NULL PRIMARY KEY,
                        habitId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        priority TEXT NOT NULL,
                        dayOfWeek TEXT,
                        archived INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (habitId) REFERENCES habit(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_track_habitId ON track(habitId)"
                )

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS milestone (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        trackId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL,
                        completed INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (trackId) REFERENCES track(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_milestone_trackId ON milestone(trackId)"
                )

                db.execSQL("ALTER TABLE activity ADD COLUMN trackId TEXT")
                db.execSQL("ALTER TABLE activity ADD COLUMN milestoneId INTEGER")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_activity_trackId ON activity(trackId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_activity_milestoneId " +
                    "ON activity(milestoneId)"
                )

                db.execSQL("ALTER TABLE habit DROP COLUMN dailyTexts")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS app_config (
                        id INTEGER NOT NULL PRIMARY KEY,
                        dayBoundaryHour INTEGER NOT NULL
                    )
                """)
                db.execSQL(
                    "INSERT INTO app_config (id, dayBoundaryHour) VALUES (1, 2)"
                )
            }
        }
    }
}
