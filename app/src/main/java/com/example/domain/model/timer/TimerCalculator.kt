package com.example.domain.model.timer

/**
 * Pure timer calculations from persisted intervals (TIME-01~07, ADR-002).
 * Never advances state on its own; callers supply "now".
 */
object TimerCalculator {

    fun performedSeconds(
        startedAtEpochMs: Long,
        completedAtEpochMs: Long
    ): Int {
        return ((completedAtEpochMs - startedAtEpochMs).coerceAtLeast(0L) / 1000L).toInt()
    }

    fun pauseTotalMillis(
        pauses: List<PauseInterval>,
        nowEpochMs: Long
    ): Long {
        return pauses.sumOf { pause ->
            val end = pause.endedAtEpochMs ?: nowEpochMs
            (end - pause.startedAtEpochMs).coerceAtLeast(0L)
        }
    }

    /**
     * Rest remaining based on completion-anchored [RestTarget].
     * Overtime is reported separately and never auto-starts the next set.
     */
    fun restRemainingSeconds(
        target: RestTarget,
        nowEpochMs: Long
    ): Int {
        val elapsed = restElapsedSeconds(target, nowEpochMs)
        return (target.targetSeconds - elapsed).coerceAtLeast(0)
    }

    fun restElapsedSeconds(
        target: RestTarget,
        nowEpochMs: Long
    ): Int {
        val end = target.closedAtEpochMs ?: nowEpochMs
        val raw = (end - target.startedAtEpochMs).coerceAtLeast(0L)
        val paused = pauseTotalMillis(target.pauseIntervals, end)
        return ((raw - paused).coerceAtLeast(0L) / 1000L).toInt()
    }

    fun restOvertimeSeconds(
        target: RestTarget,
        nowEpochMs: Long
    ): Int {
        val elapsed = restElapsedSeconds(target, nowEpochMs)
        return (elapsed - target.targetSeconds).coerceAtLeast(0)
    }

    fun extendRestTarget(target: RestTarget, extraSeconds: Int): RestTarget {
        require(extraSeconds >= 0)
        return target.copy(targetSeconds = target.targetSeconds + extraSeconds)
    }

    fun beginRestPause(
        target: RestTarget,
        pauseStartedAtEpochMs: Long,
        pauseStartedAtMonotonicMs: Long
    ): RestTarget {
        if (target.pauseIntervals.any { it.isOpen() }) return target
        return target.copy(
            pauseIntervals = target.pauseIntervals + PauseInterval(
                startedAtEpochMs = pauseStartedAtEpochMs,
                startedAtMonotonicMs = pauseStartedAtMonotonicMs
            )
        )
    }

    fun endRestPause(
        target: RestTarget,
        pauseEndedAtEpochMs: Long,
        pauseEndedAtMonotonicMs: Long
    ): RestTarget {
        val pauses = target.pauseIntervals.toMutableList()
        val openIndex = pauses.indexOfLast { it.isOpen() }
        if (openIndex < 0) return target
        pauses[openIndex] = pauses[openIndex].copy(
            endedAtEpochMs = pauseEndedAtEpochMs,
            endedAtMonotonicMs = pauseEndedAtMonotonicMs
        )
        return target.copy(pauseIntervals = pauses)
    }

    fun sessionElapsedSeconds(
        sessionStartedAtEpochMs: Long,
        nowEpochMs: Long,
        sessionPauses: List<PauseInterval> = emptyList(),
        excludePausesFromElapsed: Boolean = false
    ): Pair<Int, Int> {
        val raw = ((nowEpochMs - sessionStartedAtEpochMs).coerceAtLeast(0L) / 1000L).toInt()
        val pauseSec = (pauseTotalMillis(sessionPauses, nowEpochMs) / 1000L).toInt()
        val elapsed = if (excludePausesFromElapsed) (raw - pauseSec).coerceAtLeast(0) else raw
        return elapsed to pauseSec
    }

    /**
     * After missed UI ticks, recompute from stored intervals (TIME-02).
     */
    fun snapshotAfterMissedTicks(
        performedStartEpochMs: Long?,
        performedEndEpochMs: Long?,
        restTarget: RestTarget?,
        sessionStartEpochMs: Long,
        sessionPauses: List<PauseInterval>,
        nowEpochMs: Long,
        bootIdMatches: Boolean
    ): TimerSnapshot {
        val performed = if (performedStartEpochMs != null && performedEndEpochMs != null) {
            performedSeconds(performedStartEpochMs, performedEndEpochMs)
        } else {
            0
        }
        val restElapsed = restTarget?.let { restElapsedSeconds(it, nowEpochMs) } ?: 0
        val restRemaining = restTarget?.let { restRemainingSeconds(it, nowEpochMs) } ?: 0
        val restOvertime = restTarget?.let { restOvertimeSeconds(it, nowEpochMs) } ?: 0
        val (sessionElapsed, pauseTotal) = sessionElapsedSeconds(
            sessionStartedAtEpochMs = sessionStartEpochMs,
            nowEpochMs = nowEpochMs,
            sessionPauses = sessionPauses,
            excludePausesFromElapsed = false
        )
        val confidence = if (bootIdMatches) {
            TimerConfidence.Trusted
        } else {
            TimerConfidence.Estimated("Boot identifier changed; open intervals may be estimated")
        }
        return TimerSnapshot(
            performedSeconds = performed,
            restElapsedSeconds = restElapsed,
            restRemainingSeconds = restRemaining,
            restOvertimeSeconds = restOvertime,
            sessionElapsedSeconds = sessionElapsed,
            pauseTotalSeconds = pauseTotal,
            confidence = confidence
        )
    }
}
