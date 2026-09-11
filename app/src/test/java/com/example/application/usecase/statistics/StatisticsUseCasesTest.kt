package com.example.application.usecase.statistics

import com.example.domain.model.PersonalRecord
import com.example.domain.model.WorkoutVolume
import com.example.domain.repository.DataExporter
import com.example.domain.repository.StatisticsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeStatisticsRepository : StatisticsRepository {
    val volumes = mutableListOf<WorkoutVolume>()
    val prs = mutableListOf<PersonalRecord>()

    override fun observeVolumeByPeriod(startDate: Long, endDate: Long): Flow<List<WorkoutVolume>> {
        return flowOf(volumes.filter { it.dateMillis in startDate..endDate })
    }

    override fun observePersonalRecords(): Flow<List<PersonalRecord>> {
        return flowOf(prs)
    }
}

class FakeDataExporter : DataExporter {
    var jsonContent = "{\"sessions\":[]}"
    var csvContent = "sessionId,exerciseId,weight,reps,rpe"

    override suspend fun exportDataAsJson(): Result<String> = Result.success(jsonContent)
    override suspend fun exportDataAsCsv(): Result<String> = Result.success(csvContent)
}

class StatisticsUseCasesTest {

    private lateinit var statisticsRepository: FakeStatisticsRepository
    private lateinit var dataExporter: FakeDataExporter

    private lateinit var calculateWorkoutVolumeUseCase: CalculateWorkoutVolumeUseCase
    private lateinit var calculatePersonalRecordsUseCase: CalculatePersonalRecordsUseCase
    private lateinit var exportWorkoutDataUseCase: ExportWorkoutDataUseCase

    @Before
    fun setUp() {
        statisticsRepository = FakeStatisticsRepository()
        dataExporter = FakeDataExporter()

        calculateWorkoutVolumeUseCase = CalculateWorkoutVolumeUseCase(statisticsRepository)
        calculatePersonalRecordsUseCase = CalculatePersonalRecordsUseCase(statisticsRepository)
        exportWorkoutDataUseCase = ExportWorkoutDataUseCase(dataExporter)
    }

    @Test
    fun `calculateWorkoutVolumeUseCase should filter volume by time range`() = runTest {
        statisticsRepository.volumes.add(WorkoutVolume(dateMillis = 1000L, totalVolume = 2500.0))
        statisticsRepository.volumes.add(WorkoutVolume(dateMillis = 2000L, totalVolume = 3200.0))
        statisticsRepository.volumes.add(WorkoutVolume(dateMillis = 5000L, totalVolume = 4000.0))

        val result = calculateWorkoutVolumeUseCase(startDate = 1500L, endDate = 3000L).first()
        assertEquals(1, result.size)
        assertEquals(3200.0, result[0].totalVolume, 0.0)
    }

    @Test
    fun `calculatePersonalRecordsUseCase should return stream of PRs`() = runTest {
        statisticsRepository.prs.add(PersonalRecord(exerciseId = "ex_squat", maxWeight = 140.0, achievedAt = 2000L))
        statisticsRepository.prs.add(PersonalRecord(exerciseId = "ex_bench", maxWeight = 100.0, achievedAt = 2100L))

        val result = calculatePersonalRecordsUseCase().first()
        assertEquals(2, result.size)
        assertEquals("ex_squat", result[0].exerciseId)
        assertEquals(140.0, result[0].maxWeight, 0.0)
    }

    @Test
    fun `exportWorkoutDataUseCase should export json and csv data`() = runTest {
        val jsonResult = exportWorkoutDataUseCase.exportAsJson()
        assertTrue(jsonResult.isSuccess)
        assertEquals("{\"sessions\":[]}", jsonResult.getOrNull())

        val csvResult = exportWorkoutDataUseCase.exportAsCsv()
        assertTrue(csvResult.isSuccess)
        val csv = csvResult.getOrNull() ?: ""
        assertTrue(csv.startsWith("sessionId,exerciseId", ignoreCase = false))
    }
}
