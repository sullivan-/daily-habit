package com.habit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.habit.service.ChimePlayer
import com.habit.service.TimerService
import com.habit.ui.PrimaryScreen
import com.habit.viewmodel.AgendaViewModel
import com.habit.viewmodel.AgendaViewModelFactory
import com.habit.viewmodel.ChimeEvent
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: AgendaViewModel by viewModels {
        AgendaViewModelFactory((application as HabitApp).container)
    }

    private lateinit var chimePlayer: ChimePlayer
    private var serviceRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                android.graphics.Color.TRANSPARENT
            )
        )
        super.onCreate(savedInstanceState)

        chimePlayer = ChimePlayer()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.chimeEvents.collect { event ->
                    when (event) {
                        is ChimeEvent.Interval -> chimePlayer.playIntervalChime()
                        is ChimeEvent.Threshold -> chimePlayer.playThresholdChime()
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                if (state.timerRunning && !serviceRunning) {
                    val habit = state.selectedHabit ?: return@collect
                    startForegroundService(TimerService.startIntent(
                        context = this@MainActivity,
                        habitName = habit.name,
                        accumulatedMs = state.elapsedMs,
                        chimeIntervalMs = (habit.chimeIntervalSeconds ?: 0) * 1000L,
                        thresholdMs = (habit.thresholdMinutes ?: 0) * 60 * 1000L
                    ))
                    serviceRunning = true
                } else if (!state.timerRunning && serviceRunning) {
                    startService(TimerService.stopIntent(this@MainActivity))
                    serviceRunning = false
                }
            }
        }

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Scaffold { innerPadding ->
                    PrimaryScreen(
                        viewModel,
                        modifier = Modifier
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background)
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::chimePlayer.isInitialized) {
            chimePlayer.release()
        }
    }
}
