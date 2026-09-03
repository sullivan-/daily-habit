package com.habit

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.rememberNavController
import com.habit.service.ChimePlayer
import com.habit.service.TimerService
import com.habit.ui.AppNavigation
import com.habit.viewmodel.AgendaViewModel
import com.habit.viewmodel.AgendaViewModelFactory
import com.habit.viewmodel.ChimeEvent
import com.habit.viewmodel.IntervalOptions
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: AgendaViewModel by viewModels {
        AgendaViewModelFactory((application as HabitApp).container)
    }

    private lateinit var chimePlayer: ChimePlayer
    private var serviceRunning = false

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                android.graphics.Color.TRANSPARENT
            )
        )
        super.onCreate(savedInstanceState)

        requestNotificationPermission()
        chimePlayer = ChimePlayer(this)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.refreshToday()
                viewModel.chimeEvents.collect { event ->
                    when (event) {
                        is ChimeEvent.Goal -> chimePlayer.playGoalChime()
                        is ChimeEvent.Stop -> chimePlayer.playStopChime()
                    }
                }
            }
        }

        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                if (state.timerRunning && !serviceRunning) {
                    val habit = state.selectedHabit ?: return@collect
                    val startEpochMs = state.activeActivity?.startTime
                        ?.toEpochMilli() ?: System.currentTimeMillis()
                    startForegroundService(TimerService.startIntent(
                        context = this@MainActivity,
                        habitName = habit.name,
                        startEpochMs = startEpochMs,
                        goalMs = habit.goalMinutes?.let { it * 60 * 1000L } ?: 0L,
                        stopMs = habit.stopMinutes?.let { it * 60 * 1000L } ?: 0L
                    ))
                    serviceRunning = true
                } else if (!state.timerRunning && serviceRunning) {
                    startService(TimerService.stopIntent(this@MainActivity))
                    serviceRunning = false
                }
            }
        }

        lifecycleScope.launch {
            var sentIntervalMs = 0L
            viewModel.uiState.collect { state ->
                val wantedMs = state.intervalChimeMs
                if (wantedMs > 0 && serviceRunning && sentIntervalMs == 0L) {
                    startService(TimerService.startIntervalIntent(
                        context = this@MainActivity,
                        intervalMs = wantedMs,
                        currentElapsedMs = state.activeActivity?.elapsedMs ?: 0,
                        isSeconds = IntervalOptions.isSecondsInterval(wantedMs)
                    ))
                    sentIntervalMs = wantedMs
                } else if (wantedMs > 0 && serviceRunning && wantedMs != sentIntervalMs) {
                    startService(TimerService.changeIntervalIntent(
                        context = this@MainActivity,
                        intervalMs = wantedMs,
                        isSeconds = IntervalOptions.isSecondsInterval(wantedMs)
                    ))
                    sentIntervalMs = wantedMs
                } else if (wantedMs == 0L && sentIntervalMs != 0L) {
                    startService(
                        TimerService.stopIntervalIntent(this@MainActivity)
                    )
                    sentIntervalMs = 0L
                }
            }
        }

        val container = (application as HabitApp).container

        setContent {
            val defaultTypography = Typography()
            val typography = defaultTypography.copy(
                bodySmall = defaultTypography.bodySmall.copy(fontSize = 13.sp)
            )
            MaterialTheme(colorScheme = darkColorScheme(), typography = typography) {
                val navController = rememberNavController()
                Scaffold { innerPadding ->
                    AppNavigation(
                        navController = navController,
                        agendaViewModel = viewModel,
                        container = container,
                        modifier = Modifier
                            .padding(innerPadding)
                            .background(MaterialTheme.colorScheme.background)
                    )
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
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
