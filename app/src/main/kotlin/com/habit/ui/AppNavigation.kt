package com.habit.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.habit.AppContainer
import com.habit.viewmodel.AgendaViewModel
import com.habit.viewmodel.ChoicesViewModel
import com.habit.viewmodel.ChoicesViewModelFactory
import com.habit.viewmodel.HabitEditorViewModel
import com.habit.viewmodel.HabitEditorViewModelFactory
import com.habit.viewmodel.TallyEditorViewModel
import com.habit.viewmodel.TallyEditorViewModelFactory

@Composable
fun AppNavigation(
    navController: NavHostController,
    agendaViewModel: AgendaViewModel,
    container: AppContainer,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "main",
        modifier = modifier
    ) {
        composable("main") {
            PrimaryScreen(
                viewModel = agendaViewModel,
                onNewHabit = { navController.navigate("habit-editor/new") },
                onEditHabit = { habitId ->
                    navController.navigate("habit-editor/$habitId")
                },
                onHabitList = { navController.navigate("habit-list") },
                onChoices = { navController.navigate("choices") }
            )
        }
        composable("habit-list") {
            HabitListScreen(
                viewModel = agendaViewModel,
                onEditHabit = { habitId ->
                    navController.navigate("habit-editor/$habitId")
                },
                onNewHabit = { navController.navigate("habit-editor/new") },
                onBack = { navController.popBackStack() }
            )
        }
        composable("habit-editor/{habitId}") { backStackEntry ->
            val habitId = backStackEntry.arguments?.getString("habitId")
            val editorVm: HabitEditorViewModel = viewModel(
                factory = HabitEditorViewModelFactory(container)
            )
            HabitEditorScreen(
                viewModel = editorVm,
                habitId = if (habitId == "new") null else habitId,
                onBack = { navController.popBackStack() }
            )
        }
        composable("choices") {
            val choicesVm: ChoicesViewModel = viewModel(
                factory = ChoicesViewModelFactory(container)
            )
            ChoicesScreen(
                viewModel = choicesVm,
                onEditTally = { tallyId ->
                    navController.navigate("tally-editor/$tallyId")
                },
                onNewTally = { navController.navigate("tally-editor/new") },
                onBack = { navController.popBackStack() }
            )
        }
        composable("tally-editor/{tallyId}") { backStackEntry ->
            val tallyIdStr = backStackEntry.arguments?.getString("tallyId")
            val editorVm: TallyEditorViewModel = viewModel(
                factory = TallyEditorViewModelFactory(container)
            )
            TallyEditorScreen(
                viewModel = editorVm,
                tallyId = if (tallyIdStr == "new") null
                    else tallyIdStr?.toLongOrNull(),
                onBack = { navController.popBackStack() }
            )
        }
    }
}
