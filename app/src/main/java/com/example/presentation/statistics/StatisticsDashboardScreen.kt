package com.example.presentation.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.PersonalRecord
import com.example.domain.model.WorkoutVolume
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsDashboardScreen(
    viewModel: StatisticsViewModel,
    onExportJson: (() -> Unit)? = null,
    onExportCsv: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val volumes by viewModel.volumeFlow.collectAsStateWithLifecycle()
    val summaryKpi by viewModel.summaryKpi.collectAsStateWithLifecycle()
    val personalRecords by viewModel.personalRecordsFlow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var showImportDialog by remember { mutableStateOf(false) }
    var importType by remember { mutableStateOf("JSON") } // "JSON" or "CSV"
    var importInputText by remember { mutableStateOf("") }
    var importErrorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("운동 통계 & 성과", fontWeight = FontWeight.Bold)
                        Text(
                            "꾸준한 운동 기록과 성장을 한눈에 확인하세요",
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
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 🎯 [Zone 1: 기간 필터 & 핵심 성과 요약 (Bento KPI Grid)]
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 기간 선택 칩
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TimePeriod.values().forEach { period ->
                            FilterChip(
                                selected = selectedPeriod == period,
                                onClick = { viewModel.setPeriod(period) },
                                label = {
                                    Text(
                                        text = period.label,
                                        fontWeight = if (selectedPeriod == period) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    // 3열 Bento KPI 카드
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BentoKpiCard(
                            title = "총 볼륨",
                            value = "${summaryKpi.totalStrengthVolume.toInt()} kg",
                            icon = Icons.Default.FitnessCenter,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        BentoKpiCard(
                            title = "유산소",
                            value = "${summaryKpi.totalCardioMinutes} 분",
                            icon = Icons.AutoMirrored.Filled.DirectionsRun,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        BentoKpiCard(
                            title = "운동 일수",
                            value = "${summaryKpi.workoutDaysCount} 일",
                            icon = Icons.Default.CalendarToday,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 📈 [Zone 2: 일자별 볼륨 추이 (Workout Volume)]
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedPeriod.label} 볼륨 추이",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (volumes.isNotEmpty()) {
                            Text(
                                text = "총 ${volumes.size}일 기록",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    VolumeChart(volumes)
                }
            }

            // 🏆 [Zone 3: 종목별 최고 기록 (Personal Records)]
            item {
                Text(
                    text = "종목별 최고 기록 (Personal Records)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            if (personalRecords.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "아직 달성한 PR 기록이 없습니다.\n운동을 완료하면 최고 중량과 기록이 여기에 표시됩니다.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(personalRecords) { pr ->
                    PersonalRecordCard(pr)
                }
            }

            // ⚙️ [Zone 4: 최하단 데이터 백업 및 복원 (Data Management)]
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "데이터 백업 및 복원",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "운동 기록을 JSON 백업 파일로 복사하거나, 이전 백업에서 복원할 수 있습니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.exportAsJson { result ->
                                        result.onSuccess { json ->
                                            clipboardManager.setText(AnnotatedString(json))
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("JSON 백업 데이터가 클립보드에 복사되었습니다.")
                                            }
                                        }.onFailure { err ->
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("내보내기 실패: ${err.message}")
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("JSON 백업", style = MaterialTheme.typography.labelMedium)
                            }

                            Button(
                                onClick = {
                                    importType = "JSON"
                                    importInputText = ""
                                    importErrorMessage = null
                                    showImportDialog = true
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("백업 복원", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    // Import Dialog with live Validation & Preview
    if (showImportDialog) {
        val parsedPreview = remember(importInputText) {
            if (importInputText.isBlank()) null
            else {
                try {
                    val root = JSONObject(importInputText)
                    val sessionsArray = root.optJSONArray("sessions")
                    val setsArray = root.optJSONArray("exercise_sets") ?: root.optJSONArray("sets")
                    val sessionCount = sessionsArray?.length() ?: 0
                    val setCount = setsArray?.length() ?: 0
                    if (sessionCount > 0 || setCount > 0) {
                        "✅ 확인됨: 세션 ${sessionCount}개 · 세트 ${setCount}개 포함"
                    } else {
                        "⚠️ 세션 또는 세트 데이터가 비어 있습니다."
                    }
                } catch (e: Exception) {
                    "❌ 유효한 JSON 포맷이 아닙니다."
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("$importType 백업 데이터 복원", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "이전에 복사해둔 $importType 백업 텍스트를 붙여넣어 주세요.\n복원 시 기존 데이터는 보존되며 새 기록이 안전하게 병합됩니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = importInputText,
                        onValueChange = {
                            importInputText = it
                            importErrorMessage = null
                        },
                        label = { Text("$importType 텍스트") },
                        placeholder = { Text("여기에 백업 데이터를 붙여넣으세요...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        maxLines = 8,
                        isError = importErrorMessage != null
                    )

                    if (parsedPreview != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = parsedPreview,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (parsedPreview.startsWith("✅")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }

                    if (importErrorMessage != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = importErrorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importInputText.isBlank()) {
                            importErrorMessage = "데이터를 입력해 주세요."
                            return@Button
                        }

                        if (importType == "JSON") {
                            viewModel.importFromJson(importInputText) { result ->
                                result.onSuccess { count ->
                                    showImportDialog = false
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("성공적으로 ${count}개의 기록을 복원했습니다!")
                                    }
                                }.onFailure { err ->
                                    importErrorMessage = "복원 실패: ${err.message}"
                                }
                            }
                        } else {
                            viewModel.importFromCsv(importInputText) { result ->
                                result.onSuccess { count ->
                                    showImportDialog = false
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("성공적으로 ${count}개의 기록을 복원했습니다!")
                                    }
                                }.onFailure { err ->
                                    importErrorMessage = "복원 실패: ${err.message}"
                                }
                            }
                        }
                    },
                    enabled = parsedPreview?.startsWith("✅") == true
                ) {
                    Text("복원하기")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
fun BentoKpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor.copy(alpha = 0.85f)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.85f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = contentColor
            )
        }
    }
}

@Composable
fun VolumeChart(volumes: List<WorkoutVolume>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            volumes.forEach { volume ->
                val dateFormat = SimpleDateFormat("MM-dd (E)", Locale.KOREA)
                val dateStr = dateFormat.format(Date(volume.dateMillis))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        dateStr,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (volume.totalVolume > 0) {
                            Text(
                                "근력 ${volume.totalVolume.toInt()} kg",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (volume.cardioDurationMinutes > 0) {
                            Text(
                                "· 유산소 ${volume.cardioDurationMinutes}분",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            if (volumes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "해당 기간 동안의 볼륨 기록이 없습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PersonalRecordCard(pr: PersonalRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pr.exerciseName.ifBlank { pr.exerciseId },
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (pr.isCardio) {
                    Text(
                        text = "🏃 유산소 종목",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "🏋️ 근력 종목",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            if (pr.isCardio) {
                val levelStr = pr.maxCardioLevel?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "-"
                val minStr = pr.maxCardioMinutes?.toString() ?: "0"
                Text(
                    text = "최고 레벨 $levelStr · 최장 ${minStr}분",
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = "최고 ${pr.maxWeight} kg",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
