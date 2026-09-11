package com.example.presentation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.di.AppContainer
import com.example.presentation.session.WorkoutSessionDetailScreen
import com.example.presentation.session.WorkoutSessionViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    appContainer: AppContainer
) {
    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            val authViewModel = viewModel<com.example.presentation.auth.AuthViewModel>(
                factory = com.example.presentation.auth.AuthViewModel.Factory(appContainer.authRepository)
            )
            com.example.presentation.auth.LoginScreen(
                viewModel = authViewModel,
                onNavigateToHome = {
                    navController.navigate("main") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("main") {
            com.example.presentation.MainScreen(
                appContainer = appContainer,
                onNavigateToSessionDetail = { sessionId ->
                    navController.navigate("session/$sessionId")
                }
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
            val exerciseViewModel = viewModel<com.example.presentation.exercise.ExerciseViewModel>(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return com.example.presentation.exercise.ExerciseViewModel(
                            appContainer.observeExercisesUseCase,
                            appContainer.createExerciseUseCase,
                            appContainer.searchExercisesUseCase
                        ) as T
                    }
                }
            )
            WorkoutSessionDetailScreen(
                viewModel = sessionViewModel,
                exerciseViewModel = exerciseViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
