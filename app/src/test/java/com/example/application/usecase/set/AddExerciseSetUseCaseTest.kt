package com.example.application.usecase.set

import com.example.domain.model.ExerciseSet
import com.example.domain.model.WorkoutSession
import com.example.presentation.session.FakeSessionRepository
import com.example.presentation.session.FakeSetRepository
import com.example.presentation.session.FakeTransactionProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class AddExerciseSetUseCaseTest {

    private lateinit var setRepository: FakeSetRepository
    private lateinit var sessionRepository: FakeSessionRepository
    private lateinit var transactionProvider: FakeTransactionProvider
    private lateinit var addExerciseSetUseCase: AddExerciseSetUseCase

    @Before
    fun setup() {
        setRepository = FakeSetRepository()
        sessionRepository = FakeSessionRepository()
        transactionProvider = FakeTransactionProvider()
        addExerciseSetUseCase = AddExerciseSetUseCase(setRepository, sessionRepository, transactionProvider)
    }

    @Test
    fun `invoke should save set and update session timestamp on success`() = runTest {
        val sessionId = "session_1"
        
        val result = addExerciseSetUseCase(
            sessionId = sessionId,
            exerciseId = "ex_squat",
            weight = 100.0,
            reps = 5
        )

        assertTrue(result.isSuccess)

        // Verify set was created
        assertNotNull(setRepository.createdSet)
        assertEquals(100.0, setRepository.createdSet!!.weight, 0.0)

        // Verify session timestamp was updated
        val updatedSession = sessionRepository.updatedSession
        assertNotNull(updatedSession)
        assertEquals(sessionId, updatedSession!!.id)
        assertTrue("updatedAt should be greater than original", updatedSession.updatedAt > 0L)
    }

    @Test
    fun `delete should remove set and bump session timestamp`() = runTest {
        val deleteUseCase = DeleteExerciseSetUseCase(
            setRepository,
            sessionRepository,
            transactionProvider
        )
        val sessionId = "session_1"
        sessionRepository.sessions.add(
            WorkoutSession(id = sessionId, startTime = 0L, updatedAt = 1L)
        )

        val result = deleteUseCase("set_1", sessionId)

        assertTrue(result.isSuccess)
        assertEquals(sessionId, sessionRepository.updatedSession?.id)
        assertTrue(sessionRepository.updatedSession!!.updatedAt > 1L)
    }

    @Test
    fun `update should persist set changes and bump session timestamp`() = runTest {
        val updateUseCase = UpdateExerciseSetUseCase(
            setRepository,
            sessionRepository,
            transactionProvider
        )
        val sessionId = "session_1"
        sessionRepository.sessions.add(
            WorkoutSession(id = sessionId, startTime = 0L, updatedAt = 1L)
        )
        val existing = ExerciseSet(
            id = "set_1",
            sessionId = sessionId,
            exerciseId = "ex_squat",
            weight = 100.0,
            reps = 5,
            updatedAt = 1L
        )

        val result = updateUseCase(existing.copy(weight = 110.0, reps = 3))

        assertTrue(result.isSuccess)
        assertEquals(sessionId, sessionRepository.updatedSession?.id)
        assertTrue(sessionRepository.updatedSession!!.updatedAt > 1L)
    }
}
