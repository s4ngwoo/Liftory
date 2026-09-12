package com.example.application.usecase.execution

import com.example.domain.exception.ActiveSessionAlreadyExistsException
import com.example.domain.model.WorkoutSession
import com.example.domain.model.execution.SessionExecutionState
import com.example.domain.model.execution.SetExecutionState
import com.example.domain.model.execution.WorkoutExecution
import com.example.domain.port.IdGenerator
import com.example.domain.port.MonotonicClock
import com.example.domain.port.WallClock
import com.example.domain.repository.SessionPlanRepository
import com.example.domain.repository.TransactionProvider
import com.example.domain.repository.WorkoutExecutionRepository
import com.example.domain.repository.WorkoutSessionRepository

/**
 * Starts a workout session from a confirmed plan without creating performed set rows (N04.2).
 */
class StartSessionUseCase(
    private val planRepository: SessionPlanRepository,
    private val sessionRepository: WorkoutSessionRepository,
    private val executionRepository: WorkoutExecutionRepository,
    private val transactionProvider: TransactionProvider,
    private val idGenerator: IdGenerator,
    private val wallClock: WallClock
) {
    suspend operator fun invoke(planId: String): Result<WorkoutExecution> {
        val plan = planRepository.getPlanById(planId)
            ?: return Result.failure(IllegalArgumentException("Plan not found: $planId"))
        if (!plan.isConfirmed) {
            return Result.failure(IllegalStateException("Plan must be confirmed before starting session"))
        }
        val existingActive = sessionRepository.getActiveSession()
        if (existingActive != null) {
            return Result.failure(ActiveSessionAlreadyExistsException(existingActive))
        }
        val existingExecution = executionRepository.getActiveExecution()
        if (existingExecution != null) {
            val placeholder = WorkoutSession(
                id = existingExecution.sessionId,
                startTime = existingExecution.startedAtEpochMs ?: wallClock.nowMillis()
            )
            return Result.failure(ActiveSessionAlreadyExistsException(placeholder))
        }

        return transactionProvider.runAsTransaction {
            val now = wallClock.nowMillis()
            val sessionId = idGenerator.generate()
            val session = WorkoutSession(
                id = sessionId,
                startTime = now,
                endTime = null,
                notes = "",
                createdAt = now,
                updatedAt = now
            )
            sessionRepository.create(session).getOrElse { return@runAsTransaction Result.failure(it) }

            val execution = WorkoutExecution(
                sessionId = sessionId,
                planId = plan.id,
                planSnapshot = plan,
                sessionState = SessionExecutionState.ACTIVE,
                currentExerciseIndex = 0,
                currentSetIndex = 0,
                setState = SetExecutionState.Ready,
                revision = 1L,
                startedAtEpochMs = now
            )
            executionRepository.save(execution)
        }
    }
}

class StartSetUseCase(
    private val executionRepository: WorkoutExecutionRepository,
    private val transactionProvider: TransactionProvider,
    private val wallClock: WallClock,
    private val monotonicClock: MonotonicClock
) {
    suspend operator fun invoke(
        sessionId: String,
        commandId: String,
        expectedRevision: Long
    ): Result<WorkoutExecution> = transactionProvider.runAsTransaction {
        val execution = executionRepository.getBySessionId(sessionId)
            ?: return@runAsTransaction Result.failure(IllegalArgumentException("Execution not found"))
        if (execution.isFinished) {
            return@runAsTransaction Result.failure(IllegalStateException("Session completed"))
        }

        val controller = com.example.domain.model.execution.SessionExecutionController(execution)
        val result = controller.runCommand(commandId, expectedRevision) {
            controller.setMachine().startSet(
                startEpochMs = wallClock.nowMillis(),
                startMonotonicMs = monotonicClock.elapsedRealtimeMillis()
            )
        }
        result.fold(
            onSuccess = { updated -> executionRepository.save(updated) },
            onFailure = { Result.failure(it) }
        )
    }
}

class CompleteSetUseCase(
    private val executionRepository: WorkoutExecutionRepository,
    private val transactionProvider: TransactionProvider,
    private val wallClock: WallClock
) {
    suspend operator fun invoke(
        sessionId: String,
        commandId: String,
        expectedRevision: Long,
        durationSeconds: Int
    ): Result<WorkoutExecution> = transactionProvider.runAsTransaction {
        val execution = executionRepository.getBySessionId(sessionId)
            ?: return@runAsTransaction Result.failure(IllegalArgumentException("Execution not found"))

        val controller = com.example.domain.model.execution.SessionExecutionController(execution)
        val completionEpoch = wallClock.nowMillis()
        val result = controller.runCommand(commandId, expectedRevision) {
            controller.setMachine().completeSet(
                completionEpochMs = completionEpoch,
                durationSeconds = durationSeconds
            )
        }
        result.fold(
            onSuccess = { updated ->
                // Values unconfirmed: no ConfirmedSetRecord / statistics yet (EXEC-02).
                executionRepository.save(updated)
            },
            onFailure = { Result.failure(it) }
        )
    }
}

class ConfirmPerformedValuesUseCase(
    private val executionRepository: WorkoutExecutionRepository,
    private val transactionProvider: TransactionProvider
) {
    suspend operator fun invoke(
        sessionId: String,
        commandId: String,
        expectedRevision: Long,
        measurement: com.example.domain.model.MeasurementValue,
        targetRestSeconds: Int,
        isLastSet: Boolean,
        isLastExercise: Boolean = false
    ): Result<WorkoutExecution> = transactionProvider.runAsTransaction {
        val execution = executionRepository.getBySessionId(sessionId)
            ?: return@runAsTransaction Result.failure(IllegalArgumentException("Execution not found"))

        val controller = com.example.domain.model.execution.SessionExecutionController(execution)
        val awaiting = execution.setState as? SetExecutionState.AwaitingConfirmation
            ?: return@runAsTransaction Result.failure(
                IllegalStateException("Set is not awaiting confirmation")
            )

        val result = controller.runCommand(commandId, expectedRevision) {
            controller.setMachine().confirmValues(measurement, targetRestSeconds, isLastSet)
        }
        result.fold(
            onSuccess = { updated ->
                val plan = updated.planSnapshot
                val exercise = plan?.exercises?.getOrNull(updated.currentExerciseIndex)
                controller.recordConfirmedSet(
                    com.example.domain.model.execution.ConfirmedSetRecord(
                        plannedExerciseId = exercise?.id,
                        exerciseId = exercise?.exerciseId ?: "",
                        exerciseIndex = updated.currentExerciseIndex,
                        setIndex = updated.currentSetIndex,
                        measurement = measurement,
                        performedDurationSeconds = awaiting.durationSeconds,
                        startedAtEpochMs = awaiting.startedAtEpochMs,
                        completedAtEpochMs = awaiting.completedAtEpochMs,
                        isIncludedInStatistics = true
                    )
                )
                controller.advanceAfterConfirm(isLastSet, isLastExercise)
                val withRest = if (isLastSet) {
                    controller.current.copy(activeRestTarget = null)
                } else {
                    val resting = controller.setMachine().currentState as? SetExecutionState.Resting
                    controller.current.copy(
                        activeRestTarget = resting?.let {
                            com.example.domain.model.timer.RestTarget(
                                startedAtEpochMs = it.restStartedAtEpochMs,
                                startedAtMonotonicMs = it.restStartedAtEpochMs,
                                targetSeconds = it.targetRestSeconds,
                                bootId = controller.current.bootId
                            )
                        }
                    )
                }
                executionRepository.save(withRest)
            },
            onFailure = { Result.failure(it) }
        )
    }
}

class StartNextSetUseCase(
    private val executionRepository: WorkoutExecutionRepository,
    private val transactionProvider: TransactionProvider,
    private val wallClock: WallClock,
    private val monotonicClock: MonotonicClock
) {
    suspend operator fun invoke(
        sessionId: String,
        commandId: String,
        expectedRevision: Long
    ): Result<WorkoutExecution> = transactionProvider.runAsTransaction {
        val execution = executionRepository.getBySessionId(sessionId)
            ?: return@runAsTransaction Result.failure(IllegalArgumentException("Execution not found"))

        val controller = com.example.domain.model.execution.SessionExecutionController(execution)
        val result = controller.runCommand(commandId, expectedRevision) {
            controller.setMachine().startNextSet(
                nextStartEpochMs = wallClock.nowMillis(),
                nextStartMonotonicMs = monotonicClock.elapsedRealtimeMillis()
            )
        }
        result.fold(
            onSuccess = { updated ->
                // Closing rest atomically with next set start (EXEC-06 / N05).
                executionRepository.save(updated.copy(activeRestTarget = null))
            },
            onFailure = { Result.failure(it) }
        )
    }
}

class FinishSessionUseCase(
    private val executionRepository: WorkoutExecutionRepository,
    private val sessionRepository: WorkoutSessionRepository,
    private val transactionProvider: TransactionProvider,
    private val wallClock: WallClock
) {
    suspend operator fun invoke(
        sessionId: String,
        commandId: String,
        expectedRevision: Long
    ): Result<WorkoutExecution> = transactionProvider.runAsTransaction {
        val execution = executionRepository.getBySessionId(sessionId)
            ?: return@runAsTransaction Result.failure(IllegalArgumentException("Execution not found"))

        val controller = com.example.domain.model.execution.SessionExecutionController(execution)
        val now = wallClock.nowMillis()
        val result = controller.runCommand(commandId, expectedRevision) {
            controller.finishSession(now).isSuccess
        }
        result.fold(
            onSuccess = { updated ->
                val session = sessionRepository.getById(sessionId)
                if (session != null) {
                    sessionRepository.update(
                        session.copy(endTime = updated.completedAtEpochMs ?: now, updatedAt = now)
                    )
                }
                executionRepository.save(updated)
            },
            onFailure = { Result.failure(it) }
        )
    }
}

class UndoSetCompletionUseCase(
    private val executionRepository: WorkoutExecutionRepository,
    private val transactionProvider: TransactionProvider
) {
    suspend operator fun invoke(
        sessionId: String,
        commandId: String,
        expectedRevision: Long
    ): Result<WorkoutExecution> = transactionProvider.runAsTransaction {
        val execution = executionRepository.getBySessionId(sessionId)
            ?: return@runAsTransaction Result.failure(IllegalArgumentException("Execution not found"))

        val controller = com.example.domain.model.execution.SessionExecutionController(execution)
        val result = controller.runCommand(commandId, expectedRevision) {
            controller.setMachine().undo()
        }
        result.fold(
            onSuccess = { updated ->
                // Drop last confirmed record for this set position if present.
                val trimmed = updated.copy(
                    confirmedRecords = updated.confirmedRecords.dropLastWhile {
                        it.exerciseIndex == updated.currentExerciseIndex &&
                            it.setIndex == updated.currentSetIndex
                    }
                )
                executionRepository.save(trimmed)
            },
            onFailure = { Result.failure(it) }
        )
    }
}
