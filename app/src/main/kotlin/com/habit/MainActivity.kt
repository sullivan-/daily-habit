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
