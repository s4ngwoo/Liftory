package com.example.presentation.exercise

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.EquipmentType
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
    var selectedEquipmentFilter by remember { mutableStateOf<EquipmentType?>(null) } // null = All
    var showAddDialog by remember { mutableStateOf(false) }
    var viewingExercise by remember { mutableStateOf<Exercise?>(null) }

    val categories = listOf(
        "All" to "전체 부위",
        "Chest" to "가슴",
        "Back" to "등",
        "Legs" to "하체",
        "Shoulders" to "어깨",
        "Arms" to "팔"
    )

    val filteredExercises = remember(exercises, searchQuery, selectedCategory, selectedEquipmentFilter) {
        exercises.filter { exercise ->
            val matchesCategory = selectedCategory == "All" || exercise.muscleGroup.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() || exercise.name.contains(searchQuery, ignoreCase = true)
            val matchesEquipment = selectedEquipmentFilter == null || exercise.equipmentType == selectedEquipmentFilter
            matchesCategory && matchesSearch && matchesEquipment
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
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
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
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                placeholder = { Text("운동 종목 또는 브랜드 검색...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            // Equipment Type Tabs (Free Weight vs Machine)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedEquipmentFilter == null,
                    onClick = { selectedEquipmentFilter = null },
                    label = { Text("전체 기구") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedEquipmentFilter == EquipmentType.FREE_WEIGHT,
                    onClick = { selectedEquipmentFilter = EquipmentType.FREE_WEIGHT },
                    label = { Text("🏋️ 프리웨이트") },
                    modifier = Modifier.weight(1.3f)
                )
                FilterChip(
                    selected = selectedEquipmentFilter == EquipmentType.MACHINE,
                    onClick = { selectedEquipmentFilter = EquipmentType.MACHINE },
                    label = { Text("⚙️ 머신운동") },
                    modifier = Modifier.weight(1.2f)
                )
            }

            // Category filter chips (Body Parts)
            LazyRow(
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
                                    "일치하는 운동 종목이 없습니다",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "새로운 커스텀 프리웨이트 또는 머신 종목을 등록해보세요.",
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
            onConfirm = { name, group, equipType, brand ->
                viewModel.createCustomExercise(name, group, equipType, brand)
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
                    val typeStr = if (exercise.equipmentType == EquipmentType.MACHINE) {
                        "⚙️ 머신운동" + if (!exercise.machineBrand.isNullOrBlank()) " (${exercise.machineBrand})" else ""
                    } else {
                        "🏋️ 프리웨이트"
                    }
                    Text("운동 유형: $typeStr", style = MaterialTheme.typography.bodyMedium)
                    Text("등록 유형: ${if (exercise.isCustom) "사용자 정의 종목" else "기본 제공 종목"}", style = MaterialTheme.typography.bodyMedium)
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = exercise.muscleGroup,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("•", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    if (exercise.equipmentType == EquipmentType.MACHINE) {
                        val brandLabel = if (!exercise.machineBrand.isNullOrBlank()) "머신 (${exercise.machineBrand})" else "머신"
                        Text(
                            text = brandLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = "프리웨이트",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
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
    onConfirm: (name: String, muscleGroup: String, equipmentType: EquipmentType, machineBrand: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf("Chest") }
    var selectedEquipment by remember { mutableStateOf(EquipmentType.FREE_WEIGHT) }
    var machineBrand by remember { mutableStateOf("") }

    val muscleGroups = listOf(
        "Chest" to "가슴",
        "Back" to "등",
        "Legs" to "하체",
        "Shoulders" to "어깨",
        "Arms" to "팔",
        "Core" to "복근"
    )

    val popularBrands = listOf("Hammer Strength", "Cybex", "Life Fitness", "Newtech", "Arsenal", "Panatta")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 운동 종목 추가", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("운동 이름 (예: 체스트 프레스)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Equipment Type Selection (Free Weight vs Machine)
                Text("기구 구분:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedEquipment == EquipmentType.FREE_WEIGHT,
                        onClick = { selectedEquipment = EquipmentType.FREE_WEIGHT },
                        label = { Text("🏋️ 프리웨이트") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedEquipment == EquipmentType.MACHINE,
                        onClick = { selectedEquipment = EquipmentType.MACHINE },
                        label = { Text("⚙️ 머신운동") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // If Machine is selected, show Machine Brand input & suggestions
                if (selectedEquipment == EquipmentType.MACHINE) {
                    OutlinedTextField(
                        value = machineBrand,
                        onValueChange = { machineBrand = it },
                        label = { Text("머신 브랜드 / 제조사 (선택)") },
                        placeholder = { Text("예: Hammer Strength") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(popularBrands) { brand ->
                            SuggestionChip(
                                onClick = { machineBrand = brand },
                                label = { Text(brand, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                Text("타겟 부위 선택:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                LazyRow(
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
                onClick = { 
                    onConfirm(
                        name,
                        selectedGroup,
                        selectedEquipment,
                        if (selectedEquipment == EquipmentType.MACHINE) machineBrand.trim().ifBlank { null } else null
                    ) 
                },
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
