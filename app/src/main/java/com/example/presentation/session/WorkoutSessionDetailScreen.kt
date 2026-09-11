package com.example.presentation.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.ExerciseHistoryRecord
import com.example.domain.model.ExerciseSet
import com.example.domain.util.SessionNotesManager
import com.example.presentation.timer.RestTimerManager
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSessionDetailScreen(
    exerciseViewModel: com.example.presentation.exercise.ExerciseViewModel,
    viewModel: WorkoutSessionViewModel,
    onBack: () -> Unit,
    restTimerManager: RestTimerManager = remember { RestTimerManager() },
    modifier: Modifier = Modifier
) {
    val currentSession by viewModel.currentSession.collectAsStateWithLifecycle()
    val sets by viewModel.currentSessionSets.collectAsStateWithLifecycle()
    val exerciseHistoryMap by viewModel.exerciseHistoryMap.collectAsStateWithLifecycle()
    val restTimerState by restTimerManager.timerState.collectAsStateWithLifecycle()

    var showEditorSheet by remember { mutableStateOf(false) }
    var showExerciseSelection by remember { mutableStateOf(false) }
    var selectedExerciseId by remember { mutableStateOf<String?>(null) }

    var activeSetToComplete by remember { mutableStateOf<ExerciseSet?>(null) }
    var exerciseForPlannedSet by remember { mutableStateOf<Pair<String, String>?>(null) }

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showFinishWorkoutDialog by remember { mutableStateOf(false) }

    val sessionTitle = remember(currentSession?.notes) {
        val raw = currentSession?.notes ?: ""
        val parsed = SessionNotesManager.getSessionTitle(raw)
        if (parsed.isNotBlank()) parsed else "운동 세션"
    }
    var editedTitle by remember(sessionTitle) { mutableStateOf(sessionTitle) }

    val exercises by exerciseViewModel.exercises.collectAsStateWithLifecycle()

    val isCompleted = currentSession?.endTime != null

    // Cumulative Workout Elapsed Timer (ticking every second only for active sessions)
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(currentSession?.id, isCompleted) {
        if (isCompleted) return@LaunchedEffect
        while (true) {
            delay(1000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val elapsedSeconds = remember(nowMillis, currentSession?.startTime, currentSession?.endTime) {
        val start = currentSession?.startTime ?: nowMillis
        val end = currentSession?.endTime ?: nowMillis
        ((end - start).coerceAtLeast(0L) / 1000L).toInt()
    }

    val elapsedHours = elapsedSeconds / 3600
    val elapsedMinutes = (elapsedSeconds % 3600) / 60
    val elapsedSecs = elapsedSeconds % 60
    val elapsedFormatted = if (elapsedHours > 0) {
        "%02d:%02d:%02d".format(elapsedHours, elapsedMinutes, elapsedSecs)
    } else {
        "%02d:%02d".format(elapsedMinutes, elapsedSecs)
    }

    // Group sets by exerciseId
    val groupedSets = sets.groupBy { it.exerciseId }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = sessionTitle,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        val completedSetsCount = sets.count { it.isCompleted }
                        Text(
                            text = if (sets.isEmpty()) "세트 없음" else if (completedSetsCount == sets.size) "총 ${sets.size}세트 완료" else "총 ${completedSetsCount}/${sets.size}세트 완료",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isCompleted) {
                        if (sets.isNotEmpty()) {
                            FilledTonalButton(
                                onClick = { showFinishWorkoutDialog = true },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("완료", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    } else {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("완료됨", fontWeight = FontWeight.Bold) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(8.dp)
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
            if (!isCompleted && sets.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showExerciseSelection = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "운동 종목 추가")
                }
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
            // ⏱️ [Zone 1: 상단 시간 & 세션 상태 영역 (Time & Lifecycle Zone)]
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // 1. 총 누적 운동 시간
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "총 운동 시간",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = elapsedFormatted,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (!isCompleted) {
                                if (sets.isNotEmpty()) {
                                    Button(
                                        onClick = {
                                            val nextPendingSet = sets.firstOrNull { !it.isCompleted }
                                            if (nextPendingSet != null) {
                                                activeSetToComplete = nextPendingSet
                                            } else {
                                                val lastExId = sets.last().exerciseId
                                                val lastExName = exercises.find { it.id == lastExId }?.name ?: "운동"
                                                exerciseForPlannedSet = lastExId to lastExName
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("세트 완료", fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("운동 완료됨", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                            }
                        }

                        if (!isCompleted) {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // 2. 실시간 세트 휴식 타이머 & 컨트롤
                            if (restTimerState.isRunning) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.HourglassBottom,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = if (restTimerState.isStopwatch) "세트 휴식 초시계" else "세트 간 휴식 중",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                                val rMin = restTimerState.remainingSeconds / 60
                                                val rSec = restTimerState.remainingSeconds % 60
                                                Text(
                                                    text = "%02d:%02d".format(rMin, rSec),
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (!restTimerState.isStopwatch) {
                                                FilledTonalButton(
                                                    onClick = { restTimerManager.addSeconds(30) },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("+30초", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            IconButton(onClick = { restTimerManager.togglePauseResume() }) {
                                                Icon(
                                                    imageVector = if (restTimerState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                                    contentDescription = "Pause/Resume",
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                            IconButton(onClick = { restTimerManager.stopTimer() }) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Close Timer",
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "휴식 타이머:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    listOf(60, 90, 120).forEach { sec ->
                                        SuggestionChip(
                                            onClick = { restTimerManager.startTimer(sec) },
                                            label = { Text("${sec}초", fontWeight = FontWeight.Medium) },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                    SuggestionChip(
                                        onClick = { restTimerManager.startStopwatch() },
                                        label = { Text("스톱워치", fontWeight = FontWeight.Medium) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        } else {
                            // Completed state summary row
                            val strengthSets = sets.filter { set ->
                                val ex = exercises.find { it.id == set.exerciseId }
                                ex?.isCardio != true
                            }
                            val totalVolume = strengthSets.sumOf { it.weight * it.reps }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "종목 ${groupedSets.keys.size}개 · 총 ${sets.size}세트 완료",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (totalVolume > 0) {
                                    Text(
                                        text = "총 볼륨 ${totalVolume.toInt()} kg",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 🏋️ [Zone 2: 중단 운동 정보 & 관리 영역 (Workout Content Zone)]
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "운동 세션 정보",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = sessionTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "종목 ${groupedSets.keys.size}개 · 총 ${sets.size}세트 기록됨",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = {
                                    editedTitle = sessionTitle
                                    showEditDialog = true
                                }) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "세션명 수정",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { showDeleteDialog = true }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "세션 삭제",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        if (!isCompleted && sets.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            OutlinedButton(
                                onClick = { showExerciseSelection = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("운동 종목 추가", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (groupedSets.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "첫 운동을 선택하고 기록을 시작하세요",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "오늘 진행할 운동 종목을 추가하면\n세트 기록과 휴식 타이머가 시작됩니다.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                            if (!isCompleted) {
                                Button(
                                    onClick = { showExerciseSelection = true },
                                    modifier = Modifier.fillMaxWidth(0.85f),
                                    shape = RoundedCornerShape(14.dp),
                                    contentPadding = PaddingValues(vertical = 12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("첫 운동 추가", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                        }
                    }
                }
            } else {
                groupedSets.forEach { (exerciseId, exerciseSets) ->
                    item {
                        val exerciseName = exercises.find { it.id == exerciseId }?.name ?: "Exercise: $exerciseId"
                        val exerciseComment = SessionNotesManager.getExerciseComment(
                            currentSession?.notes ?: "",
                            exerciseId
                        )

                        val exercise = exercises.find { it.id == exerciseId }
                        val isCardio = exercise?.isCardio == true

                        ExerciseGroupCard(
                            exerciseId = exerciseId,
                            exerciseName = exerciseName,
                            isCardio = isCardio,
                            sets = exerciseSets,
                            comment = exerciseComment,
                            lastHistory = exerciseHistoryMap[exerciseId],
                            onAddPlannedSet = {
                                if (!isCompleted) {
                                    exerciseForPlannedSet = exerciseId to exerciseName
                                }
                            },
                            onSetClick = { set ->
                                if (!isCompleted) {
                                    activeSetToComplete = set
                                }
                            },
                            onToggleCompleted = { set ->
                                if (!isCompleted) {
                                    viewModel.toggleSetCompleted(set.id)
                                }
                            },
                            onSaveComment = { newComment ->
                                currentSession?.let { s ->
                                    val updatedNotes = SessionNotesManager.setExerciseComment(
                                        s.notes,
                                        exerciseId,
                                        newComment
                                    )
                                    viewModel.updateSessionNotes(s.id, updatedNotes)
                                }
                            }
                        )
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
                viewModel.loadHistoryForExercise(exercise.id)
                showExerciseSelection = false
                showEditorSheet = true
            },
            onDismissRequest = { showExerciseSelection = false }
        )
    }

    if (showEditorSheet) {
        val selectedExercise = exercises.find { it.id == selectedExerciseId }
        val exerciseName = selectedExercise?.name ?: "Exercise"
        val isCardio = selectedExercise?.isCardio == true
        val existingSets = sets.filter { it.exerciseId == selectedExerciseId }
        val lastSet = existingSets.lastOrNull()
        val lastSummary = if (lastSet != null) {
            if (isCardio) {
                "${lastSet.weight} · ${lastSet.reps}분" + if (lastSet.rpe != null) " (RPE ${lastSet.rpe})" else ""
            } else {
                "${lastSet.weight} kg × ${lastSet.reps}회" + if (lastSet.rpe != null) " (RPE ${lastSet.rpe})" else ""
            }
        } else null

        val currentSetNum = existingSets.size + 1
        val historyRecord = selectedExerciseId?.let { exerciseHistoryMap[it] }
        val lastSessionSet = historyRecord?.sets?.find { it.setNumber == currentSetNum } ?: historyRecord?.sets?.lastOrNull()

        ExerciseSetEditorSheet(
            exerciseName = exerciseName,
            setNumber = currentSetNum,
            isCardio = isCardio,
            lastSetSummary = lastSummary,
            lastWeight = lastSet?.weight,
            lastReps = lastSet?.reps,
            lastRpe = lastSet?.rpe,
            lastSessionDate = historyRecord?.sessionDate,
            lastSessionWeight = lastSessionSet?.weight,
            lastSessionReps = lastSessionSet?.reps,
            lastSessionRpe = lastSessionSet?.rpe,
            onDismissRequest = { showEditorSheet = false },
            onSaveSet = { weight, reps, rpe ->
                selectedExerciseId?.let { viewModel.addSet(it, weight, reps, rpe) }
                // Automatically start 90s rest timer upon set completion
                restTimerManager.startTimer(90)
                showEditorSheet = false
            }
        )
    }

    // Edit Session Title Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("세션 이름 수정", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it },
                    label = { Text("세션 이름") },
                    placeholder = { Text("예: 가슴 & 삼두 루틴") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        currentSession?.let { session ->
                            val updated = SessionNotesManager.setSessionTitle(session.notes, editedTitle.trim())
                            viewModel.updateSessionNotes(session.id, updated)
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

    // Finish Workout Summary Dialog
    if (showFinishWorkoutDialog) {
        AlertDialog(
            onDismissRequest = { showFinishWorkoutDialog = false },
            icon = { Icon(Icons.Default.Celebration, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("운동을 마칠까요?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("⏱️ 총 운동 시간: $elapsedFormatted")
                    Text("📊 기록된 세트: 총 ${sets.size}세트")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "모든 세트 기록이 안전하게 저장되며,\n통계 대시보드에 즉시 반영됩니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        currentSession?.let { s ->
                            viewModel.updateSession(s.copy(endTime = System.currentTimeMillis()))
                        }
                        showFinishWorkoutDialog = false
                        onBack()
                    }
                ) {
                    Text("운동 마치기")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishWorkoutDialog = false }) {
                    Text("계속 운동하기")
                }
            }
        )
    }

    // Set Completion & Reps Check-off Dialog
    activeSetToComplete?.let { setToComplete ->
        val exName = exercises.find { it.id == setToComplete.exerciseId }?.name ?: "운동"
        val isCardioEx = exercises.find { it.id == setToComplete.exerciseId }?.isCardio == true
        SetCompletionDialog(
            set = setToComplete,
            exerciseName = exName,
            isCardio = isCardioEx,
            onDismiss = { activeSetToComplete = null },
            onComplete = { actualReps, rpe ->
                viewModel.completeSet(setToComplete.id, actualReps, rpe)
                restTimerManager.startTimer(setToComplete.restSeconds ?: 90)
                activeSetToComplete = null
            }
        )
    }

    // Add Planned Set Dialog
    exerciseForPlannedSet?.let { (exId, exName) ->
        val exSets = sets.filter { it.exerciseId == exId }
        val isCardioEx = exercises.find { it.id == exId }?.isCardio == true
        AddPlannedSetDialog(
            exerciseName = exName,
            lastSet = exSets.lastOrNull(),
            isCardio = isCardioEx,
            onDismiss = { exerciseForPlannedSet = null },
            onAdd = { weight, targetReps ->
                viewModel.addPlannedSet(exId, weight, targetReps)
                exerciseForPlannedSet = null
            }
        )
    }
}

@Composable
fun ExerciseGroupCard(
    exerciseId: String,
    exerciseName: String,
    sets: List<ExerciseSet>,
    comment: String,
    isCardio: Boolean = false,
    lastHistory: ExerciseHistoryRecord? = null,
    onAddPlannedSet: () -> Unit,
    onSetClick: (ExerciseSet) -> Unit,
    onToggleCompleted: (ExerciseSet) -> Unit,
    onSaveComment: (String) -> Unit
) {
    var isEditingComment by remember { mutableStateOf(false) }
    var commentInput by remember(comment) { mutableStateOf(comment) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    val completedCount = sets.count { it.isCompleted }
                    Text(
                        text = if (completedCount == sets.size) "${sets.size}세트 완료" else "${completedCount}/${sets.size}세트 완료",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = onAddPlannedSet,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ 세트", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            // Last Workout Session History Reference
            if (lastHistory != null && lastHistory.sets.isNotEmpty()) {
                val lastDateStr = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date(lastHistory.sessionDate))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "지난 수행 기록 ($lastDateStr)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            lastHistory.sets.forEach { prevSet ->
                                val setDesc = if (isCardio) {
                                    "${prevSet.setNumber}세트 ${prevSet.weight}·${prevSet.reps}분"
                                } else {
                                    "${prevSet.setNumber}세트 ${prevSet.weight}kg×${prevSet.reps}"
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                ) {
                                    Text(
                                        text = setDesc + if (prevSet.rpe != null) " (RPE ${prevSet.rpe})" else "",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Set Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("상태", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(36.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Set", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(32.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (isCardio) "속도/레벨" else "Weight", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1.2f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (isCardio) "시간(분)" else "Reps", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1.2f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("RPE", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(36.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Sets List
            sets.forEachIndexed { index, set ->
                val isCompleted = set.isCompleted
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isCompleted) MaterialTheme.colorScheme.surfaceContainerHigh
                            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                        )
                        .clickable {
                            if (!isCompleted) onSetClick(set)
                            else onToggleCompleted(set)
                        }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status Checkbox
                    IconButton(
                        onClick = {
                            if (!isCompleted) onSetClick(set)
                            else onToggleCompleted(set)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (isCompleted) "Completed" else "Check off set",
                            tint = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(32.dp)
                    )

                    Text(
                        text = if (isCardio) {
                            if (set.weight % 1.0 == 0.0) "${set.weight.toInt()}" else "${set.weight}"
                        } else {
                            "${set.weight} kg"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1.2f)
                    )

                    // Reps or Target Reps
                    if (isCompleted) {
                        Text(
                            text = if (isCardio) "${set.reps}분" else "${set.reps}회",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1.2f)
                        )
                    } else {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text(
                                text = if (isCardio) "목표 ${set.targetReps ?: set.reps}분" else "목표 ${set.targetReps ?: set.reps}회",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = set.rpe?.let { "$it" } ?: "-",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))

            // Per-Exercise Evaluation & Comment Section
            if (isEditingComment) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = commentInput,
                        onValueChange = { commentInput = it },
                        label = { Text("종목 평가 / 피드백 메모") },
                        placeholder = { Text("예: 가슴 자극 좋았음. 다음엔 82.5kg 시도") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            commentInput = comment
                            isEditingComment = false
                        }) {
                            Text("취소")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            onSaveComment(commentInput.trim())
                            isEditingComment = false
                        }) {
                            Text("코멘트 저장")
                        }
                    }
                }
            } else {
                if (comment.isNotBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.ChatBubbleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = comment,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { isEditingComment = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Comment",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { isEditingComment = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddComment,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "종목 평가 / 피드백 남기기",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SetCompletionDialog(
    set: ExerciseSet,
    exerciseName: String,
    isCardio: Boolean = false,
    onDismiss: () -> Unit,
    onComplete: (actualReps: Int, rpe: Double?) -> Unit
) {
    val initialReps = set.targetReps ?: set.reps
    var repsInput by remember { mutableIntStateOf(initialReps) }
    var selectedRpe by remember { mutableStateOf(set.rpe) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isCardio) "세트 완료 체크" else "${if (set.weight % 1.0 == 0.0) set.weight.toInt() else set.weight}kg 세트 완료 체크",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Target vs Actual header
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isCardio) "목표 시간" else "목표 횟수",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (isCardio) "${set.targetReps ?: set.reps}분" else "${set.targetReps ?: set.reps}회",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Actual Reps Counter
                Text(
                    text = if (isCardio) "실제 수행 시간" else "실제 수행 횟수",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FilledIconButton(
                        onClick = { if (repsInput > 1) repsInput-- },
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(24.dp))
                    }

                    Text(
                        text = if (isCardio) "${repsInput}분" else "${repsInput}회",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    FilledIconButton(
                        onClick = { repsInput++ },
                        modifier = Modifier.size(48.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }

                // Quick Increment/Decrement Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    if (isCardio) {
                        listOf(-5, -1, 1, 5).forEach { diff ->
                            SuggestionChip(
                                onClick = { repsInput = (repsInput + diff).coerceAtLeast(1) },
                                label = { Text("${if (diff > 0) "+$diff" else "$diff"}분") },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    } else {
                        listOf(-5, -1, 1, 5).forEach { diff ->
                            SuggestionChip(
                                onClick = { repsInput = (repsInput + diff).coerceAtLeast(1) },
                                label = { Text("${if (diff > 0) "+$diff" else "$diff"}") },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                // RPE Selector (Optional)
                if (!isCardio) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "체감 난이도 (RPE - 선택)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(7.0, 8.0, 8.5, 9.0, 9.5, 10.0).forEach { rpeVal ->
                                FilterChip(
                                    selected = selectedRpe == rpeVal,
                                    onClick = { selectedRpe = if (selectedRpe == rpeVal) null else rpeVal },
                                    label = { Text("$rpeVal", style = MaterialTheme.typography.labelSmall) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onComplete(repsInput, selectedRpe) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "세트 완료 & 휴식 시작",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("취소")
            }
        }
    )
}

@Composable
fun AddPlannedSetDialog(
    exerciseName: String,
    lastSet: ExerciseSet?,
    isCardio: Boolean = false,
    onDismiss: () -> Unit,
    onAdd: (weight: Double, targetReps: Int) -> Unit
) {
    val initialWeight = lastSet?.weight ?: if (isCardio) 6.0 else 60.0
    val initialReps = lastSet?.let { it.targetReps ?: it.reps } ?: if (isCardio) 20 else 10

    var weightInput by remember { mutableStateOf(if (initialWeight % 1.0 == 0.0) "${initialWeight.toInt()}" else "$initialWeight") }
    var repsInput by remember { mutableIntStateOf(initialReps) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "$exerciseName 세트 추가",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Weight input
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text(if (isCardio) "목표 속도 / 레벨" else "목표 무게 (kg)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Next
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Reps / Duration
                Column {
                    Text(
                        text = if (isCardio) "목표 시간: ${repsInput}분" else "목표 횟수: ${repsInput}회",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FilledIconButton(
                            onClick = { if (repsInput > 1) repsInput-- }
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }
                        Text(
                            text = if (isCardio) "${repsInput}분" else "${repsInput}회",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        FilledIconButton(
                            onClick = { repsInput++ }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase")
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(-5, -1, 1, 5).forEach { diff ->
                            SuggestionChip(
                                onClick = { repsInput = (repsInput + diff).coerceAtLeast(1) },
                                label = { Text("${if (diff > 0) "+$diff" else "$diff"}") },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = weightInput.toDoubleOrNull() ?: 0.0
                    onAdd(w, repsInput)
                }
            ) {
                Text("세트 등록")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}
