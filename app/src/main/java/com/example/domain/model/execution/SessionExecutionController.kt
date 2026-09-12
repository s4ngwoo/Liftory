package com.example.domain.model.execution

import com.example.domain.model.MeasurementValue

/**
 * Session-scoped execution controller composing [ExecutionStateMachine] with
 * exercise/set indexing and finished-session immutability (EXEC-07, EXEC-09, EXEC-10).
 */
class SessionExecutionController(
    private var execution: WorkoutExecution,
    private val setMachine: ExecutionStateMachine = ExecutionStateMachine(execution.setState)
) {
    private val commandRunner = ExecutionCommandRunner(
        initialRevision = execution.revision,
        initiallyExecutedCommandIds = execution.executedCommandIds
    )

    val current: WorkoutExecution
        get() = execution.copy(
            setState = setMachine.currentState,
            revision = commandRunner.currentRevision,
            executedCommandIds = commandRunner.executedIds
        )

    fun peekExercise(targetExerciseIndex: Int): WorkoutExecution {
        // EXEC-09: viewing another exercise does not change execution target.
        require(targetExerciseIndex >= 0)
        return current
    }

    fun switchExercise(
        targetExerciseIndex: Int,
        disposition: InProgressSetDisposition,
        planChange: PlanChange? = null
    ): Result<WorkoutExecution> {
        if (execution.isFinished) {
            return Result.failure(IllegalStateException("Session is completed; cannot switch exercise"))
        }
        return when (disposition) {
            InProgressSetDisposition.PEEK_ONLY -> Result.success(current)
            InProgressSetDisposition.REQUIRE_EXPLICIT_CANCEL -> {
                if (setMachine.currentState is SetExecutionState.Performing ||
                    setMachine.currentState is SetExecutionState.AwaitingConfirmation ||
                    setMachine.currentState is SetExecutionState.Resting
                ) {
                    Result.failure(
                        IllegalStateException(
                            "In-progress set must be cancelled or completed before switching"
                        )
                    )
                } else {
                    moveToExercise(targetExerciseIndex, planChange)
                }
            }
            InProgressSetDisposition.SKIP_REMAINING -> {
                setMachine.resetToReady()
                moveToExercise(targetExerciseIndex, planChange)
            }
        }
    }

    private fun moveToExercise(
        targetExerciseIndex: Int,
        planChange: PlanChange?
    ): Result<WorkoutExecution> {
        val changes = if (planChange != null) execution.planChanges + planChange else execution.planChanges
        execution = execution.copy(
            currentExerciseIndex = targetExerciseIndex,
            currentSetIndex = 0,
            setState = SetExecutionState.Ready,
            planChanges = changes,
            sessionState = SessionExecutionState.ACTIVE
        )
        setMachine.resetToReady()
        return Result.success(current)
    }

    fun applyLateTimerEvent(): Result<WorkoutExecution> {
        // EXEC-10: late timer events after completion must not resume execution.
        if (execution.isFinished) {
            return Result.success(current)
        }
        return Result.success(current)
    }

    fun amendCompletedRecord(
        exerciseIndex: Int,
        setIndex: Int,
        newMeasurement: MeasurementValue
    ): Result<WorkoutExecution> {
        if (!execution.isFinished) {
            return Result.failure(IllegalStateException("Amend completed record only after session finish"))
        }
        val updated = execution.confirmedRecords.map { record ->
            if (record.exerciseIndex == exerciseIndex && record.setIndex == setIndex) {
                record.copy(measurement = newMeasurement)
            } else {
                record
            }
        }
        // Keep completedAtEpochMs fixed; do not reopen timers.
        execution = execution.copy(confirmedRecords = updated)
        return Result.success(current)
    }

    fun finishSession(completedAtEpochMs: Long): Result<WorkoutExecution> {
        if (execution.isFinished) {
            return Result.success(current)
        }
        val setState = setMachine.currentState
        if (setState is SetExecutionState.Performing) {
            return Result.failure(
                IllegalStateException("Cannot finish session while a set is performing")
            )
        }
        execution = execution.copy(
            sessionState = SessionExecutionState.COMPLETED,
            completedAtEpochMs = completedAtEpochMs,
            setState = setMachine.currentState
        )
        return Result.success(current)
    }

    fun recordConfirmedSet(record: ConfirmedSetRecord) {
        execution = execution.copy(confirmedRecords = execution.confirmedRecords + record)
    }

    fun advanceAfterConfirm(isLastSetOfExercise: Boolean, isLastExercise: Boolean) {
        when {
            isLastSetOfExercise && isLastExercise -> {
                // Stay in Completed set state; session finish is explicit.
            }
            isLastSetOfExercise -> {
                execution = execution.copy(
                    currentExerciseIndex = execution.currentExerciseIndex + 1,
                    currentSetIndex = 0
                )
                setMachine.resetToReady()
            }
            else -> {
                execution = execution.copy(currentSetIndex = execution.currentSetIndex + 1)
            }
        }
        execution = execution.copy(setState = setMachine.currentState)
    }

    fun runCommand(
        commandId: String,
        expectedRevision: Long,
        action: () -> Boolean
    ): Result<WorkoutExecution> {
        if (execution.isFinished) {
            return Result.failure(IllegalStateException("Session completed; execution commands rejected"))
        }
        val result = commandRunner.runCommand(commandId, expectedRevision, action)
        return result.map { current }
    }

    fun setMachine(): ExecutionStateMachine = setMachine
}
