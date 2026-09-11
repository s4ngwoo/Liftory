package com.example.application.usecase.routine

import com.example.domain.model.ExercisePreset
import com.example.domain.model.RoutineTemplate
import com.example.domain.repository.RoutineTemplateRepository

class UpdateRoutineTemplateUseCase(
    private val repository: RoutineTemplateRepository
) {
    suspend operator fun invoke(
        templateId: String,
        name: String,
        exercises: List<ExercisePreset>
    ): Result<Unit> {
        val existing = repository.getById(templateId)
            ?: return Result.failure(NoSuchElementException("Routine template not found with id: $templateId"))

        val updated = existing.copy(
            name = name,
            exercises = exercises,
            updatedAt = System.currentTimeMillis()
        )
        return repository.update(updated)
    }
}
