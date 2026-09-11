package com.example.presentation.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.PersonalRecord
import com.example.domain.model.WorkoutVolume
import kotlinx.coroutines.launch
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
    val volumes by viewModel.volumeFlow.collectAsStateWithLifecycle()
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
                title = { Text("통계 & 기록 (Stats)", fontWeight = FontWeight.Bold) },
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
            // Data Backup & Restore Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "데이터 백업 및 복원 (Backup & Restore)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "운동 기록을 JSON/CSV로 백업하거나 이전 백업에서 복원할 수 있습니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Export Button
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

                            // Import Button
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
                                Text("JSON 복원", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // Volume Chart
            item {
                Text(
                    text = "최근 30일 볼륨 (Workout Volume)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                VolumeChart(volumes)
            }

            // Personal Records (PR)
            item {
                Text(
                    text = "종목별 최고 기록 (Personal Records)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (personalRecords.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("아직 달성한 PR 기록이 없습니다.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            } else {
                items(personalRecords) { pr ->
                    PersonalRecordCard(pr)
                }
            }
        }
    }

    // Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("$importType 백업 데이터 복원", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "이전에 복사해둔 $importType 백업 텍스트를 붙여넣어 주세요.",
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
                    }
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
fun VolumeChart(volumes: List<WorkoutVolume>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            volumes.forEach { volume ->
                val dateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())
                val dateStr = dateFormat.format(Date(volume.dateMillis))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(dateStr, style = MaterialTheme.typography.bodySmall)
                    Text("${volume.totalVolume} kg", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
            if (volumes.isEmpty()) {
                Text("해당 기간 동안의 볼륨 기록이 없습니다.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun PersonalRecordCard(pr: PersonalRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(pr.exerciseId, fontWeight = FontWeight.Bold)
            Text("${pr.maxWeight} kg", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}
