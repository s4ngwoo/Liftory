package com.example.application.usecase.exercise

import com.example.domain.repository.ExerciseRepository

class DeleteExerciseUseCase(
    private val exerciseRepository: ExerciseRepository
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        return exerciseRepository.delete(id)
    }
}
