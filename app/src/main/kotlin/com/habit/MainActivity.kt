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
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
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

        val container = (application as HabitApp).container

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
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
