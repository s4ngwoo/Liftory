package com.example.application.usecase.set

import com.example.domain.model.ExerciseSet
import com.example.domain.repository.ExerciseSetRepository
import com.example.domain.repository.TransactionProvider
import com.example.domain.repository.WorkoutSessionRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class AddExerciseSetUseCase(
    private val setRepository: ExerciseSetRepository,
    private val sessionRepository: WorkoutSessionRepository,
    private val transactionProvider: TransactionProvider
) {
    suspend open operator fun invoke(
        sessionId: String,
        exerciseId: String,
        weight: Double,
        reps: Int,
        rpe: Double? = null,
        restSeconds: Int? = 90,
        orderIndex: Int = 0
    ): Result<ExerciseSet> = transactionProvider.runAsTransaction {
        val set = ExerciseSet(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            exerciseId = exerciseId,
            weight = weight,
            reps = reps,
            rpe = rpe,
            restSeconds = restSeconds,
            orderIndex = orderIndex
        )
        val result = setRepository.create(set)
        if (result.isSuccess) {
            sessionRepository.getById(sessionId)?.let { session ->
                sessionRepository.update(session.copy(updatedAt = System.currentTimeMillis()))
            }
        }
        result
    }
}

class UpdateExerciseSetUseCase(
    private val setRepository: ExerciseSetRepository,
    private val sessionRepository: WorkoutSessionRepository,
    private val transactionProvider: TransactionProvider
) {
    suspend open operator fun invoke(set: ExerciseSet): Result<Unit> = transactionProvider.runAsTransaction {
        val result = setRepository.update(set.copy(updatedAt = System.currentTimeMillis()))
        if (result.isSuccess) {
            sessionRepository.getById(set.sessionId)?.let { session ->
                sessionRepository.update(session.copy(updatedAt = System.currentTimeMillis()))
            }
        }
        result
    }
}

class DeleteExerciseSetUseCase(
    private val setRepository: ExerciseSetRepository,
    private val sessionRepository: WorkoutSessionRepository,
    private val transactionProvider: TransactionProvider
) {
    suspend open operator fun invoke(setId: String, sessionId: String): Result<Unit> = transactionProvider.runAsTransaction {
        val result = setRepository.delete(setId)
        if (result.isSuccess) {
            sessionRepository.getById(sessionId)?.let { session ->
                sessionRepository.update(session.copy(updatedAt = System.currentTimeMillis()))
            }
        }
        result
    }
}

open class ObserveExerciseSetsUseCase(
    private val setRepository: ExerciseSetRepository
) {
    open operator fun invoke(sessionId: String): Flow<List<ExerciseSet>> {
        return setRepository.observeBySession(sessionId)
    }
}
