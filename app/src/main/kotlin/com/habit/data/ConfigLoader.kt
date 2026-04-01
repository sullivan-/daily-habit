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
    val chimeIntervalSeconds: Int? = null,
    val thresholdMinutes: Int? = null,
    val thresholdType: String? = null,
    val priority: String,
    val dailyTexts: Map<String, String> = emptyMap()
)

@Serializable
data class AppConfigJson(
    val dayBoundaryHour: Int,
    val habits: List<HabitJson>
)

data class AppConfig(
    val dayBoundaryHour: Int,
    val habits: List<Habit>
)

class ConfigLoader(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    fun load(): AppConfig {
        val text = context.assets.open("habits.json")
            .bufferedReader()
            .use { it.readText() }
        val raw = json.decodeFromString<AppConfigJson>(text)
        return AppConfig(
            dayBoundaryHour = raw.dayBoundaryHour,
            habits = raw.habits.map { it.toHabit() }
        )
    }

    private fun HabitJson.toHabit() = Habit(
        id = id,
        name = name,
        timesOfDay = timesOfDay,
        sortOrder = sortOrder,
        daysActive = daysActive.map { DayOfWeek.valueOf(it) }.toSet(),
        dailyTarget = dailyTarget,
        dailyTargetMode = TargetMode.valueOf(dailyTargetMode),
        timed = timed,
        chimeIntervalSeconds = chimeIntervalSeconds,
        thresholdMinutes = thresholdMinutes,
        thresholdType = thresholdType?.let { ThresholdType.valueOf(it) },
        priority = Priority.valueOf(priority),
        dailyTexts = dailyTexts.mapKeys { DayOfWeek.valueOf(it.key) }
    )
}
