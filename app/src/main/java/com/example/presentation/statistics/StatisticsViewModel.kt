package com.example.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.application.usecase.statistics.CalculatePersonalRecordsUseCase
import com.example.application.usecase.statistics.CalculateWorkoutVolumeUseCase
import com.example.application.usecase.statistics.ExportWorkoutDataUseCase
import com.example.application.usecase.statistics.ImportWorkoutDataUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class StatisticsViewModel(
    calculateWorkoutVolumeUseCase: CalculateWorkoutVolumeUseCase,
    calculatePersonalRecordsUseCase: CalculatePersonalRecordsUseCase,
    private val exportWorkoutDataUseCase: ExportWorkoutDataUseCase,
    private val importWorkoutDataUseCase: ImportWorkoutDataUseCase
) : ViewModel() {

    private val thirtyDaysAgo = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -30)
    }.timeInMillis

    val volumeFlow = calculateWorkoutVolumeUseCase(
        startDate = thirtyDaysAgo,
        endDate = System.currentTimeMillis()
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val personalRecordsFlow = calculatePersonalRecordsUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun exportAsJson(onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val result = exportWorkoutDataUseCase.exportAsJson()
            onResult(result)
        }
    }

    fun exportAsCsv(onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val result = exportWorkoutDataUseCase.exportAsCsv()
            onResult(result)
        }
    }

    fun importFromJson(jsonString: String, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val result = importWorkoutDataUseCase.importFromJson(jsonString)
            onResult(result)
        }
    }

    fun importFromCsv(csvString: String, onResult: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            val result = importWorkoutDataUseCase.importFromCsv(csvString)
            onResult(result)
        }
    }
}
