package com.example.presentation.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

    // Cumulative Workout Elapsed Timer (ticking every second)
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(currentSession?.id) {
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "운동 시간: $elapsedFormatted",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        editedTitle = sessionTitle
                        showEditDialog = true
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Session Title")
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
        bottomBar = {
            // Live Rest Timer Bar (Docked at bottom if running, or quick action)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                AnimatedVisibility(
                    visible = restTimerState.isRunning,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HourglassBottom,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (restTimerState.isStopwatch) "세트 휴식 초시계" else "세트 간 휴식 중",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    val rMin = restTimerState.remainingSeconds / 60
                                    val rSec = restTimerState.remainingSeconds % 60
                                    Text(
                                        text = "%02d:%02d".format(rMin, rSec),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (!restTimerState.isStopwatch) {
                                    FilledTonalButton(
                                        onClick = { restTimerManager.addSeconds(30) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("+30초", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                IconButton(onClick = { restTimerManager.togglePauseResume() }) {
                                    Icon(
                                        imageVector = if (restTimerState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                        contentDescription = "Pause/Resume"
                                    )
                                }
                                IconButton(onClick = { restTimerManager.stopTimer() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Timer")
                                }
                            }
                        }
                    }
                }
            }
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
            // Header Action Card: Workout Status, Elapsed Clock, and Rest Quick Launchers
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
                            Column {
                                Text(
                                    text = "진행 중인 세션",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "총 ${sets.size}세트 완료",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = { showFinishWorkoutDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("운동 완료")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Rest Timer Launchers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "휴식 타이머:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            listOf(60, 90, 120).forEach { sec ->
                                SuggestionChip(
                                    onClick = { restTimerManager.startTimer(sec) },
                                    label = { Text("${sec}초") },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                            SuggestionChip(
                                onClick = { restTimerManager.startStopwatch() },
                                label = { Text("스톱워치") },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            if (groupedSets.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
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
                                text = "우측 하단의 + 버튼을 눌러 종목을 선택하고 첫 세트를 기록해보세요!",
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
                        val exerciseComment = SessionNotesManager.getExerciseComment(
                            currentSession?.notes ?: "",
                            exerciseId
                        )

                        ExerciseGroupCard(
                            exerciseId = exerciseId,
                            exerciseName = exerciseName,
                            sets = exerciseSets,
                            comment = exerciseComment,
                            lastHistory = exerciseHistoryMap[exerciseId],
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
        val exerciseName = exercises.find { it.id == selectedExerciseId }?.name ?: "Exercise"
        val existingSets = sets.filter { it.exerciseId == selectedExerciseId }
        val lastSet = existingSets.lastOrNull()
        val lastSummary = if (lastSet != null) {
            "${lastSet.weight} kg × ${lastSet.reps}회" + if (lastSet.rpe != null) " (RPE ${lastSet.rpe})" else ""
        } else null

        val currentSetNum = existingSets.size + 1
        val historyRecord = selectedExerciseId?.let { exerciseHistoryMap[it] }
        val lastSessionSet = historyRecord?.sets?.find { it.setNumber == currentSetNum } ?: historyRecord?.sets?.lastOrNull()

        ExerciseSetEditorSheet(
            exerciseName = exerciseName,
            setNumber = currentSetNum,
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
            title = { Text("오늘의 운동 완료!", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("총 운동 시간: $elapsedFormatted")
                    Text("완료한 세트: 총 ${sets.size}세트")
                    Text("오늘도 목표를 달성하셨습니다. 세션을 저장하고 마무리하시겠습니까?")
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
                    Text("세션 완료 및 저장")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFinishWorkoutDialog = false }) {
                    Text("운동 계속하기")
                }
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
    lastHistory: ExerciseHistoryRecord? = null,
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
                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${sets.size}세트",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                ) {
                                    Text(
                                        text = "${prevSet.setNumber}세트 ${prevSet.weight}kg×${prevSet.reps}" + if (prevSet.rpe != null) " (RPE ${prevSet.rpe})" else "",
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
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Set", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                Text("Weight", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1.2f))
                Text("Reps", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                Text("RPE", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            }

            // Sets List
            sets.forEachIndexed { index, set ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
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
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1.2f)
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
