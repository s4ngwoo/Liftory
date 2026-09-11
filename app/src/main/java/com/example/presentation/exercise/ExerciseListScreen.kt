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
    var selectedCategory by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    var viewingExercise by remember { mutableStateOf<Exercise?>(null) }

    val categories = listOf(
        "All" to "전체",
        "Chest" to "가슴",
        "Back" to "등",
        "Legs" to "하체",
        "Shoulders" to "어깨",
        "Arms" to "팔"
    )

    val filteredExercises = remember(exercises, searchQuery, selectedCategory) {
        exercises.filter { exercise ->
            val matchesCategory = selectedCategory == "All" || exercise.muscleGroup.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() || exercise.name.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("운동 라이브러리 (Exercises)", fontWeight = FontWeight.Bold) },
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
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    viewModel.searchExercises(it) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("운동 종목 검색...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            // Category filter chips
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories.size) { index ->
                    val (key, label) = categories[index]
                    FilterChip(
                        selected = selectedCategory == key,
                        onClick = { selectedCategory = key },
                        label = { Text(label) }
                    )
                }
            }

            // Exercise list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (filteredExercises.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "일치하는 운동 종목이 없습니다",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "새로운 커스텀 운동을 추가하거나 검색어를 변경해보세요.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Button(onClick = { showAddDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("새 종목 등록하기")
                                }
                            }
                        }
                    }
                } else {
                    items(filteredExercises) { exercise ->
                        ExerciseCard(
                            exercise = exercise,
                            onClick = { 
                                if (onExerciseSelected != null) {
                                    onExerciseSelected(exercise)
                                } else {
                                    viewingExercise = exercise
                                }
                            }
                        )
                    }
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

    viewingExercise?.let { exercise ->
        AlertDialog(
            onDismissRequest = { viewingExercise = null },
            title = { Text(exercise.name, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("타겟 부위: ${exercise.muscleGroup}", style = MaterialTheme.typography.bodyMedium)
                    Text("유형: ${if (exercise.isCustom) "사용자 지정 운동" else "기본 제공 운동"}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "세션 화면에서 '+' 버튼을 눌러 이 운동의 무게와 횟수를 기록할 수 있습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewingExercise = null }) {
                    Text("닫기")
                }
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
    var selectedGroup by remember { mutableStateOf("Chest") }
    val muscleGroups = listOf(
        "Chest" to "가슴",
        "Back" to "등",
        "Legs" to "하체",
        "Shoulders" to "어깨",
        "Arms" to "팔",
        "Core" to "복근"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 운동 종목 추가") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("운동 이름 (예: 인클라인 벤치프레스)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("타겟 부위 선택:", style = MaterialTheme.typography.labelMedium)
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(muscleGroups.size) { index ->
                        val (key, label) = muscleGroups[index]
                        FilterChip(
                            selected = selectedGroup == key,
                            onClick = { selectedGroup = key },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, selectedGroup) },
                enabled = name.isNotBlank()
            ) {
                Text("추가하기")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}
