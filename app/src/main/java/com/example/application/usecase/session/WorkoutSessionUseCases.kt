package com.example.application.usecase.session

import com.example.domain.model.WorkoutSession
import com.example.domain.repository.WorkoutSessionRepository
import kotlinx.coroutines.flow.Flow

class GetWorkoutSessionUseCase(
    private val sessionRepository: WorkoutSessionRepository
) {
    suspend open operator fun invoke(id: String): WorkoutSession? {
        return sessionRepository.getById(id)
    }
}

class UpdateWorkoutSessionUseCase(
    private val sessionRepository: WorkoutSessionRepository
) {
    suspend open operator fun invoke(session: WorkoutSession): Result<Unit> {
        return sessionRepository.update(session.copy(updatedAt = System.currentTimeMillis()))
    }
}

class DeleteWorkoutSessionUseCase(
    private val sessionRepository: WorkoutSessionRepository
) {
    suspend open operator fun invoke(id: String): Result<Unit> {
        return sessionRepository.delete(id)
    }
}

open class ObserveWorkoutSessionsUseCase(
    private val sessionRepository: WorkoutSessionRepository
) {
    open operator fun invoke(): Flow<List<WorkoutSession>> {
        return sessionRepository.observeAll()
    }
}
