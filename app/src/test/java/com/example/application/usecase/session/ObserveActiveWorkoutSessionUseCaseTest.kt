package com.example.application.usecase.session

import com.example.domain.model.WorkoutSession
import com.example.domain.repository.WorkoutSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObserveActiveWorkoutSessionUseCaseTest {

    private class FakeWorkoutSessionRepository(
        private val activeSessionFlow: Flow<WorkoutSession?>
    ) : WorkoutSessionRepository {
        override suspend fun create(session: WorkoutSession): Result<WorkoutSession> = Result.success(session)
        override suspend fun getById(id: String): WorkoutSession? = null
        override suspend fun update(session: WorkoutSession): Result<Unit> = Result.success(Unit)
        override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
        override fun observeAll(): Flow<List<WorkoutSession>> = flowOf(emptyList())
        override fun observeActiveSession(): Flow<WorkoutSession?> = activeSessionFlow
    }

    @Test
    fun invoke_whenActiveSessionExists_emitsActiveSession() = runTest {
        val active = WorkoutSession(
            id = "active-123",
            startTime = 1000L,
            endTime = null,
            notes = "가슴 & 삼두"
        )
        val repo = FakeWorkoutSessionRepository(flowOf(active))
        val useCase = ObserveActiveWorkoutSessionUseCase(repo)

        val result = useCase().first()
        assertEquals("active-123", result?.id)
        assertEquals("가슴 & 삼두", result?.notes)
        assertNull(result?.endTime)
    }

    @Test
    fun invoke_whenNoActiveSession_emitsNull() = runTest {
        val repo = FakeWorkoutSessionRepository(flowOf(null))
        val useCase = ObserveActiveWorkoutSessionUseCase(repo)

        val result = useCase().first()
        assertNull(result)
    }
}
