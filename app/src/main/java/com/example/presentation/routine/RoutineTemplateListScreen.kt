package com.example.presentation.routine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.EquipmentType
import com.example.domain.model.Exercise
import com.example.domain.model.ExercisePreset
import com.example.domain.model.RoutineTemplate
import com.example.domain.model.WorkoutSession
import com.example.domain.util.SessionNotesManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineTemplateListScreen(
    viewModel: RoutineTemplateViewModel,
    onApplyTemplate: (templateId: String) -> Unit,
    onNavigateToSessionDetail: ((sessionId: String) -> Unit)? = null,
    exerciseViewModel: com.example.presentation.exercise.ExerciseViewModel? = null,
    modifier: Modifier = Modifier
) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val routineLastWorkoutMap by viewModel.routineLastWorkoutMap.collectAsStateWithLifecycle()
    val exercises by (exerciseViewModel?.exercises?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(emptyList<Exercise>()) })

    var routineToEdit by remember { mutableStateOf<RoutineTemplate?>(null) }
    var isCreatingRoutine by remember { mutableStateOf(false) }
    var templateToDelete by remember { mutableStateOf<RoutineTemplate?>(null) }
    var routineConflictToPrompt by remember { mutableStateOf<RoutineTemplate?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("루틴 라이브러리", fontWeight = FontWeight.Bold)
                        Text(
                            "자주 하는 운동 루틴을 선택하여 즉시 세션을 시작하세요",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isCreatingRoutine = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "새 루틴 만들기")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(templates) { template ->
                RoutineTemplateCard(
                    template = template,
                    exercises = exercises,
                    lastWorkoutDate = routineLastWorkoutMap[template.id],
                    onApply = {
                        if (activeSession != null) {
                            routineConflictToPrompt = template
                        } else {
                            onApplyTemplate(template.id)
                        }
                    },
                    onEdit = { routineToEdit = template },
                    onDelete = { templateToDelete = template }
                )
            }

            if (templates.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "등록된 루틴 템플릿이 없습니다",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Push Day, Pull Day, Leg Day처럼 자주 수행하는 운동 구성을 미리 저장하고 바로 불러와보세요!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(onClick = { isCreatingRoutine = true }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("새 루틴 만들기")
                            }
                        }
                    }
                }
            }
        }
    }

    // Conflict Dialog when an active session is already running
    if (routineConflictToPrompt != null) {
        val targetTemplate = routineConflictToPrompt!!
        val activeNotes = activeSession?.notes ?: ""
        val ongoingTitle = SessionNotesManager.getSessionTitle(activeNotes).ifBlank { "진행 중인 운동" }
        AlertDialog(
            onDismissRequest = { routineConflictToPrompt = null },
            icon = { Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("이미 진행 중인 운동이 있습니다", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "현재 '${ongoingTitle}' 세션이 진행 중입니다.\n한 번에 하나의 운동 세션만 진행할 수 있습니다.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "기존 운동을 종료하고 '${targetTemplate.name}' 루틴을 시작하시겠습니까?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val activeId = activeSession?.id
                        routineConflictToPrompt = null
                        if (activeId != null) {
                            onNavigateToSessionDetail?.invoke(activeId)
                        }
                    }
                ) {
                    Text("진행 중인 운동으로 이동")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { routineConflictToPrompt = null }) {
                        Text("취소")
                    }
                    FilledTonalButton(
                        onClick = {
                            val templateId = targetTemplate.id
                            routineConflictToPrompt = null
                            viewModel.applyTemplate(
                                templateId = templateId,
                                finishExistingActive = true,
                                onSessionCreated = { newSessionId ->
                                    onNavigateToSessionDetail?.invoke(newSessionId) ?: onApplyTemplate(templateId)
                                }
                            )
                        }
                    ) {
                        Text("종료 후 루틴 시작")
                    }
                }
            }
        )
    }

    if (isCreatingRoutine) {

        RoutineEditorDialog(
            initialTemplate = null,
            availableExercises = exercises,
            onDismiss = { isCreatingRoutine = false },
            onSave = { name, presets ->
                viewModel.createTemplate(name, presets)
                isCreatingRoutine = false
            }
        )
    }

    if (routineToEdit != null) {
        RoutineEditorDialog(
            initialTemplate = routineToEdit,
            availableExercises = exercises,
            onDismiss = { routineToEdit = null },
            onSave = { name, presets ->
                routineToEdit?.let { template ->
                    viewModel.updateTemplate(template.id, name, presets)
                }
                routineToEdit = null
            }
        )
    }

    if (templateToDelete != null) {
        AlertDialog(
            onDismissRequest = { templateToDelete = null },
            title = { Text("루틴 삭제", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { Text("'${templateToDelete?.name}' 루틴을 삭제하시겠습니까?") },
            confirmButton = {
                Button(
                    onClick = {
                        templateToDelete?.let { viewModel.deleteTemplate(it.id) }
                        templateToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { templateToDelete = null }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
fun RoutineTemplateCard(
    template: RoutineTemplate,
    exercises: List<Exercise>,
    lastWorkoutDate: Long? = null,
    onApply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onApply),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Title + Exercises Badge + Edit & Options Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = template.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "총 ${template.exercises.size}개 운동 포함",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (lastWorkoutDate != null) {
                                val dateStr = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date(lastWorkoutDate))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            modifier = Modifier.size(11.dp),
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "최근: $dateStr",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Routine",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("이 루틴으로 세션 시작") },
                                leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onApply()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("루틴 세부 편집") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("루틴 삭제", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            // Exercise List Preview
            if (template.exercises.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(10.dp))

                val displayExercises = template.exercises.take(3)
                val remainingCount = template.exercises.size - displayExercises.size

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    displayExercises.forEach { preset ->
                        val exercise = exercises.find { it.id == preset.exerciseId }
                        val exName = exercise?.name ?: preset.exerciseId
                        val isMachine = exercise?.equipmentType == EquipmentType.MACHINE
                        val brand = exercise?.machineBrand

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "•",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    text = exName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                val isCardio = exercise?.isCardio == true
                                val badgeColor = when {
                                    isCardio -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                    isMachine -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                                    else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                }
                                val badgeContentColor = when {
                                    isCardio -> MaterialTheme.colorScheme.onPrimaryContainer
                                    isMachine -> MaterialTheme.colorScheme.onTertiaryContainer
                                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                                }
                                val badgeText = when {
                                    isCardio -> "🏃 유산소"
                                    isMachine -> if (!brand.isNullOrBlank()) "⚙️ $brand" else "⚙️ 머신"
                                    else -> "🏋️ 프리"
                                }
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = badgeColor
                                ) {
                                    Text(
                                        text = badgeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                        color = badgeContentColor
                                    )
                                }
                            }
                            Text(
                                text = if (exercise?.isCardio == true) "속도 ${preset.defaultWeight} · ${preset.defaultReps}분" else "${preset.defaultWeight}kg × ${preset.defaultReps}회",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (remainingCount > 0) {
                        Text(
                            text = "외 ${remainingCount}개 종목 더보기...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Primary Full-Width CTA Button
            Button(
                onClick = onApply,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("이 루틴으로 시작", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditorDialog(
    initialTemplate: RoutineTemplate?,
    availableExercises: List<Exercise>,
    onDismiss: () -> Unit,
    onSave: (name: String, presets: List<ExercisePreset>) -> Unit
) {
    var name by remember { mutableStateOf(initialTemplate?.name ?: "") }
    var presets by remember { mutableStateOf(initialTemplate?.exercises ?: emptyList()) }
    var showExercisePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.88f),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Text(
                text = if (initialTemplate != null) "루틴 세부 편집" else "새 루틴 만들기",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("루틴 이름 (예: 등 & 이두 데이)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "루틴 운동 구성 (총 ${presets.size}개)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FilledTonalButton(
                        onClick = { showExercisePicker = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("운동 추가", style = MaterialTheme.typography.labelMedium)
                    }
                }

                if (presets.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "포함된 운동이 없습니다",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "우측 상단의 '+ 운동 추가'를 눌러 종목을 추가하세요",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    presets.forEachIndexed { index, preset ->
                        val exercise = availableExercises.find { it.id == preset.exerciseId }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                val isCardio = exercise?.isCardio == true
                                val isMachine = exercise?.equipmentType == EquipmentType.MACHINE

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            "${index + 1}. ",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            exercise?.name ?: preset.exerciseId,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val badgeColor = when {
                                            isCardio -> MaterialTheme.colorScheme.primaryContainer
                                            isMachine -> MaterialTheme.colorScheme.tertiaryContainer
                                            else -> MaterialTheme.colorScheme.secondaryContainer
                                        }
                                        val badgeContentColor = when {
                                            isCardio -> MaterialTheme.colorScheme.onPrimaryContainer
                                            isMachine -> MaterialTheme.colorScheme.onTertiaryContainer
                                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                                        }
                                        val badgeText = when {
                                            isCardio -> "🏃 유산소"
                                            isMachine -> "⚙️ 머신"
                                            else -> "🏋️ 프리"
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = badgeColor
                                        ) {
                                            Text(
                                                text = badgeText,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                color = badgeContentColor
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (index > 0) {
                                            IconButton(
                                                onClick = {
                                                    val mutable = presets.toMutableList()
                                                    val temp = mutable[index]
                                                    mutable[index] = mutable[index - 1]
                                                    mutable[index - 1] = temp
                                                    presets = mutable
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.ArrowUpward,
                                                    contentDescription = "위로 이동",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        if (index < presets.size - 1) {
                                            IconButton(
                                                onClick = {
                                                    val mutable = presets.toMutableList()
                                                    val temp = mutable[index]
                                                    mutable[index] = mutable[index + 1]
                                                    mutable[index + 1] = temp
                                                    presets = mutable
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.ArrowDownward,
                                                    contentDescription = "아래로 이동",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                presets = presets.filterIndexed { i, _ -> i != index }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "삭제",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    var weightText by remember(preset.defaultWeight) {
                                        mutableStateOf(if (preset.defaultWeight % 1.0 == 0.0) "${preset.defaultWeight.toInt()}" else "${preset.defaultWeight}")
                                    }
                                    var repsText by remember(preset.defaultReps) {
                                        mutableStateOf("${preset.defaultReps}")
                                    }

                                    OutlinedTextField(
                                        value = weightText,
                                        onValueChange = {
                                            weightText = it
                                            val w = it.toDoubleOrNull() ?: 0.0
                                            presets = presets.mapIndexed { i, p ->
                                                if (i == index) p.copy(defaultWeight = w) else p
                                            }
                                        },
                                        label = { Text(if (isCardio) "목표 속도/레벨" else "목표 무게(kg)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedTextField(
                                        value = repsText,
                                        onValueChange = {
                                            repsText = it
                                            val r = it.toIntOrNull() ?: 0
                                            presets = presets.mapIndexed { i, p ->
                                                if (i == index) p.copy(defaultReps = r) else p
                                            }
                                        },
                                        label = { Text(if (isCardio) "목표 시간(분)" else "목표 횟수(회)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name.trim(), presets.mapIndexed { idx, p -> p.copy(orderIndex = idx) })
                },
                enabled = name.isNotBlank()
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )

    if (showExercisePicker) {
        RoutineExercisePickerDialog(
            exercises = availableExercises,
            onDismiss = { showExercisePicker = false },
            onSelectExercise = { ex ->
                presets = presets + ExercisePreset(
                    exerciseId = ex.id,
                    defaultWeight = 20.0,
                    defaultReps = 10,
                    orderIndex = presets.size
                )
                showExercisePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineExercisePickerDialog(
    exercises: List<Exercise>,
    onDismiss: () -> Unit,
    onSelectExercise: (Exercise) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<EquipmentType?>(null) }

    val filteredExercises = remember(exercises, searchQuery, selectedFilter) {
        exercises.filter { exercise ->
            val matchesQuery = exercise.name.contains(searchQuery, ignoreCase = true) ||
                    exercise.muscleGroup.contains(searchQuery, ignoreCase = true) ||
                    (exercise.machineBrand?.contains(searchQuery, ignoreCase = true) == true)
            val matchesType = selectedFilter == null || exercise.equipmentType == selectedFilter
            matchesQuery && matchesType
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        title = { Text("루틴에 추가할 운동 선택", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("운동 이름, 부위, 브랜드 검색...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilter == null,
                            onClick = { selectedFilter = null },
                            label = { Text("전체") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == EquipmentType.FREE_WEIGHT,
                            onClick = { selectedFilter = EquipmentType.FREE_WEIGHT },
                            label = { Text("🏋️ 프리웨이트") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == EquipmentType.MACHINE,
                            onClick = { selectedFilter = EquipmentType.MACHINE },
                            label = { Text("⚙️ 머신") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedFilter == EquipmentType.CARDIO,
                            onClick = { selectedFilter = EquipmentType.CARDIO },
                            label = { Text("🏃 유산소") }
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredExercises) { exercise ->
                        val isMachine = exercise.equipmentType == EquipmentType.MACHINE
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectExercise(exercise) },
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = exercise.name,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = exercise.muscleGroup,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (isMachine && !exercise.machineBrand.isNullOrBlank()) {
                                            Text(
                                                text = "• ${exercise.machineBrand}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                val badgeText = when (exercise.equipmentType) {
                                    EquipmentType.CARDIO -> "🏃 유산소"
                                    EquipmentType.MACHINE -> "⚙️ 머신"
                                    EquipmentType.FREE_WEIGHT -> "🏋️ 프리"
                                }
                                val badgeContainer = when (exercise.equipmentType) {
                                    EquipmentType.CARDIO -> MaterialTheme.colorScheme.primaryContainer
                                    EquipmentType.MACHINE -> MaterialTheme.colorScheme.tertiaryContainer
                                    EquipmentType.FREE_WEIGHT -> MaterialTheme.colorScheme.secondaryContainer
                                }
                                val badgeContent = when (exercise.equipmentType) {
                                    EquipmentType.CARDIO -> MaterialTheme.colorScheme.onPrimaryContainer
                                    EquipmentType.MACHINE -> MaterialTheme.colorScheme.onTertiaryContainer
                                    EquipmentType.FREE_WEIGHT -> MaterialTheme.colorScheme.onSecondaryContainer
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = badgeContainer
                                ) {
                                    Text(
                                        text = badgeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = badgeContent
                                    )
                                }
                            }
                        }
                    }

                    if (filteredExercises.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "검색된 운동이 없습니다",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("닫기")
            }
        }
    )
}
