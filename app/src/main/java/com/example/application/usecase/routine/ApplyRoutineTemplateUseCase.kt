package com.example.application.usecase.routine

import com.example.domain.model.ExerciseSet
import com.example.domain.repository.ExerciseSetRepository
import com.example.domain.repository.RoutineTemplateRepository
import com.example.domain.repository.TransactionProvider
import com.example.domain.repository.WorkoutSessionRepository
import java.util.UUID

class ApplyRoutineTemplateUseCase(
    private val routineTemplateRepository: RoutineTemplateRepository,
    private val exerciseSetRepository: ExerciseSetRepository,
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val transactionProvider: TransactionProvider
) {
    suspend operator fun invoke(sessionId: String, templateId: String): Result<Unit> {
        val template = routineTemplateRepository.getById(templateId)
            ?: return Result.failure(Exception("Template not found"))
        
        val session = workoutSessionRepository.getById(sessionId)
            ?: return Result.failure(Exception("Session not found"))

        return try {
            transactionProvider.runAsTransaction {
                val now = System.currentTimeMillis()
                
                template.exercises.forEachIndexed { index, preset ->
                    val newSet = ExerciseSet(
                        id = UUID.randomUUID().toString(),
                        sessionId = sessionId,
                        exerciseId = preset.exerciseId,
                        weight = preset.defaultWeight,
                        reps = preset.defaultReps,
                        rpe = null,
                        orderIndex = index,
                        isCompleted = false,
                        targetReps = preset.defaultReps,
                        createdAt = now,
                        updatedAt = now
                    )
                    exerciseSetRepository.create(newSet)
                }
                
                // Update session updatedAt to ensure sync
                workoutSessionRepository.update(session.copy(updatedAt = now))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
