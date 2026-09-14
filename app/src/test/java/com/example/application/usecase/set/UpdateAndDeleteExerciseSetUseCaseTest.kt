package com.example.application.usecase.set

import com.example.domain.model.ExerciseSet
import com.example.domain.model.WorkoutSession
import com.example.presentation.session.FakeSessionRepository
import com.example.presentation.session.FakeSetRepository
import com.example.presentation.session.FakeTransactionProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RecordingFakeSetRepository : FakeSetRepository() {
    val deletedIds = mutableListOf<String>()
    var updateShouldFail = false
    var deleteShouldFail = false

    override suspend fun update(set: ExerciseSet): Result<Unit> {
        if (updateShouldFail) return Result.failure(IllegalStateException("update failed"))
        return super.update(set)
    }

    override suspend fun delete(id: String): Result<Unit> {
        if (deleteShouldFail) return Result.failure(IllegalStateException("delete failed"))
        deletedIds.add(id)
        return super.delete(id)
    }
}

class UpdateAndDeleteExerciseSetUseCaseTest {

    private lateinit var setRepository: RecordingFakeSetRepository
    private lateinit var sessionRepository: FakeSessionRepository
    private lateinit var updateExerciseSetUseCase: UpdateExerciseSetUseCase
    private lateinit var deleteExerciseSetUseCase: DeleteExerciseSetUseCase

    @Before
    fun setup() {
        setRepository = RecordingFakeSetRepository()
        sessionRepository = FakeSessionRepository()
        val tx = FakeTransactionProvider()
        updateExerciseSetUseCase = UpdateExerciseSetUseCase(setRepository, sessionRepository, tx)
        deleteExerciseSetUseCase = DeleteExerciseSetUseCase(setRepository, sessionRepository, tx)
        sessionRepository.sessions.add(
            WorkoutSession(
                id = "session_1",
                startTime = 1_000L,
                endTime = null,
                notes = "",
                createdAt = 1_000L,
                updatedAt = 1_000L
            )
        )
    }

    @Test
    fun `update bumps parent session updatedAt on success`() = runTest {
        val set = ExerciseSet(
            id = "set_1",
            sessionId = "session_1",
            exerciseId = "ex_squat",
            weight = 100.0,
            reps = 5,
            updatedAt = 1_000L
        )

        val result = updateExerciseSetUseCase(set)

        assertTrue(result.isSuccess)
        assertEquals("set_1", setRepository.updatedSet!!.id)
        assertEquals(100.0, setRepository.updatedSet!!.weight, 0.0)
        assertTrue(
            "set updatedAt must be rewritten",
            setRepository.updatedSet!!.updatedAt > 1_000L
        )
        val session = sessionRepository.updatedSession
        assertEquals("session_1", session!!.id)
        assertTrue("session updatedAt must advance for sync ordering", session.updatedAt > 1_000L)
    }

    @Test
    fun `update failure leaves session timestamp unchanged`() = runTest {
        setRepository.updateShouldFail = true
        val set = ExerciseSet(
            id = "set_1",
            sessionId = "session_1",
            exerciseId = "ex_squat",
            weight = 100.0,
            reps = 5,
            updatedAt = 1_000L
        )

        val result = updateExerciseSetUseCase(set)

        assertTrue(result.isFailure)
        assertNull(sessionRepository.updatedSession)
        assertEquals(1_000L, sessionRepository.sessions.first { it.id == "session_1" }.updatedAt)
    }

    @Test
    fun `delete bumps parent session updatedAt on success`() = runTest {
        val result = deleteExerciseSetUseCase("set_1", "session_1")

        assertTrue(result.isSuccess)
        assertEquals(listOf("set_1"), setRepository.deletedIds)
        val session = sessionRepository.updatedSession
        assertEquals("session_1", session!!.id)
        assertTrue(session.updatedAt > 1_000L)
    }

    @Test
    fun `delete failure leaves session timestamp unchanged`() = runTest {
        setRepository.deleteShouldFail = true

        val result = deleteExerciseSetUseCase("set_1", "session_1")

        assertTrue(result.isFailure)
        assertTrue(setRepository.deletedIds.isEmpty())
        assertNull(sessionRepository.updatedSession)
        assertEquals(1_000L, sessionRepository.sessions.first { it.id == "session_1" }.updatedAt)
    }
}
