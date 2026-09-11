package com.example.presentation.session

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.WorkoutSession
import com.example.domain.util.SessionNotesManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSessionListScreen(
    viewModel: WorkoutSessionViewModel,
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.sessionListUiState.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()

    var sessionToEdit by remember { mutableStateOf<WorkoutSession?>(null) }
    var editNotesText by remember { mutableStateOf("") }
    var sessionToDelete by remember { mutableStateOf<WorkoutSession?>(null) }
    var sessionConflictToPrompt by remember { mutableStateOf<WorkoutSession?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("운동 세션 기록 (Sessions)", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            if (sessions.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        if (activeSession != null) {
                            sessionConflictToPrompt = activeSession
                        } else {
                            viewModel.createNewSession("Workout Session") { sessionId ->
                                onNavigateToDetail(sessionId)
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Session")
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (sessions.isEmpty()) {
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
                                text = "진행된 운동 세션이 없습니다",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "오늘의 운동을 시작해볼까요? 세션을 생성하고 운동 종목과 세트를 기록하세요.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = {
                                    if (activeSession != null) {
                                        sessionConflictToPrompt = activeSession
                                    } else {
                                        viewModel.createNewSession("Workout Session") { sessionId ->
                                            onNavigateToDetail(sessionId)
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("오늘의 운동 시작하기")
                            }
                        }
                    }
                }
            } else {

                items(sessions) { session ->
                    BentoSessionCard(
                        session = session,
                        onClick = {
                            viewModel.selectSession(session.id)
                            onNavigateToDetail(session.id)
                        },
                        onEdit = {
                            sessionToEdit = session
                            editNotesText = session.notes
                        },
                        onDelete = {
                            sessionToDelete = session
                        }
                    )
                }
            }
        }
    }

    // Conflict Dialog when trying to start a new session while one is active
    if (sessionConflictToPrompt != null) {
        val ongoing = sessionConflictToPrompt!!
        val ongoingTitle = SessionNotesManager.getSessionTitle(ongoing.notes).ifBlank { "진행 중인 운동" }
        AlertDialog(
            onDismissRequest = { sessionConflictToPrompt = null },
            icon = { Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("이미 진행 중인 운동이 있습니다", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "현재 '${ongoingTitle}' 세션이 진행 중입니다.\n한 번에 하나의 운동 세션만 진행할 수 있습니다.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "기존 운동으로 이동하거나, 기존 운동을 종료하고 새 운동을 시작할 수 있습니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetId = ongoing.id
                        sessionConflictToPrompt = null
                        onNavigateToDetail(targetId)
                    }
                ) {
                    Text("진행 중인 운동으로 이동")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { sessionConflictToPrompt = null }) {
                        Text("취소")
                    }
                    FilledTonalButton(
                        onClick = {
                            sessionConflictToPrompt = null
                            viewModel.createNewSession("Workout Session", finishExistingActive = true) { newId ->
                                onNavigateToDetail(newId)
                            }
                        }
                    ) {
                        Text("종료 후 새로 시작")
                    }
                }
            }
        )
    }

    // Edit Notes Dialog
    if (sessionToEdit != null) {
        AlertDialog(
            onDismissRequest = { sessionToEdit = null },
            title = { Text("세션 이름/메모 수정", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editNotesText,
                    onValueChange = { editNotesText = it },
                    label = { Text("세션 이름 또는 메모") },
                    placeholder = { Text("예: 가슴 & 삼두 루틴, 하체 폭파 등") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        sessionToEdit?.let { s ->
                            viewModel.updateSessionNotes(s.id, editNotesText.trim())
                        }
                        sessionToEdit = null
                    }
                ) {
                    Text("저장")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToEdit = null }) {
                    Text("취소")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("세션 삭제", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = {
                Text("정말 이 운동 세션을 삭제하시겠습니까?\n기록된 모든 세트 데이터가 함께 삭제되며 복구할 수 없습니다.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        sessionToDelete?.let { s ->
                            viewModel.deleteSession(s.id)
                        }
                        sessionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
fun BentoSessionCard(
    session: WorkoutSession,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isActive = session.endTime == null
    val dateFormat = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(session.startTime))
    var showMenu by remember { mutableStateOf(false) }

    val sessionTitle = remember(session.notes) {
        val parsed = SessionNotesManager.getSessionTitle(session.notes)
        if (parsed.isNotBlank()) parsed else "Workout Session"
    }

    val subtitle = if (isActive) {
        "🔥 운동 진행 중 • $dateStr"
    } else {
        val durationMin = ((session.endTime!! - session.startTime).coerceAtLeast(0L) / 60000L).toInt()
        "$dateStr • ${durationMin}분 완료"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        border = if (isActive) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
        } else null,
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isActive) "🔥" else dateStr.take(3),
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = sessionTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isActive) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "진행 중",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Session Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("수정 (Edit)") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("삭제 (Delete)", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

