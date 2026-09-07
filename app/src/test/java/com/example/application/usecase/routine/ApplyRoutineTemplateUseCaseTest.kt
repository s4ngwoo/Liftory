package com.example.application.usecase.routine

import com.example.domain.model.ExercisePreset
import com.example.domain.model.RoutineTemplate
import com.example.domain.model.WorkoutSession
import com.example.presentation.session.FakeSetRepository
import com.example.presentation.session.FakeSessionRepository
import com.example.presentation.session.FakeTransactionProvider
import com.example.domain.repository.RoutineTemplateRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class ApplyRoutineTemplateUseCaseTest {

    private lateinit var routineTemplateRepository: FakeRoutineTemplateRepository
    private lateinit var exerciseSetRepository: FakeSetRepository
    private lateinit var workoutSessionRepository: FakeSessionRepository
    private lateinit var transactionProvider: FakeTransactionProvider
    private lateinit var applyRoutineTemplateUseCase: ApplyRoutineTemplateUseCase

    @Before
    fun setup() {
        routineTemplateRepository = FakeRoutineTemplateRepository()
        exerciseSetRepository = FakeSetRepository()
        workoutSessionRepository = FakeSessionRepository()
        transactionProvider = FakeTransactionProvider()
        
        applyRoutineTemplateUseCase = ApplyRoutineTemplateUseCase(
            routineTemplateRepository,
            exerciseSetRepository,
            workoutSessionRepository,
            transactionProvider
        )
    }

    @Test
    fun `invoke should create sets from template presets and update session`() = runTest {
        val templateId = "template_1"
        val sessionId = "session_1"
        
        val template = RoutineTemplate(
            id = templateId,
            name = "Push Day",
            exercises = listOf(
                ExercisePreset("ex_bench", 100.0, 10, 0),
                ExercisePreset("ex_ohp", 60.0, 8, 1)
            )
        )
        routineTemplateRepository.templates.add(template)
        
        val session = WorkoutSession(id = sessionId, startTime = 0L)
        workoutSessionRepository.sessions.add(session)
        
        val result = applyRoutineTemplateUseCase(sessionId, templateId)
        
        assertTrue(result.isSuccess)
        
        val createdSets = exerciseSetRepository.createdSets
        assertEquals(2, createdSets.size)
        assertEquals("ex_bench", createdSets[0].exerciseId)
        assertEquals(100.0, createdSets[0].weight, 0.0)
        assertEquals(10, createdSets[0].reps)
        assertEquals(0, createdSets[0].orderIndex)
        
        assertEquals("ex_ohp", createdSets[1].exerciseId)
        assertEquals(60.0, createdSets[1].weight, 0.0)
        
        val updatedSession = workoutSessionRepository.updatedSession
        assertEquals(sessionId, updatedSession?.id)
        assertTrue(updatedSession!!.updatedAt > 0L)
    }
}

class FakeRoutineTemplateRepository : RoutineTemplateRepository {
    val templates = mutableListOf<RoutineTemplate>()
    override fun observeAll(): Flow<List<RoutineTemplate>> = flowOf(templates)
    override suspend fun getById(id: String): RoutineTemplate? = templates.find { it.id == id }
    override suspend fun create(template: RoutineTemplate): Result<RoutineTemplate> {
        templates.add(template)
        return Result.success(template)
    }
    override suspend fun update(template: RoutineTemplate): Result<Unit> = Result.success(Unit)
    override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
}
