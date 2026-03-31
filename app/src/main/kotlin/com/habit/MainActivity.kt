package com.habit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import com.habit.ui.PrimaryScreen
import com.habit.viewmodel.AgendaViewModel
import com.habit.viewmodel.AgendaViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: AgendaViewModel by viewModels {
        AgendaViewModelFactory((application as HabitApp).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PrimaryScreen(viewModel)
            }
        }
    }
}
