package com.example.presentation.routine

import com.example.application.usecase.routine.CreateRoutineTemplateUseCase
import com.example.application.usecase.routine.ObserveRoutineTemplatesUseCase
import com.example.domain.model.RoutineTemplate
import com.example.application.usecase.routine.FakeRoutineTemplateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class RoutineTemplateViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeRoutineTemplateRepository
    private lateinit var viewModel: RoutineTemplateViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeRoutineTemplateRepository()
        
        val observeUseCase = ObserveRoutineTemplatesUseCase(repository)
        val createUseCase = CreateRoutineTemplateUseCase(repository)
        
        viewModel = RoutineTemplateViewModel(observeUseCase, createUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `createEmptyTemplate should add template to repository`() = runTest {
        viewModel.createEmptyTemplate("Pull Day")
        
        advanceUntilIdle() // Wait for coroutines to complete
        
        val templates = repository.templates
        assertEquals(1, templates.size)
        assertEquals("Pull Day", templates[0].name)
    }

    @Test
    fun `applyTemplate should create workout session and callback with sessionId`() = runTest {
        val templateId = "tpl_123"
        repository.create(RoutineTemplate(id = templateId, name = "Leg Day", exercises = emptyList()))
        
        var createdSessionId: String? = null
        val fakeCreateSessionUseCase = com.example.application.usecase.session.CreateWorkoutSessionUseCase(
            sessionRepository = object : com.example.domain.repository.WorkoutSessionRepository {
                override suspend fun create(session: com.example.domain.model.WorkoutSession): Result<com.example.domain.model.WorkoutSession> = Result.success(session)
                override suspend fun update(session: com.example.domain.model.WorkoutSession): Result<Unit> = Result.success(Unit)
                override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
                override suspend fun getById(id: String): com.example.domain.model.WorkoutSession? = null
                override fun observeAll(): kotlinx.coroutines.flow.Flow<List<com.example.domain.model.WorkoutSession>> = kotlinx.coroutines.flow.flowOf(emptyList())
            }
        )

        val vm = RoutineTemplateViewModel(
            observeRoutineTemplatesUseCase = ObserveRoutineTemplatesUseCase(repository),
            createRoutineTemplateUseCase = CreateRoutineTemplateUseCase(repository),
            createWorkoutSessionUseCase = fakeCreateSessionUseCase,
            applyRoutineTemplateUseCase = null
        )

        advanceUntilIdle()

        vm.applyTemplate(templateId) { newId ->
            createdSessionId = newId
        }

        advanceUntilIdle()
        org.junit.Assert.assertNotNull(createdSessionId)
    }

    @Test
    fun `updateTemplate should update template in repository`() = runTest {
        val templateId = "tpl_edit"
        repository.create(RoutineTemplate(id = templateId, name = "Old Name", exercises = emptyList()))
        
        val updateUseCase = com.example.application.usecase.routine.UpdateRoutineTemplateUseCase(repository)
        val vm = RoutineTemplateViewModel(
            observeRoutineTemplatesUseCase = ObserveRoutineTemplatesUseCase(repository),
            createRoutineTemplateUseCase = CreateRoutineTemplateUseCase(repository),
            updateRoutineTemplateUseCase = updateUseCase
        )
        advanceUntilIdle()

        var successCalled = false
        val newPresets = listOf(
            com.example.domain.model.ExercisePreset(
                exerciseId = "ex_1",
                defaultWeight = 80.0,
                defaultReps = 5,
                orderIndex = 0
            )
        )
        vm.updateTemplate(templateId, "Updated Routine", newPresets) {
            successCalled = true
        }
        advanceUntilIdle()

        org.junit.Assert.assertTrue(successCalled)
        val updated = repository.getById(templateId)
        org.junit.Assert.assertNotNull(updated)
        assertEquals("Updated Routine", updated?.name)
        assertEquals(1, updated?.exercises?.size)
        assertEquals(80.0, updated?.exercises?.first()?.defaultWeight)
    }

    @Test
    fun `routineLastWorkoutMap should map templateId to last session startTime`() = runTest {
        val templateId = "tpl_leg"
        repository.create(RoutineTemplate(id = templateId, name = "Leg Day", exercises = emptyList()))

        val pastSession = com.example.domain.model.WorkoutSession(
            id = "sess_1",
            startTime = 1700000000000L,
            notes = "Leg Day Workout"
        )
        val fakeObserveSessionsUseCase = com.example.application.usecase.session.ObserveWorkoutSessionsUseCase(
            sessionRepository = object : com.example.domain.repository.WorkoutSessionRepository {
                override suspend fun create(session: com.example.domain.model.WorkoutSession) = Result.success(session)
                override suspend fun update(session: com.example.domain.model.WorkoutSession) = Result.success(Unit)
                override suspend fun delete(id: String) = Result.success(Unit)
                override suspend fun getById(id: String) = null
                override fun observeAll() = kotlinx.coroutines.flow.flowOf(listOf(pastSession))
            }
        )

        val vm = RoutineTemplateViewModel(
            observeRoutineTemplatesUseCase = ObserveRoutineTemplatesUseCase(repository),
            createRoutineTemplateUseCase = CreateRoutineTemplateUseCase(repository),
            observeWorkoutSessionsUseCase = fakeObserveSessionsUseCase
        )
        advanceUntilIdle()

        val lastWorkoutMap = vm.routineLastWorkoutMap.value
        assertEquals(1700000000000L, lastWorkoutMap[templateId])
    }
}
