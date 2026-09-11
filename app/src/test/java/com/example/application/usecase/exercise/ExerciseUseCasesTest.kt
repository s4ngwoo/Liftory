package com.example.application.usecase.exercise

import com.example.domain.model.Exercise
import com.example.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeExerciseRepository : ExerciseRepository {
    val exercises = mutableListOf<Exercise>()

    override fun observeAll(): Flow<List<Exercise>> = flowOf(exercises)

    override suspend fun getById(id: String): Result<Exercise> {
        val found = exercises.find { it.id == id }
        return if (found != null) Result.success(found) else Result.failure(NoSuchElementException("Not found"))
    }

    override fun search(query: String): Flow<List<Exercise>> {
        val filtered = exercises.filter { it.name.contains(query, ignoreCase = true) }
        return flowOf(filtered)
    }

    override fun getExercisesByCategory(category: String): Flow<List<Exercise>> {
        val filtered = exercises.filter { it.muscleGroup.equals(category, ignoreCase = true) }
        return flowOf(filtered)
    }

    override suspend fun create(exercise: Exercise): Result<Exercise> {
        exercises.add(exercise)
        return Result.success(exercise)
    }

    override suspend fun update(exercise: Exercise): Result<Unit> {
        val index = exercises.indexOfFirst { it.id == exercise.id }
        if (index != -1) {
            exercises[index] = exercise
        }
        return Result.success(Unit)
    }

    override suspend fun delete(id: String): Result<Unit> {
        exercises.removeAll { it.id == id }
        return Result.success(Unit)
    }
}

class ExerciseUseCasesTest {

    private lateinit var repository: FakeExerciseRepository
    private lateinit var createUseCase: CreateExerciseUseCase
    private lateinit var observeUseCase: ObserveExercisesUseCase
    private lateinit var searchUseCase: SearchExercisesUseCase

    @Before
    fun setUp() {
        repository = FakeExerciseRepository()
        createUseCase = CreateExerciseUseCase(repository)
        observeUseCase = ObserveExercisesUseCase(repository)
        searchUseCase = SearchExercisesUseCase(repository)
    }

    @Test
    fun `createExerciseUseCase should create custom exercise with uuid and muscle group`() = runTest {
        val result = createUseCase(name = "Deadlift", muscleGroup = "Back")
        assertTrue(result.isSuccess)

        val created = result.getOrNull()!!
        assertEquals("Deadlift", created.name)
        assertEquals("Back", created.muscleGroup)
        assertTrue(created.isCustom)
        assertTrue(created.id.isNotBlank())
    }

    @Test
    fun `searchExercisesUseCase should return matched exercises by query`() = runTest {
        createUseCase(name = "Bench Press", muscleGroup = "Chest")
        createUseCase(name = "Incline Dumbbell Press", muscleGroup = "Chest")
        createUseCase(name = "Barbell Squat", muscleGroup = "Legs")

        val searchResult = searchUseCase("Press").first()
        assertEquals(2, searchResult.size)
        assertTrue(searchResult.any { it.name == "Bench Press" })
        assertTrue(searchResult.any { it.name == "Incline Dumbbell Press" })
    }
}
