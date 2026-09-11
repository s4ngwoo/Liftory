package com.example.presentation.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    val currentSession by viewModel.currentSession.collectAsStateWithLifecycle()
    val sets by viewModel.currentSessionSets.collectAsStateWithLifecycle()
    var showEditorSheet by remember { mutableStateOf(false) }
    var showExerciseSelection by remember { mutableStateOf(false) }
    var selectedExerciseId by remember { mutableStateOf<String?>(null) }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editedNotes by remember(currentSession?.notes) { mutableStateOf(currentSession?.notes ?: "") }

    val exercises by exerciseViewModel.exercises.collectAsStateWithLifecycle()

    // Group sets by exerciseId for Bento Grid display
    val groupedSets = sets.groupBy { it.exerciseId }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentSession?.notes?.ifBlank { "운동 세션" } ?: "운동 세션",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        Text(
                            text = "세션 상세 (Session Detail)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        editedNotes = currentSession?.notes ?: ""
                        showEditDialog = true
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Session")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Session",
                            tint = MaterialTheme.colorScheme.error
                        )
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
        val exerciseName = exercises.find { it.id == selectedExerciseId }?.name ?: "Exercise"
        val existingSets = sets.filter { it.exerciseId == selectedExerciseId }
        val lastSet = existingSets.lastOrNull()
        val lastSummary = if (lastSet != null) {
            "${lastSet.weight} kg × ${lastSet.reps}회" + if (lastSet.rpe != null) " (RPE ${lastSet.rpe})" else ""
        } else null

        ExerciseSetEditorSheet(
            exerciseName = exerciseName,
            setNumber = existingSets.size + 1,
            lastSetSummary = lastSummary,
            onDismissRequest = { showEditorSheet = false },
            onSaveSet = { weight, reps, rpe ->
                selectedExerciseId?.let { viewModel.addSet(it, weight, reps, rpe) }
                showEditorSheet = false
            }
        )
    }

    // Edit Session Notes Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("세션 이름/메모 수정", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editedNotes,
                    onValueChange = { editedNotes = it },
                    label = { Text("세션 이름 또는 메모") },
                    placeholder = { Text("예: 가슴 & 삼두 루틴, 하체 폭파 등") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        currentSession?.let { session ->
                            viewModel.updateSessionNotes(session.id, editedNotes.trim())
                        }
                        showEditDialog = false
                    }
                ) {
                    Text("저장")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    // Delete Session Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("세션 삭제", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = {
                Text("정말 이 운동 세션을 삭제하시겠습니까?\n기록된 모든 세트 데이터가 함께 삭제되며 복구할 수 없습니다.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        currentSession?.let { session ->
                            viewModel.deleteSession(session.id) {
                                onBack()
                            }
                        }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소")
                }
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
                Text("RPE", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
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
                    Text(
                        text = set.rpe?.let { "$it" } ?: "-",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
