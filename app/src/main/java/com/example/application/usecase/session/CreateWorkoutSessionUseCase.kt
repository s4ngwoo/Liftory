package com.example.application.usecase.session

import com.example.domain.model.WorkoutSession
import com.example.domain.repository.WorkoutSessionRepository
import java.util.UUID

class CreateWorkoutSessionUseCase(
    private val sessionRepository: WorkoutSessionRepository
) {
    suspend operator fun invoke(
        notes: String = "",
        startTime: Long = System.currentTimeMillis()
    ): Result<WorkoutSession> {
        val session = WorkoutSession(
            id = UUID.randomUUID().toString(),
            startTime = startTime,
            notes = notes
        )
        return sessionRepository.create(session)
    }
}
