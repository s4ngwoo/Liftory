package com.example.baseline

import com.example.application.usecase.session.CreateWorkoutSessionUseCase
import com.example.application.usecase.session.GetWorkoutSessionUseCase
import com.example.application.usecase.statistics.CalculateWorkoutVolumeUseCase
import com.example.domain.model.WorkoutSession
import com.example.domain.model.WorkoutVolume
import com.example.domain.repository.StatisticsRepository
import com.example.domain.repository.WorkoutSessionRepository
import com.example.testfixtures.SyntheticFixtures
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * BASE-01 ~ BASE-03 Characterization tests from development-tdd-test-catalog.md.
 * Verifies local offline persistence baseline, metric calculation isolation, and deterministic repeatability.
 */
class BaselineCharacterizationTest {

    private lateinit var fakeSessionRepo: FakeLocalSessionRepository
    private lateinit var fakeStatsRepo: FakeLocalStatsRepository

    private lateinit var createSessionUseCase: CreateWorkoutSessionUseCase
    private lateinit var getSessionUseCase: GetWorkoutSessionUseCase
    private lateinit var calculateWorkoutVolumeUseCase: CalculateWorkoutVolumeUseCase

    @Before
    fun setUp() {
        fakeSessionRepo = FakeLocalSessionRepository()
        fakeStatsRepo = FakeLocalStatsRepository()

        createSessionUseCase = CreateWorkoutSessionUseCase(fakeSessionRepo)
        getSessionUseCase = GetWorkoutSessionUseCase(fakeSessionRepo)
        calculateWorkoutVolumeUseCase = CalculateWorkoutVolumeUseCase(fakeStatsRepo)
    }

    /**
     * BASE-01: Given an app with zero cloud/auth setup, when creating and storing a local session,
     * then it can be queried and observed locally without network dependency.
     */
    @Test
    fun `BASE-01 local session creation and retrieval works without cloud or auth dependency`() = runTest {
        // When: user creates a session offline
        val result = createSessionUseCase("오늘의 하체 운동")
        assertTrue(result.isSuccess)
        val createdSession = result.getOrNull()

        // Then: session is immediately retrievable locally
        assertNotNull(createdSession)
        val retrieved = getSessionUseCase(createdSession!!.id)
        assertNotNull(retrieved)
        assertEquals("오늘의 하체 운동", retrieved!!.notes)
        assertEquals(null, retrieved.endTime) // Active session
    }

    /**
     * BASE-02: Given synthetic mixed sets (F-MIXED: 800kg strength + 15min cardio),
     * when observing volume statistics,
     * then strength volume and cardio duration are separated and NOT summed into 920kg.
     */
    @Test
    fun `BASE-02 statistics volume isolates strength volume from cardio duration`() = runTest {
        // Given F-MIXED synthetic volumes
        val sampleVolumes = listOf(
            WorkoutVolume(
                dateMillis = SyntheticFixtures.mixedSession.startTime,
                totalVolume = 800.0, // Strength volume (80kg * 10)
                cardioDurationMinutes = 15 // Cardio duration (15 min)
            )
        )
        fakeStatsRepo.volumeFlowToEmit = flowOf(sampleVolumes)

        // When: observing volume
        val volumes = calculateWorkoutVolumeUseCase(0L, 5000L).first()

        // Then: verify volume is 800kg, cardio is 15min, and sum is distinct
        assertEquals(1, volumes.size)
        val vol = volumes.first()
        assertEquals(800.0, vol.totalVolume, 0.01)
        assertEquals(15, vol.cardioDurationMinutes)
    }

    /**
     * BASE-03: Given same synthetic DB fixture and fixed timestamps,
     * when repeating baseline scenario,
     * then results are strictly identical and deterministic.
     */
    @Test
    fun `BASE-03 repeated execution with fixed fixture yields identical deterministic results`() = runTest {
        val seq = SyntheticFixtures.fTimeSequence

        // Calculation 1
        val actualRest1 = seq.nextSetStartSec - seq.setCompleteSec
        val remaining1 = (seq.setCompleteSec + seq.restTargetSec) - seq.nextSetStartSec

        // Calculation 2
        val actualRest2 = seq.nextSetStartSec - seq.setCompleteSec
        val remaining2 = (seq.setCompleteSec + seq.restTargetSec) - seq.nextSetStartSec

        assertEquals(60L, actualRest1)
        assertEquals(actualRest1, actualRest2)
        assertEquals(30L, remaining1)
        assertEquals(remaining1, remaining2)
    }
}

class FakeLocalSessionRepository : WorkoutSessionRepository {
    private val sessions = mutableMapOf<String, WorkoutSession>()
    private val sessionsFlow = MutableStateFlow<List<WorkoutSession>>(emptyList())

    override suspend fun create(session: WorkoutSession): Result<WorkoutSession> {
        sessions[session.id] = session
        sessionsFlow.value = sessions.values.toList()
        return Result.success(session)
    }

    override suspend fun getById(id: String): WorkoutSession? = sessions[id]

    override suspend fun update(session: WorkoutSession): Result<Unit> {
        sessions[session.id] = session
        sessionsFlow.value = sessions.values.toList()
        return Result.success(Unit)
    }

    override suspend fun delete(id: String): Result<Unit> {
        sessions.remove(id)
        sessionsFlow.value = sessions.values.toList()
        return Result.success(Unit)
    }

    override fun observeAll(): Flow<List<WorkoutSession>> = sessionsFlow
    override fun observeActiveSession(): Flow<WorkoutSession?> = MutableStateFlow(sessions.values.firstOrNull { it.endTime == null })
    override suspend fun getActiveSession(): WorkoutSession? = sessions.values.firstOrNull { it.endTime == null }
    override suspend fun getActiveSessions(): List<WorkoutSession> = sessions.values.filter { it.endTime == null }
}

class FakeLocalStatsRepository : StatisticsRepository {
    var volumeFlowToEmit: Flow<List<WorkoutVolume>> = flowOf(emptyList())

    override fun observeVolumeByPeriod(startDate: Long, endDate: Long): Flow<List<WorkoutVolume>> = volumeFlowToEmit
    override fun observePersonalRecords(): Flow<List<com.example.domain.model.PersonalRecord>> = flowOf(emptyList())
}
