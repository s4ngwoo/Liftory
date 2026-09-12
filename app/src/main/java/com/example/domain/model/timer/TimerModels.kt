package com.example.domain.model.timer

/**
 * Persistent execution interval using absolute timestamps (ADR-002, N05.1).
 * Display ticks must not mutate these values.
 */
data class ExecutionInterval(
    val startedAtEpochMs: Long,
    val startedAtMonotonicMs: Long,
    val endedAtEpochMs: Long? = null,
    val endedAtMonotonicMs: Long? = null,
    val bootId: String? = null
) {
    fun isOpen(): Boolean = endedAtEpochMs == null
}

data class PauseInterval(
    val startedAtEpochMs: Long,
    val startedAtMonotonicMs: Long,
    val endedAtEpochMs: Long? = null,
    val endedAtMonotonicMs: Long? = null
) {
    fun isOpen(): Boolean = endedAtEpochMs == null
}

/**
 * Rest target anchored at set completion (not confirmation).
 */
data class RestTarget(
    val startedAtEpochMs: Long,
    val startedAtMonotonicMs: Long,
    val targetSeconds: Int,
    val pauseIntervals: List<PauseInterval> = emptyList(),
    val bootId: String? = null,
    val closedAtEpochMs: Long? = null
)

sealed interface TimerConfidence {
    data object Trusted : TimerConfidence
    data class Estimated(
        val reason: String
    ) : TimerConfidence
}

data class TimerSnapshot(
    val performedSeconds: Int,
    val restElapsedSeconds: Int,
    val restRemainingSeconds: Int,
    val restOvertimeSeconds: Int,
    val sessionElapsedSeconds: Int,
    val pauseTotalSeconds: Int,
    val confidence: TimerConfidence = TimerConfidence.Trusted
)
