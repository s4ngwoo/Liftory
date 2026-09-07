package com.example.domain

import com.example.application.usecase.session.CreateWorkoutSessionUseCase
import com.example.application.usecase.session.DeleteWorkoutSessionUseCase
import com.example.application.usecase.session.GetWorkoutSessionUseCase
import com.example.domain.model.WorkoutSession
import com.example.domain.repository.WorkoutSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeWorkoutSessionRepository : WorkoutSessionRepository {
    private val sessions = mutableMapOf<String, WorkoutSession>()
    private val _flow = MutableStateFlow<List<WorkoutSession>>(emptyList())

    override suspend fun create(session: WorkoutSession): Result<WorkoutSession> {
        sessions[session.id] = session
        _flow.value = sessions.values.toList()
        return Result.success(session)
    }

    override suspend fun getById(id: String): WorkoutSession? = sessions[id]

    override suspend fun update(session: WorkoutSession): Result<Unit> {
        sessions[session.id] = session
        _flow.value = sessions.values.toList()
        return Result.success(Unit)
    }

    override suspend fun delete(id: String): Result<Unit> {
        sessions.remove(id)
        _flow.value = sessions.values.toList()
        return Result.success(Unit)
    }

    override fun observeAll(): Flow<List<WorkoutSession>> = _flow
}

class WorkoutSessionUseCaseTest {

    private lateinit var fakeRepository: FakeWorkoutSessionRepository
    private lateinit var createUseCase: CreateWorkoutSessionUseCase
    private lateinit var getUseCase: GetWorkoutSessionUseCase
    private lateinit var deleteUseCase: DeleteWorkoutSessionUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeWorkoutSessionRepository()
        createUseCase = CreateWorkoutSessionUseCase(fakeRepository)
        getUseCase = GetWorkoutSessionUseCase(fakeRepository)
        deleteUseCase = DeleteWorkoutSessionUseCase(fakeRepository)
    }

    @Test
    fun createSession_persistsSessionAndReturnsSuccess() = runTest {
        val result = createUseCase(notes = "Chest Day")
        assertTrue(result.isSuccess)

        val created = result.getOrNull()
        assertNotNull(created)
        assertEquals("Chest Day", created?.notes)

        val fetched = getUseCase(created!!.id)
        assertNotNull(fetched)
        assertEquals(created.id, fetched?.id)
    }

    @Test
    fun deleteSession_removesSession() = runTest {
        val session = createUseCase(notes = "Leg Day").getOrThrow()
        assertNotNull(getUseCase(session.id))

        val deleteResult = deleteUseCase(session.id)
        assertTrue(deleteResult.isSuccess)
        assertNull(getUseCase(session.id))
    }
}
