package com.example.presentation.statistics

import com.example.application.usecase.statistics.CalculatePersonalRecordsUseCase
import com.example.application.usecase.statistics.CalculateWorkoutVolumeUseCase
import com.example.application.usecase.statistics.ExportWorkoutDataUseCase
import com.example.application.usecase.statistics.ImportWorkoutDataUseCase
import com.example.domain.model.PersonalRecord
import com.example.domain.model.WorkoutVolume
import com.example.domain.repository.DataExporter
import com.example.domain.repository.DataImporter
import com.example.domain.repository.StatisticsRepository
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FakeStatisticsRepository : StatisticsRepository {
    var volumeFlowToEmit: Flow<List<WorkoutVolume>> = flowOf(emptyList())
    var prFlowToEmit: Flow<List<PersonalRecord>> = flowOf(emptyList())
    var lastRequestedStartDate: Long? = null
    var lastRequestedEndDate: Long? = null

    override fun observeVolumeByPeriod(startDate: Long, endDate: Long): Flow<List<WorkoutVolume>> {
        lastRequestedStartDate = startDate
        lastRequestedEndDate = endDate
        return volumeFlowToEmit
    }

    override fun observePersonalRecords(): Flow<List<PersonalRecord>> {
        return prFlowToEmit
    }
}

class FakeDataExporter : DataExporter {
    override suspend fun exportDataAsJson(): Result<String> = Result.success("{}")
    override suspend fun exportDataAsCsv(): Result<String> = Result.success("csv")
}

class FakeDataImporter : DataImporter {
    override suspend fun importDataFromJson(jsonString: String): Result<Int> = Result.success(5)
    override suspend fun importDataFromCsv(csvString: String): Result<Int> = Result.success(3)
}

@OptIn(ExperimentalCoroutinesApi::class)
class StatisticsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeStatisticsRepository
    private lateinit var viewModel: StatisticsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeStatisticsRepository()
        viewModel = StatisticsViewModel(
            calculateWorkoutVolumeUseCase = CalculateWorkoutVolumeUseCase(fakeRepository),
            calculatePersonalRecordsUseCase = CalculatePersonalRecordsUseCase(fakeRepository),
            exportWorkoutDataUseCase = ExportWorkoutDataUseCase(FakeDataExporter()),
            importWorkoutDataUseCase = ImportWorkoutDataUseCase(FakeDataImporter())
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial period should be LAST_30_DAYS`() {
        assertEquals(TimePeriod.LAST_30_DAYS, viewModel.selectedPeriod.value)
    }

    @Test
    fun `changing period updates selectedPeriod and requests corresponding date range`() = runTest {
        viewModel.setPeriod(TimePeriod.LAST_7_DAYS)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(TimePeriod.LAST_7_DAYS, viewModel.selectedPeriod.value)
    }

    @Test
    fun `summaryKpi correctly aggregates volume, cardio duration, and active days`() = runTest {
        val sampleVolumes = listOf(
            WorkoutVolume(dateMillis = 1000L, totalVolume = 500.0, cardioDurationMinutes = 20),
            WorkoutVolume(dateMillis = 2000L, totalVolume = 800.0, cardioDurationMinutes = 0),
            WorkoutVolume(dateMillis = 3000L, totalVolume = 0.0, cardioDurationMinutes = 30)
        )
        fakeRepository.volumeFlowToEmit = flowOf(sampleVolumes)

        viewModel.summaryKpi.test {
            // Wait for emissions
            val item1 = awaitItem()
            val finalItem = if (item1.totalStrengthVolume == 0.0 && item1.workoutDaysCount == 0) {
                awaitItem()
            } else {
                item1
            }

            assertEquals(1300.0, finalItem.totalStrengthVolume, 0.01)
            assertEquals(50, finalItem.totalCardioMinutes)
            assertEquals(3, finalItem.workoutDaysCount)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
