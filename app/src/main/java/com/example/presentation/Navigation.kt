package com.example.presentation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.di.AppContainer
import com.example.presentation.exercise.ExerciseListScreen
import com.example.presentation.exercise.ExerciseViewModel
import com.example.presentation.routine.RoutineTemplateListScreen
import com.example.presentation.routine.RoutineTemplateViewModel
import com.example.presentation.scaffold.StrengthLogScaffoldScreen
import com.example.presentation.scaffold.StrengthLogScaffoldViewModel
import com.example.presentation.session.WorkoutSessionDetailScreen
import com.example.presentation.session.WorkoutSessionViewModel
import com.example.presentation.statistics.StatisticsDashboardScreen
import com.example.presentation.statistics.StatisticsViewModel
import com.example.presentation.timer.RestTimerScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    appContainer: AppContainer
) {
    NavHost(
        navController = navController,
        startDestination = "scaffold"
    ) {
        composable("scaffold") {
            val scaffoldViewModel = viewModel<StrengthLogScaffoldViewModel>(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return StrengthLogScaffoldViewModel(
                            appContainer.observeWorkoutSessionsUseCase,
                            appContainer.observeExercisesUseCase,
                            appContainer.createWorkoutSessionUseCase,
                            appContainer.syncQueueRepository, appContainer.startSyncWorkUseCase
                        ) as T
                    }
                }
            )
            StrengthLogScaffoldScreen(
                viewModel = scaffoldViewModel
            )
        }

        composable("session/{sessionId}") { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            val sessionViewModel = viewModel<WorkoutSessionViewModel>(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return WorkoutSessionViewModel(
                            appContainer.observeWorkoutSessionsUseCase,
                            appContainer.observeExerciseSetsUseCase,
                            appContainer.createWorkoutSessionUseCase,
                            appContainer.addExerciseSetUseCase
                        ) as T
                    }
                }
            )
            WorkoutSessionDetailScreen(
                viewModel = sessionViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
