package com.habit.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Habit::class, Activity::class, Tally::class, Choice::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun activityDao(): ActivityDao
    abstract fun tallyDao(): TallyDao
    abstract fun choiceDao(): ChoiceDao

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
    }
}
