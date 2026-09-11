package com.example.presentation.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.application.usecase.statistics.CalculatePersonalRecordsUseCase
import com.example.application.usecase.statistics.CalculateWorkoutVolumeUseCase
import com.example.application.usecase.statistics.ExportWorkoutDataUseCase
import com.example.application.usecase.statistics.ImportWorkoutDataUseCase
import com.example.domain.model.WorkoutVolume
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class TimePeriod(val label: String, val days: Int) {
    LAST_7_DAYS("최근 7일", 7),
    LAST_30_DAYS("최근 30일", 30),
    LAST_90_DAYS("최근 3개월", 90),
    ALL_TIME("전체", 0)
}

data class StatisticsSummaryKpi(
    val totalStrengthVolume: Double = 0.0,
    val totalCardioMinutes: Int = 0,
    val workoutDaysCount: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModel(
    private val calculateWorkoutVolumeUseCase: CalculateWorkoutVolumeUseCase,
    calculatePersonalRecordsUseCase: CalculatePersonalRecordsUseCase,
    private val exportWorkoutDataUseCase: ExportWorkoutDataUseCase,
    private val importWorkoutDataUseCase: ImportWorkoutDataUseCase
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(TimePeriod.LAST_30_DAYS)
    val selectedPeriod: StateFlow<TimePeriod> = _selectedPeriod.asStateFlow()

    val volumeFlow: StateFlow<List<WorkoutVolume>> = _selectedPeriod
        .flatMapLatest { period ->
            val now = System.currentTimeMillis()
            val startDate = if (period.days > 0) {
                Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -period.days)
                }.timeInMillis
            } else {
                0L
            }
            calculateWorkoutVolumeUseCase(startDate = startDate, endDate = now)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val summaryKpi: StateFlow<StatisticsSummaryKpi> = volumeFlow
        .map { volumes ->
            StatisticsSummaryKpi(
                totalStrengthVolume = volumes.sumOf { it.totalVolume },
                totalCardioMinutes = volumes.sumOf { it.cardioDurationMinutes },
                workoutDaysCount = volumes.count { it.totalVolume > 0 || it.cardioDurationMinutes > 0 }
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StatisticsSummaryKpi()
        )

    val personalRecordsFlow = calculatePersonalRecordsUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setPeriod(period: TimePeriod) {
        _selectedPeriod.value = period
    }

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
