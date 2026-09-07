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

    // Group sets by exerciseId for Bento Grid display
    val groupedSets = sets.groupBy { it.exerciseId }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Session Details", fontWeight = FontWeight.Bold) },
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
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
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
                    Text(
                        text = "No exercises yet. Click + to add a set.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                groupedSets.forEach { (exerciseId, exerciseSets) ->
                    item {
                        ExerciseGroupCard(exerciseId = exerciseId, sets = exerciseSets)
                    }
                }
            }
        }
    }

    if (showEditorSheet) {
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
        ExerciseSetEditorSheet(
            onDismissRequest = { showEditorSheet = false },
            onSaveSet = { weight, reps, rpe ->
                // Basic stub for exerciseId. In Sprint 2, this will be selected via an Exercise Picker.
                selectedExerciseId?.let { viewModel.addSet(it, weight, reps, rpe) }
                showEditorSheet = false
            }
        )
    }
}

@Composable
fun ExerciseGroupCard(exerciseId: String, sets: List<ExerciseSet>) {
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
                text = "Exercise: $exerciseId",
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
