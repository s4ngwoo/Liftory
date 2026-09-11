package com.example.application.usecase.session

import com.example.domain.model.WorkoutSession
import com.example.domain.repository.WorkoutSessionRepository
import kotlinx.coroutines.flow.Flow

open class ObserveActiveWorkoutSessionUseCase(
    private val sessionRepository: WorkoutSessionRepository
) {
    open operator fun invoke(): Flow<WorkoutSession?> {
        return sessionRepository.observeActiveSession()
    }
}
