package com.example.application.usecase.session

import com.example.domain.exception.ActiveSessionAlreadyExistsException
import com.example.domain.model.WorkoutSession
import com.example.domain.repository.WorkoutSessionRepository
import java.util.UUID

class CreateWorkoutSessionUseCase(
    private val sessionRepository: WorkoutSessionRepository
) {
    suspend operator fun invoke(
        notes: String = "",
        startTime: Long = System.currentTimeMillis(),
        finishExistingActive: Boolean = false
    ): Result<WorkoutSession> {
        val activeSession = sessionRepository.getActiveSession()
        if (activeSession != null) {
            if (finishExistingActive) {
                val existingActiveList = sessionRepository.getActiveSessions().ifEmpty { listOf(activeSession) }
                for (existing in existingActiveList) {
                    sessionRepository.update(
                        existing.copy(
                            endTime = startTime,
                            updatedAt = startTime
                        )
                    )
                }
            } else {
                return Result.failure(ActiveSessionAlreadyExistsException(activeSession))
            }
        }

        val session = WorkoutSession(
            id = UUID.randomUUID().toString(),
            startTime = startTime,
            notes = notes
        )
        return sessionRepository.create(session)
    }
}

