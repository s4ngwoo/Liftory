package com.example.presentation.exercise

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Exercise

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseListScreen(
    viewModel: ExerciseViewModel,
    modifier: Modifier = Modifier,
    onExerciseSelected: ((Exercise) -> Unit)? = null
) {
    val exercises by viewModel.exercises.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Exercises", fontWeight = FontWeight.Bold) },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Custom Exercise")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    viewModel.searchExercises(it) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search exercises...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(exercises) { exercise ->
                    ExerciseCard(exercise = exercise, onClick = { onExerciseSelected?.invoke(exercise) })
                }
            }
        }
    }

    if (showAddDialog) {
        AddExerciseDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, group ->
                viewModel.createCustomExercise(name, group)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ExerciseCard(exercise: Exercise, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(exercise.name, fontWeight = FontWeight.Bold)
                Text(
                    text = exercise.muscleGroup,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (exercise.isCustom) {
                Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                    Text("Custom", color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExerciseDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, muscleGroup: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("All") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Exercise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Exercise Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = group,
                    onValueChange = { group = it },
                    label = { Text("Muscle Group") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, group) },
                enabled = name.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
