package com.example.application.usecase.set

import com.example.domain.model.ExerciseHistoryRecord
import com.example.domain.repository.ExerciseSetRepository

class GetLastExerciseHistoryUseCase(
    private val setRepository: ExerciseSetRepository
) {
    suspend operator fun invoke(
        exerciseId: String,
        currentSessionId: String? = null
    ): ExerciseHistoryRecord? {
        return setRepository.getLastHistoryForExercise(exerciseId, currentSessionId)
    }
}
