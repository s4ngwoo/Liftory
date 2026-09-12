package com.example.application.usecase.execution

import com.example.domain.model.execution.WorkoutExecution
import com.example.domain.model.timer.TimerCalculator
import com.example.domain.model.timer.TimerSnapshot
import com.example.domain.port.WallClock
import com.example.domain.repository.WorkoutExecutionRepository

/**
 * Rebuilds a display snapshot from persisted intervals without mutating DB (N05.2, TIME-02/03).
 * UI ticks should call this (or equivalent pure calc) rather than decrementing stored seconds.
 */
class RestoreTimerSnapshotUseCase(
    private val executionRepository: WorkoutExecutionRepository,
    private val wallClock: WallClock
) {
    suspend operator fun invoke(
        sessionId: String,
        currentBootId: String?,
        nowEpochMs: Long = wallClock.nowMillis()
    ): Result<TimerSnapshot> {
        val execution = executionRepository.getBySessionId(sessionId)
            ?: return Result.failure(IllegalArgumentException("Execution not found: $sessionId"))
        return Result.success(snapshotOf(execution, currentBootId, nowEpochMs))
    }

    companion object {
        fun snapshotOf(
            execution: WorkoutExecution,
            currentBootId: String?,
            nowEpochMs: Long
        ): TimerSnapshot {
            val last = execution.confirmedRecords.lastOrNull()
            val bootMatches = execution.bootId == null ||
                currentBootId == null ||
                execution.bootId == currentBootId
            return TimerCalculator.snapshotAfterMissedTicks(
                performedStartEpochMs = last?.startedAtEpochMs,
                performedEndEpochMs = last?.completedAtEpochMs,
                restTarget = execution.activeRestTarget,
                sessionStartEpochMs = execution.startedAtEpochMs ?: nowEpochMs,
                sessionPauses = execution.sessionPauseIntervals,
                nowEpochMs = nowEpochMs,
                bootIdMatches = bootMatches
            )
        }
    }
}
