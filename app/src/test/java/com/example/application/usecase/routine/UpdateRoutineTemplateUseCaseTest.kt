package com.example.application.usecase.routine

import com.example.domain.model.ExercisePreset
import com.example.domain.model.RoutineTemplate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateRoutineTemplateUseCaseTest {

    private lateinit var repository: FakeRoutineTemplateRepository
    private lateinit var updateRoutineTemplateUseCase: UpdateRoutineTemplateUseCase

    @Before
    fun setUp() {
        repository = FakeRoutineTemplateRepository()
        updateRoutineTemplateUseCase = UpdateRoutineTemplateUseCase(repository)
    }

    @Test
    fun `update should modify template name and exercise presets successfully`() = runTest {
        val original = RoutineTemplate(
            id = "template_1",
            name = "Old Routine",
            exercises = listOf(ExercisePreset("ex_bench", 60.0, 10, 0))
        )
        repository.templates.add(original)

        val newPresets = listOf(
            ExercisePreset("ex_bench", 80.0, 5, 0),
            ExercisePreset("ex_incline", 30.0, 8, 1)
        )

        val result = updateRoutineTemplateUseCase(
            templateId = "template_1",
            name = "New Heavy Chest Day",
            exercises = newPresets
        )

        assertTrue(result.isSuccess)
        val updated = repository.templates.find { it.id == "template_1" }!!
        assertEquals("New Heavy Chest Day", updated.name)
        assertEquals(2, updated.exercises.size)
        assertEquals(80.0, updated.exercises[0].defaultWeight, 0.0)
        assertEquals("ex_incline", updated.exercises[1].exerciseId)
    }

    @Test
    fun `update returns failure when template is not found`() = runTest {
        val result = updateRoutineTemplateUseCase(
            templateId = "non_existent",
            name = "Non Existent",
            exercises = emptyList()
        )

        assertTrue(result.isFailure)
    }
}
