package com.example.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.di.AppContainer
import com.example.presentation.exercise.ExerciseListScreen
import com.example.presentation.exercise.ExerciseViewModel
import com.example.presentation.session.WorkoutSessionListScreen
import com.example.presentation.session.WorkoutSessionViewModel
import com.example.presentation.statistics.StatisticsDashboardScreen
import com.example.presentation.statistics.StatisticsViewModel

@Composable
fun MainScreen(
    appContainer: AppContainer,
    onNavigateToSessionDetail: (String) -> Unit
) {
    val bottomNavController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                NavigationBarItem(
                    selected = currentRoute == "sessions",
                    onClick = {
                        bottomNavController.navigate("sessions") {
                            popUpTo(bottomNavController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    },
                    icon = { Icon(Icons.Default.List, contentDescription = "Sessions") },
                    label = { Text("Sessions") }
                )
                NavigationBarItem(
                    selected = currentRoute == "routines",
                    onClick = {
                        bottomNavController.navigate("routines") {
                            popUpTo(bottomNavController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    },
                    icon = { Icon(Icons.Default.FitnessCenter, contentDescription = "Routines") },
                    label = { Text("Routines") }
                )
                NavigationBarItem(
                    selected = currentRoute == "exercises",
                    onClick = {
                        bottomNavController.navigate("exercises") {
                            popUpTo(bottomNavController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    },
                    icon = { Icon(Icons.Default.FitnessCenter, contentDescription = "Exercises") },
                    label = { Text("Exercises") }
                )
                NavigationBarItem(
                    selected = currentRoute == "stats",
                    onClick = {
                        bottomNavController.navigate("stats") {
                            popUpTo(bottomNavController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Stats") },
                    label = { Text("Stats") }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = "sessions",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("sessions") {
                val sessionViewModel = viewModel<WorkoutSessionViewModel>(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return WorkoutSessionViewModel(
                                appContainer.observeWorkoutSessionsUseCase,
                                appContainer.observeExerciseSetsUseCase,
                                appContainer.createWorkoutSessionUseCase,
                                appContainer.addExerciseSetUseCase,
                                appContainer.updateWorkoutSessionUseCase,
                                appContainer.deleteWorkoutSessionUseCase,
                                appContainer.getWorkoutSessionUseCase
                            ) as T
                        }
                    }
                )
                WorkoutSessionListScreen(
                    viewModel = sessionViewModel,
                    onNavigateToDetail = onNavigateToSessionDetail
                )
            }
            composable("routines") {
                val routineViewModel = viewModel<com.example.presentation.routine.RoutineTemplateViewModel>(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return com.example.presentation.routine.RoutineTemplateViewModel(
                                appContainer.observeRoutineTemplatesUseCase,
                                appContainer.createRoutineTemplateUseCase,
                                appContainer.createWorkoutSessionUseCase,
                                appContainer.applyRoutineTemplateUseCase,
                                appContainer.deleteRoutineTemplateUseCase
                            ) as T
                        }
                    }
                )
                val exerciseViewModel = viewModel<ExerciseViewModel>(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return ExerciseViewModel(
                                appContainer.observeExercisesUseCase,
                                appContainer.createExerciseUseCase,
                                appContainer.searchExercisesUseCase
                            ) as T
                        }
                    }
                )
                com.example.presentation.routine.RoutineTemplateListScreen(
                    viewModel = routineViewModel,
                    exerciseViewModel = exerciseViewModel,
                    onApplyTemplate = { templateId ->
                        routineViewModel.applyTemplate(templateId) { newSessionId ->
                            onNavigateToSessionDetail(newSessionId)
                        }
                    }
                )
            }
            composable("exercises") {
                val exerciseViewModel = viewModel<ExerciseViewModel>(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return ExerciseViewModel(
                                appContainer.observeExercisesUseCase,
                                appContainer.createExerciseUseCase,
                                appContainer.searchExercisesUseCase
                            ) as T
                        }
                    }
                )
                ExerciseListScreen(
                    viewModel = exerciseViewModel,
                    onExerciseSelected = null
                )
            }
            composable("stats") {
                val statsViewModel = viewModel<StatisticsViewModel>(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return StatisticsViewModel(
                                appContainer.calculateWorkoutVolumeUseCase,
                                appContainer.calculatePersonalRecordsUseCase,
                                appContainer.exportWorkoutDataUseCase,
                                appContainer.importWorkoutDataUseCase
                            ) as T
                        }
                    }
                )
                StatisticsDashboardScreen(viewModel = statsViewModel, onExportJson = {}, onExportCsv = {})
            }
        }
    }
}
