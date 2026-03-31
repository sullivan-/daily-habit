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
import com.habit.ui.PrimaryScreen
import com.habit.viewmodel.AgendaViewModel
import com.habit.viewmodel.AgendaViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: AgendaViewModel by viewModels {
        AgendaViewModelFactory((application as HabitApp).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(
                android.graphics.Color.TRANSPARENT
            )
        )
        super.onCreate(savedInstanceState)
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
}
