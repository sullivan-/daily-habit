package com.habit.data

import android.content.Context
import android.content.res.AssetManager
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import java.io.ByteArrayInputStream
import java.time.DayOfWeek
import org.junit.Test

class ConfigLoaderTest {

    private fun loaderWithJson(json: String): ConfigLoader {
        val mockAssets = mockk<AssetManager>()
        every { mockAssets.open("habits.json") } returns ByteArrayInputStream(json.toByteArray())
        val mockContext = mockk<Context>()
        every { mockContext.assets } returns mockAssets
        return ConfigLoader(mockContext)
    }

    @Test
    fun `loads valid config with all fields`() {
        val config = loaderWithJson("""
        {
            "dayBoundaryHour": 2,
            "habits": [{
                "id": "qigong",
                "name": "Qigong",
                "timesOfDay": [7, 15],
                "sortOrder": 1,
                "daysActive": ["MONDAY", "WEDNESDAY", "FRIDAY"],
                "dailyTarget": 2,
                "dailyTargetMode": "AT_LEAST",
                "timed": true,
                "goalMinutes": 30,
                "stopMinutes": null,
                "priority": "HIGH"
            }]
        }
        """).load()

        assertThat(config.dayBoundaryHour).isEqualTo(2)
        assertThat(config.habits).hasSize(1)

        val habit = config.habits[0]
        assertThat(habit.id).isEqualTo("qigong")
        assertThat(habit.name).isEqualTo("Qigong")
        assertThat(habit.timesOfDay).isEqualTo(listOf(7, 15))
        assertThat(habit.sortOrder).isEqualTo(1)
        assertThat(habit.daysActive).isEqualTo(
            setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY)
        )
        assertThat(habit.dailyTarget).isEqualTo(2)
        assertThat(habit.dailyTargetMode).isEqualTo(TargetMode.AT_LEAST)
        assertThat(habit.timed).isTrue()
        assertThat(habit.goalMinutes).isEqualTo(30)
        assertThat(habit.stopMinutes).isNull()
        assertThat(habit.priority).isEqualTo(Priority.HIGH)
    }

    @Test
    fun `handles optional fields`() {
        val config = loaderWithJson("""
        {
            "dayBoundaryHour": 3,
            "habits": [{
                "id": "vitamins",
                "name": "Vitamins",
                "timesOfDay": [10],
                "sortOrder": 1,
                "daysActive": ["SUNDAY"],
                "dailyTarget": 1,
                "dailyTargetMode": "EXACTLY",
                "timed": false,
                "priority": "MEDIUM"
            }]
        }
        """).load()

        val habit = config.habits[0]
        assertThat(habit.goalMinutes).isNull()
        assertThat(habit.stopMinutes).isNull()
    }

    @Test
    fun `converts all priority values`() {
        val priorities = listOf("HIGH", "MEDIUM_HIGH", "MEDIUM", "MEDIUM_LOW", "LOW")
        priorities.forEach { p ->
            val config = loaderWithJson("""
            {
                "dayBoundaryHour": 2,
                "habits": [{
                    "id": "test",
                    "name": "Test",
                    "timesOfDay": [8],
                    "sortOrder": 1,
                    "daysActive": ["MONDAY"],
                    "dailyTarget": 1,
                    "dailyTargetMode": "EXACTLY",
                    "timed": false,
                    "priority": "$p"
                }]
            }
            """).load()
            assertThat(config.habits[0].priority).isEqualTo(Priority.valueOf(p))
        }
    }

    @Test
    fun `converts all day-of-week values`() {
        val config = loaderWithJson("""
        {
            "dayBoundaryHour": 2,
            "habits": [{
                "id": "test",
                "name": "Test",
                "timesOfDay": [8],
                "sortOrder": 1,
                "daysActive": ["SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY",
                                "THURSDAY", "FRIDAY", "SATURDAY"],
                "dailyTarget": 1,
                "dailyTargetMode": "EXACTLY",
                "timed": false,
                "priority": "MEDIUM"
            }]
        }
        """).load()

        assertThat(config.habits[0].daysActive).isEqualTo(DayOfWeek.entries.toSet())
    }

    @Test
    fun `converts goal and stop minutes`() {
        val config = loaderWithJson("""
        {
            "dayBoundaryHour": 2,
            "habits": [{
                "id": "test",
                "name": "Test",
                "timesOfDay": [8],
                "sortOrder": 1,
                "daysActive": ["MONDAY"],
                "dailyTarget": 1,
                "dailyTargetMode": "AT_LEAST",
                "timed": true,
                "goalMinutes": 15,
                "stopMinutes": 45,
                "priority": "MEDIUM"
            }]
        }
        """).load()
        assertThat(config.habits[0].goalMinutes).isEqualTo(15)
        assertThat(config.habits[0].stopMinutes).isEqualTo(45)
    }

    @Test
    fun `ignores unknown fields in json`() {
        val config = loaderWithJson("""
        {
            "dayBoundaryHour": 2,
            "someUnknownField": true,
            "habits": [{
                "id": "test",
                "name": "Test",
                "timesOfDay": [8],
                "sortOrder": 1,
                "daysActive": ["MONDAY"],
                "dailyTarget": 1,
                "dailyTargetMode": "EXACTLY",
                "timed": false,
                "priority": "LOW",
                "chimeIntervalSeconds": 10
            }]
        }
        """).load()

        assertThat(config.habits).hasSize(1)
    }

    @Test
    fun `multiple habits loaded`() {
        val config = loaderWithJson("""
        {
            "dayBoundaryHour": 2,
            "habits": [
                {
                    "id": "a",
                    "name": "A",
                    "timesOfDay": [8],
                    "sortOrder": 1,
                    "daysActive": ["MONDAY"],
                    "dailyTarget": 1,
                    "dailyTargetMode": "EXACTLY",
                    "timed": false,
                    "priority": "HIGH"
                },
                {
                    "id": "b",
                    "name": "B",
                    "timesOfDay": [12],
                    "sortOrder": 2,
                    "daysActive": ["TUESDAY"],
                    "dailyTarget": 3,
                    "dailyTargetMode": "AT_LEAST",
                    "timed": true,
                    "priority": "LOW"
                }
            ]
        }
        """).load()

        assertThat(config.habits).hasSize(2)
        assertThat(config.habits[0].id).isEqualTo("a")
        assertThat(config.habits[1].id).isEqualTo("b")
    }
}
