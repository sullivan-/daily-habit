package com.habit.data

import android.content.Context
import java.time.DayOfWeek
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class HabitJson(
    val id: String,
    val name: String,
    val timesOfDay: List<Int>,
    val sortOrder: Int,
    val daysActive: List<String>,
    val dailyTarget: Int,
    val dailyTargetMode: String,
    val timed: Boolean,
    val goalMinutes: Int? = null,
    val stopMinutes: Int? = null,
    val priority: String
)

@Serializable
data class TallyJson(
    val id: String,
    val name: String,
    val priority: String
)

@Serializable
data class TrackJson(
    val id: String,
    val habitId: String,
    val name: String,
    val priority: String,
    val dayOfWeek: String? = null,
    val milestones: List<String> = emptyList(),
    val completedMilestones: Int = 0
)

@Serializable
data class AppConfigJson(
    val dayBoundaryHour: Int,
    val habits: List<HabitJson>,
    val tallies: List<TallyJson> = emptyList(),
    val tracks: List<TrackJson> = emptyList()
)

data class AppConfig(
    val dayBoundaryHour: Int,
    val habits: List<Habit>,
    val tallies: List<Tally>,
    val tracks: List<Track>,
    val milestones: Map<String, List<Milestone>>
)

class ConfigLoader(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    fun load(): AppConfig {
        val text = context.assets.open("habits.json")
            .bufferedReader()
            .use { it.readText() }
        val raw = json.decodeFromString<AppConfigJson>(text)
        val tracks = raw.tracks.map { it.toTrack() }
        val milestones = raw.tracks
            .filter { it.milestones.isNotEmpty() }
            .associate { t ->
                t.id to t.milestones.mapIndexed { i, name ->
                    Milestone(
                        trackId = t.id,
                        name = name,
                        sortOrder = i + 1,
                        completed = i < t.completedMilestones
                    )
                }
            }
        return AppConfig(
            dayBoundaryHour = raw.dayBoundaryHour,
            habits = raw.habits.map { it.toHabit() },
            tallies = raw.tallies.map { it.toTally() },
            tracks = tracks,
            milestones = milestones
        )
    }

    private fun TallyJson.toTally() = Tally(
        id = id,
        name = name,
        priority = Priority.valueOf(priority)
    )

    private fun TrackJson.toTrack() = Track(
        id = id,
        habitId = habitId,
        name = name,
        priority = Priority.valueOf(priority),
        dayOfWeek = dayOfWeek?.let { DayOfWeek.valueOf(it) }
    )

    private fun HabitJson.toHabit() = Habit(
        id = id,
        name = name,
        timesOfDay = timesOfDay,
        sortOrder = sortOrder,
        daysActive = daysActive.map { DayOfWeek.valueOf(it) }.toSet(),
        dailyTarget = dailyTarget,
        dailyTargetMode = TargetMode.valueOf(dailyTargetMode),
        timed = timed,
        goalMinutes = goalMinutes,
        stopMinutes = stopMinutes,
        priority = Priority.valueOf(priority)
    )
}
