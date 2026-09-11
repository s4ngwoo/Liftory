package com.example.application.usecase.exercise

import com.example.domain.model.Exercise
import com.example.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CreateAndDeleteExerciseUseCaseTest {

    private lateinit var repository: FakeExerciseRepository
    private lateinit var createExerciseUseCase: CreateExerciseUseCase
    private lateinit var deleteExerciseUseCase: DeleteExerciseUseCase
    private lateinit var updateExerciseUseCase: UpdateExerciseUseCase

    @Before
    fun setup() {
        repository = FakeExerciseRepository()
        createExerciseUseCase = CreateExerciseUseCase(repository)
        deleteExerciseUseCase = DeleteExerciseUseCase(repository)
        updateExerciseUseCase = UpdateExerciseUseCase(repository)
    }

    @Test
    fun `create should persist a custom exercise`() = runTest {
        val result = createExerciseUseCase("Bulgarian Split Squat", "Legs")

        assertTrue(result.isSuccess)
        val created = result.getOrThrow()
        assertEquals("Bulgarian Split Squat", created.name)
        assertEquals("Legs", created.muscleGroup)
        assertTrue(created.isCustom)
        assertEquals(created, repository.created)
    }

    @Test
    fun `delete should return repository failure`() = runTest {
        repository.deleteError = IllegalStateException("in use")

        val result = deleteExerciseUseCase("ex_1")

        assertTrue(result.isFailure)
        assertEquals("in use", result.exceptionOrNull()?.message)
        assertEquals("ex_1", repository.deletedId)
    }

    @Test
    fun `update should stamp updatedAt before saving`() = runTest {
        val original = Exercise(
            id = "ex_1",
            name = "Squat",
            isCustom = true,
            muscleGroup = "Legs",
            createdAt = 10L,
            updatedAt = 10L
        )

        val result = updateExerciseUseCase(original)

        assertTrue(result.isSuccess)
        assertEquals("ex_1", repository.updated?.id)
        assertTrue(repository.updated!!.updatedAt > 10L)
        assertEquals("Squat", repository.updated?.name)
    }
}

class FakeExerciseRepository : ExerciseRepository {
    var created: Exercise? = null
    var updated: Exercise? = null
    var deletedId: String? = null
    var deleteError: Throwable? = null

    override fun observeAll(): Flow<List<Exercise>> = flowOf(emptyList())
    override suspend fun getById(id: String): Result<Exercise> =
        Result.failure(IllegalStateException("unused"))
    override fun search(query: String): Flow<List<Exercise>> = flowOf(emptyList())
    override fun getExercisesByCategory(category: String): Flow<List<Exercise>> = flowOf(emptyList())

    override suspend fun create(exercise: Exercise): Result<Exercise> {
        created = exercise
        return Result.success(exercise)
    }

    override suspend fun update(exercise: Exercise): Result<Unit> {
        updated = exercise
        return Result.success(Unit)
    }

    override suspend fun delete(id: String): Result<Unit> {
        deletedId = id
        return deleteError?.let { Result.failure(it) } ?: Result.success(Unit)
    }
}
