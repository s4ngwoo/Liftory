package com.example.presentation.scaffold

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.Exercise
import com.example.domain.model.WorkoutSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrengthLogScaffoldScreen(
    viewModel: StrengthLogScaffoldViewModel,
    modifier: Modifier = Modifier
) {
    val sessionCount by viewModel.sessionCount.collectAsStateWithLifecycle()
    val pendingUploadsCount by viewModel.pendingUploadCount.collectAsStateWithLifecycle()
    val lastActionMessage by viewModel.actionMessage.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Strength Log",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Sprint 0 — Scaffolding & Architecture",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E676))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Offline-First",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Architecture Summary Card
            item {
                ArchitectureOverviewCard()
            }

            // Database & Scaffolding Status Card
            item {
                DatabaseStatusCard(
                    isInitialized = true,
                    sessionCount = sessionCount.size,
                    exerciseCount = 0,
                    pendingUploadCount = pendingUploadsCount,
                    lastMessage = lastActionMessage,
                    onCreateSampleSession = { viewModel.createSampleSession() },
                    onTriggerSync = viewModel::triggerSync
                )
            }

            // Layer Checklist
            item {
                Text(
                    text = "Clean Architecture Layers Checklist",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                LayerCheckItem(
                    layerName = "Domain Layer",
                    description = "Pure Kotlin models (WorkoutSession, ExerciseSet, Exercise, RoutineTemplate) & Repository interfaces. No Android dependencies.",
                    icon = Icons.Default.Layers
                )
            }

            item {
                LayerCheckItem(
                    layerName = "Infrastructure Layer",
                    description = "Room 2.x Database (strength_log.db), 4 Entities, 4 DAOs with Reactive Flow, and Repository Implementations.",
                    icon = Icons.Default.Storage
                )
            }

            item {
                LayerCheckItem(
                    layerName = "Application Layer",
                    description = "Single-responsibility Use Cases for Sessions, Sets, and Exercises ready for MVP Core (Sprint 1).",
                    icon = Icons.Default.CheckCircle
                )
            }

            item {
                LayerCheckItem(
                    layerName = "DI & Presentation",
                    description = "AppContainer injection, StateFlow state holders, Material 3 athletic theme, and Jetpack Compose UI.",
                    icon = Icons.Default.Sync
                )
            }

            // Pre-populated Exercises Preview
            item {
                Text(
                    text = "Pre-populated Default Exercises (${0})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(emptyList<com.example.domain.model.Exercise>()) { exercise ->
                ExerciseListItem(exercise = exercise)
            }

            // Recorded Sessions Preview
            item {
                Text(
                    text = "Local Sessions Recorded (${sessionCount.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (sessionCount.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "아직 기록된 세션이 없습니다.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "위의 '세션 생성 테스트' 버튼을 눌러 로컬 Room DB 저장을 즉시 확인하세요.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(sessionCount) { session ->
                    SessionListItem(session = session)
                }
            }
        }
    }
}

@Composable
private fun ArchitectureOverviewCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFD0BCFF),
            contentColor = Color(0xFF21005D)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = Color(0xFF21005D)
                )
                Text(
                    text = "Clean Architecture + Repository Pattern",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Strength Log의 도메인/인프라/애플리케이션 계층이 완벽히 분리되어 있어, 향후 스프린트(CRUD → 통계 → 종목관리 → WorkManager 동기화 → Firestore)로 안전하게 확장됩니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF21005D).copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun DatabaseStatusCard(
    isInitialized: Boolean,
    sessionCount: Int,
    exerciseCount: Int,
    pendingUploadCount: Int,
    lastMessage: String?,
    onCreateSampleSession: () -> Unit,
    onTriggerSync: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8DEF8),
            contentColor = Color(0xFF1D192B)
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "로컬 DB 영속성 상태",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isInitialized) Color(0xFF00E676).copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = if (isInitialized) "ACTIVE" else "ERROR",
                        color = if (isInitialized) Color(0xFF00E676) else Color.Red,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusMetricItem(label = "Sessions", value = sessionCount.toString(), color = Color(0xFF1D192B))
                StatusMetricItem(label = "Exercises", value = exerciseCount.toString(), color = Color(0xFF1D192B))
                StatusMetricItem(label = "Sync Queue", value = pendingUploadCount.toString(), color = Color(0xFF1D192B))
            }

            if (!lastMessage.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = lastMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF1D192B),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onCreateSampleSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_session_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF21005D),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("세션 생성 테스트", fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onTriggerSync,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trigger_sync_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6750A4),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("수동 동기화 (Sync Queue)", fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun StatusMetricItem(label: String, value: String, color: Color = MaterialTheme.colorScheme.primary) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color.copy(alpha = 0.7f),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun LayerCheckItem(
    layerName: String,
    description: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0))
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
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF7F2FA)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF6750A4),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = layerName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1D1B20)
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF49454F),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Verified",
                tint = Color(0xFF6750A4),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ExerciseListItem(exercise: Exercise) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1D1B20)
                )
                Text(
                    text = "부위: ${exercise.muscleGroup}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF49454F),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFF7F2FA)
            ) {
                Text(
                    text = if (exercise.isCustom) "Custom" else "Default",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF6750A4),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SessionListItem(session: WorkoutSession) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val dateStr = dateFormat.format(Date(session.startTime))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF3E7FF)
        ),
        shape = RoundedCornerShape(16.dp)
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
                    text = "Session #${session.id.take(8)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6750A4)
                )
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF6750A4).copy(alpha = 0.8f)
                )
            }
            if (session.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = session.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF21005D)
                )
            }
        }
    }
}
