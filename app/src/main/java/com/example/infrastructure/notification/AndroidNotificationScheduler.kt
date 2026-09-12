package com.example.infrastructure.notification

import android.content.Context
import com.example.domain.port.NotificationScheduler
import com.example.infrastructure.service.WorkoutTimerService

/**
 * Android adapter for [NotificationScheduler] (N05.5).
 * Permission/service failures are returned as Result.failure and never throw (N05.6 / TIME-08).
 */
class AndroidNotificationScheduler(
    private val context: Context
) : NotificationScheduler {

    override fun startWorkoutOngoing(
        sessionId: String,
        title: String,
        startTimeEpochMs: Long
    ): Result<Unit> = runCatching {
        WorkoutTimerService.start(context, sessionId, title, startTimeEpochMs)
    }

    override fun stopWorkoutOngoing(): Result<Unit> = runCatching {
        WorkoutTimerService.stop(context)
    }

    override fun notifyRestActive(
        sessionId: String,
        remainingSeconds: Int,
        targetSeconds: Int
    ): Result<Unit> {
        // Rest progress reuses the session FGS surface for now.
        // Failure paths must not affect local rest state.
        return Result.success(Unit)
    }

    override fun clearRestNotification(): Result<Unit> = Result.success(Unit)
}
