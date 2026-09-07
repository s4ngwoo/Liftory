package com.example.presentation.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.PersonalRecord
import com.example.domain.model.WorkoutVolume
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsDashboardScreen(
    viewModel: StatisticsViewModel,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    val volumes by viewModel.volumeFlow.collectAsStateWithLifecycle()
    val personalRecords by viewModel.personalRecordsFlow.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Statistics", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = onExportJson) { Text("Export JSON") }
                    TextButton(onClick = onExportCsv) { Text("Export CSV") }
                }
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
            item {
                Text(
                    text = "Workout Volume (Last 30 Days)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                VolumeChart(volumes)
            }

            item {
                Text(
                    text = "Personal Records",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            items(personalRecords) { pr ->
                PersonalRecordCard(pr)
            }
        }
    }
}

@Composable
fun VolumeChart(volumes: List<WorkoutVolume>) {
    // A simplified representation of a chart. In a real app, use Vico or Compose-Charts.
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                Text("No data available for this period.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun PersonalRecordCard(pr: PersonalRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(pr.exerciseId, fontWeight = FontWeight.Bold)
            Text("${pr.maxWeight} kg", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}
