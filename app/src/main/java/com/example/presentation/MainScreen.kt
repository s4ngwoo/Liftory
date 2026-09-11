package com.example.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.di.AppContainer
import com.example.domain.model.WorkoutSession
import com.example.domain.util.SessionNotesManager
import com.example.presentation.exercise.ExerciseListScreen
import com.example.presentation.exercise.ExerciseViewModel
import com.example.presentation.session.WorkoutSessionListScreen
import com.example.presentation.session.WorkoutSessionViewModel
import com.example.presentation.statistics.StatisticsDashboardScreen
import com.example.presentation.statistics.StatisticsViewModel
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    appContainer: AppContainer,
    onNavigateToSessionDetail: (String) -> Unit
) {
    val bottomNavController = rememberNavController()
    val activeSession by appContainer.observeActiveWorkoutSessionUseCase().collectAsStateWithLifecycle(initialValue = null)

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = activeSession != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                activeSession?.let { session ->
                    ActiveWorkoutBanner(
                        activeSession = session,
                        onClick = { onNavigateToSessionDetail(session.id) }
                    )
                }
            }
        },
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
                    icon = { Icon(Icons.AutoMirrored.Filled.ListAlt, contentDescription = "Sessions") },
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
                                appContainer.getWorkoutSessionUseCase,
                                appContainer.getLastExerciseHistoryUseCase
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
                                appContainer.deleteRoutineTemplateUseCase,
                                appContainer.updateRoutineTemplateUseCase,
                                appContainer.observeWorkoutSessionsUseCase
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
                    },
                    onNavigateToSessionDetail = onNavigateToSessionDetail
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

@Composable
fun ActiveWorkoutBanner(
    activeSession: WorkoutSession,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(activeSession.id) {
        while (true) {
            delay(1000L)
            nowMillis = System.currentTimeMillis()
        }
    }
    val elapsedSeconds = ((nowMillis - activeSession.startTime).coerceAtLeast(0L) / 1000L).toInt()
    val elapsedHours = elapsedSeconds / 3600
    val elapsedMinutes = (elapsedSeconds % 3600) / 60
    val elapsedSecs = elapsedSeconds % 60
    val elapsedFormatted = if (elapsedHours > 0) {
        "%02d:%02d:%02d".format(elapsedHours, elapsedMinutes, elapsedSecs)
    } else {
        "%02d:%02d".format(elapsedMinutes, elapsedSecs)
    }

    val title = remember(activeSession.notes) {
        val parsed = SessionNotesManager.getSessionTitle(activeSession.notes)
        if (parsed.isNotBlank()) parsed else "자유 운동"
    }

    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "운동 진행 중",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "⏱️ $elapsedFormatted",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            FilledTonalButton(
                onClick = onClick,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("운동 복귀", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
            }
        }
    }
}

