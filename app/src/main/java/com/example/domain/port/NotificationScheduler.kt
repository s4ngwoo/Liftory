package com.example.domain.port

/**
 * Port for workout / rest notifications (N05.5–N05.6).
 * Implementations must never throw into Application/Domain; failures return [Result].
 */
interface NotificationScheduler {
    fun startWorkoutOngoing(
        sessionId: String,
        title: String,
        startTimeEpochMs: Long
    ): Result<Unit>

    fun stopWorkoutOngoing(): Result<Unit>

    fun notifyRestActive(
        sessionId: String,
        remainingSeconds: Int,
        targetSeconds: Int
    ): Result<Unit>

    fun clearRestNotification(): Result<Unit>
}

/**
 * No-op scheduler for JVM tests and environments without notification permission.
 */
object NoOpNotificationScheduler : NotificationScheduler {
    override fun startWorkoutOngoing(
        sessionId: String,
        title: String,
        startTimeEpochMs: Long
    ): Result<Unit> = Result.success(Unit)

    override fun stopWorkoutOngoing(): Result<Unit> = Result.success(Unit)

    override fun notifyRestActive(
        sessionId: String,
        remainingSeconds: Int,
        targetSeconds: Int
    ): Result<Unit> = Result.success(Unit)

    override fun clearRestNotification(): Result<Unit> = Result.success(Unit)
}
