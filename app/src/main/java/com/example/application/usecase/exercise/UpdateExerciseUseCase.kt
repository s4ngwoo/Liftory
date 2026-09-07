package com.example.application.usecase.exercise

import com.example.domain.model.Exercise
import com.example.domain.repository.ExerciseRepository

class UpdateExerciseUseCase(
    private val exerciseRepository: ExerciseRepository
) {
    suspend operator fun invoke(exercise: Exercise): Result<Unit> {
        return exerciseRepository.update(exercise.copy(updatedAt = System.currentTimeMillis()))
    }
}
