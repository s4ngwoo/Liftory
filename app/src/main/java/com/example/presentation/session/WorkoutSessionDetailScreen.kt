package com.example.presentation.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.ExerciseSet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSessionDetailScreen(
    exerciseViewModel: com.example.presentation.exercise.ExerciseViewModel,
    viewModel: WorkoutSessionViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sets by viewModel.currentSessionSets.collectAsStateWithLifecycle()
    var showEditorSheet by remember { mutableStateOf(false) }
    var showExerciseSelection by remember { mutableStateOf(false) }
    var selectedExerciseId by remember { mutableStateOf<String?>(null) }

    val exercises by exerciseViewModel.exercises.collectAsStateWithLifecycle()

    // Group sets by exerciseId for Bento Grid display
    val groupedSets = sets.groupBy { it.exerciseId }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("세션 상세 (Session)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showExerciseSelection = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Set")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (groupedSets.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "기록된 운동이 없습니다",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "우측 하단의 + 버튼을 눌러 운동 종목을 선택하고 세트를 기록해보세요!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(onClick = { showExerciseSelection = true }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("운동 추가하기")
                            }
                        }
                    }
                }
            } else {
                groupedSets.forEach { (exerciseId, exerciseSets) ->
                    item {
                        val exerciseName = exercises.find { it.id == exerciseId }?.name ?: "Exercise: $exerciseId"
                        ExerciseGroupCard(exerciseName = exerciseName, sets = exerciseSets)
                    }
                }
            }
        }
    }

    if (showExerciseSelection) {
        ExerciseSelectionSheet(
            viewModel = exerciseViewModel,
            onExerciseSelected = { exercise ->
                selectedExerciseId = exercise.id
                showExerciseSelection = false
                showEditorSheet = true
            },
            onDismissRequest = { showExerciseSelection = false }
        )
    }

    if (showEditorSheet) {
        ExerciseSetEditorSheet(
            onDismissRequest = { showEditorSheet = false },
            onSaveSet = { weight, reps, rpe ->
                selectedExerciseId?.let { viewModel.addSet(it, weight, reps, rpe) }
                showEditorSheet = false
            }
        )
    }
}

@Composable
fun ExerciseGroupCard(exerciseName: String, sets: List<ExerciseSet>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = exerciseName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Set", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                Text("Weight", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                Text("Reps", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            }
            
            sets.forEachIndexed { index, set ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${set.weight} kg",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${set.reps}",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
