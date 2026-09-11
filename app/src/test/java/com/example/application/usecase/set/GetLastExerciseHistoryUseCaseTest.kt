package com.example.application.usecase.set

import com.example.domain.model.ExerciseHistoryRecord
import com.example.domain.model.ExerciseSet
import com.example.domain.model.ExerciseSetSummary
import com.example.domain.repository.ExerciseSetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class FakeExerciseHistorySetRepository : ExerciseSetRepository {
    val historyMap = mutableMapOf<String, ExerciseHistoryRecord>()

    override suspend fun create(set: ExerciseSet): Result<ExerciseSet> = Result.success(set)
    override suspend fun update(set: ExerciseSet): Result<Unit> = Result.success(Unit)
    override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    override fun observeBySession(sessionId: String): Flow<List<ExerciseSet>> = flowOf(emptyList())
    override suspend fun getBySession(sessionId: String): List<ExerciseSet> = emptyList()

    override suspend fun getLastHistoryForExercise(
        exerciseId: String,
        currentSessionId: String?
    ): ExerciseHistoryRecord? {
        val record = historyMap[exerciseId]
        return if (record != null && record.sessionId != currentSessionId) record else null
    }
}

class GetLastExerciseHistoryUseCaseTest {

    private lateinit var repository: FakeExerciseHistorySetRepository
    private lateinit var getLastExerciseHistoryUseCase: GetLastExerciseHistoryUseCase

    @Before
    fun setUp() {
        repository = FakeExerciseHistorySetRepository()
        getLastExerciseHistoryUseCase = GetLastExerciseHistoryUseCase(repository)
    }

    @Test
    fun `returns last session sets and date for an exercise excluding current session`() = runTest {
        val pastRecord = ExerciseHistoryRecord(
            exerciseId = "ex_bench",
            sessionId = "prev_session_1",
            sessionDate = 1700000000000L,
            sets = listOf(
                ExerciseSetSummary(setNumber = 1, weight = 100.0, reps = 5, rpe = 8.5),
                ExerciseSetSummary(setNumber = 2, weight = 100.0, reps = 5, rpe = 9.0),
                ExerciseSetSummary(setNumber = 3, weight = 95.0, reps = 6, rpe = 9.5)
            )
        )
        repository.historyMap["ex_bench"] = pastRecord

        val history = getLastExerciseHistoryUseCase(
            exerciseId = "ex_bench",
            currentSessionId = "current_session_2"
        )

        assertNotNull(history)
        assertEquals("prev_session_1", history!!.sessionId)
        assertEquals(1700000000000L, history.sessionDate)
        assertEquals(3, history.sets.size)
        assertEquals(100.0, history.sets[0].weight, 0.0)
        assertEquals(8.5, history.sets[0].rpe)
    }

    @Test
    fun `returns null when no previous history exists or only in current session`() = runTest {
        val currentRecord = ExerciseHistoryRecord(
            exerciseId = "ex_squat",
            sessionId = "session_active",
            sessionDate = 1700000000000L,
            sets = listOf(ExerciseSetSummary(1, 140.0, 3, 9.0))
        )
        repository.historyMap["ex_squat"] = currentRecord

        val history = getLastExerciseHistoryUseCase(
            exerciseId = "ex_squat",
            currentSessionId = "session_active"
        )

        assertNull(history)
    }
}
