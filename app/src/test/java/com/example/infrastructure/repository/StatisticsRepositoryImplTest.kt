package com.example.infrastructure.repository

import com.example.domain.model.PersonalRecord
import com.example.domain.model.WorkoutVolume
import com.example.infrastructure.db.dao.ExerciseSetDao
import com.example.infrastructure.db.entity.ExerciseSetEntity
import com.example.infrastructure.db.entity.PastSetTuple
import com.example.infrastructure.db.entity.PersonalRecordTuple
import com.example.infrastructure.db.entity.SessionVolumeTuple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeExerciseSetDao : ExerciseSetDao {
    var volumeTuples = listOf<SessionVolumeTuple>()
    var prTuples = listOf<PersonalRecordTuple>()

    override fun observeVolumeByPeriod(startDate: Long, endDate: Long): Flow<List<SessionVolumeTuple>> {
        return flowOf(volumeTuples)
    }

    override fun observePersonalRecords(): Flow<List<PersonalRecordTuple>> {
        return flowOf(prTuples)
    }

    override fun observeBySession(sessionId: String): Flow<List<ExerciseSetEntity>> = flowOf(emptyList())
    override suspend fun getBySession(sessionId: String): List<ExerciseSetEntity> = emptyList()
    override suspend fun getById(id: String): ExerciseSetEntity? = null
    override suspend fun insert(set: ExerciseSetEntity) {}
    override suspend fun update(set: ExerciseSetEntity) {}
    override suspend fun deleteById(id: String) {}
    override suspend fun deleteBySessionId(sessionId: String) {}
    override suspend fun getMaxWeightForExercise(exerciseId: String): Double? = null
    override suspend fun getVolumeForSessions(sessionIds: List<String>): Double? = null
    override suspend fun getPastSetsForExercise(exerciseId: String, currentSessionId: String?): List<PastSetTuple> = emptyList()
}

class StatisticsRepositoryImplTest {

    private lateinit var fakeDao: FakeExerciseSetDao
    private lateinit var repository: StatisticsRepositoryImpl

    @Before
    fun setUp() {
        fakeDao = FakeExerciseSetDao()
        repository = StatisticsRepositoryImpl(
            exerciseSetDao = fakeDao,
            ioDispatcher = Dispatchers.Unconfined
        )
    }

    @Test
    fun `observeVolumeByPeriod should cleanly separate strength volume from cardio duration minutes`() = runTest {
        // Bench Press 800kg + Cardio 15 min
        fakeDao.volumeTuples = listOf(
            SessionVolumeTuple(
                startTime = 1000L,
                totalVolume = 800.0,
                cardioMinutes = 15
            )
        )

        val volumes = repository.observeVolumeByPeriod(0L, 2000L).first()
        assertEquals(1, volumes.size)
        assertEquals(800.0, volumes[0].totalVolume, 0.0)
        assertEquals(15, volumes[0].cardioDurationMinutes)
    }

    @Test
    fun `observePersonalRecords should map human-readable name and separate cardio from strength PRs`() = runTest {
        fakeDao.prTuples = listOf(
            PersonalRecordTuple(
                exerciseId = "ex_bench",
                exerciseName = "벤치프레스",
                equipmentType = "FREE_WEIGHT",
                maxWeight = 80.0,
                maxCardioLevel = null,
                maxCardioMinutes = null,
                achievedAt = 2000L
            ),
            PersonalRecordTuple(
                exerciseId = "ex_stairmaster",
                exerciseName = "천국의 계단 (스텝밀)",
                equipmentType = "CARDIO",
                maxWeight = 0.0,
                maxCardioLevel = 8.0,
                maxCardioMinutes = 15,
                achievedAt = 2500L
            )
        )

        val prs = repository.observePersonalRecords().first()
        assertEquals(2, prs.size)

        // Verify strength PR
        val strengthPr = prs[0]
        assertEquals("ex_bench", strengthPr.exerciseId)
        assertEquals("벤치프레스", strengthPr.exerciseName)
        assertFalse(strengthPr.isCardio)
        assertEquals(80.0, strengthPr.maxWeight, 0.0)

        // Verify cardio PR
        val cardioPr = prs[1]
        assertEquals("ex_stairmaster", cardioPr.exerciseId)
        assertEquals("천국의 계단 (스텝밀)", cardioPr.exerciseName)
        assertTrue(cardioPr.isCardio)
        assertEquals(8.0, cardioPr.maxCardioLevel ?: 0.0, 0.0)
        assertEquals(15, cardioPr.maxCardioMinutes ?: 0)
    }
}
