package com.example.presentation.timer

import com.example.domain.model.timer.RestTarget
import com.example.domain.model.timer.TimerCalculator
import com.example.domain.port.MonotonicClock
import com.example.domain.port.NotificationScheduler
import com.example.domain.port.NoOpNotificationScheduler
import com.example.domain.port.WallClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Presentation adapter over persistent [RestTarget] intervals (N05.5, ADR-002).
 *
 * UI ticks only recompute display values from stored anchors — they do not mutate
 * remaining seconds in memory as the source of truth, and never write DB each second.
 */
class RestTimerManager(
    private val wallClock: WallClock = WallClock.System,
    private val monotonicClock: MonotonicClock = object : MonotonicClock {
        override fun elapsedRealtimeMillis(): Long = wallClock.nowMillis()
    },
    private val notificationScheduler: NotificationScheduler = NoOpNotificationScheduler,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    private val sessionIdForNotifications: String = ""
) {
    private val _timerState = MutableStateFlow(RestTimerState.idle())
    val timerState: StateFlow<RestTimerState> = _timerState.asStateFlow()

    /** Source of truth for countdown rests. Null when idle or in stopwatch mode. */
    var activeRestTarget: RestTarget? = null
        private set

    private var stopwatchStartedAtEpochMs: Long? = null
    private var stopwatchPausedTotalMs: Long = 0L
    private var stopwatchPauseStartedAtEpochMs: Long? = null

    private var tickJob: Job? = null
    private var lastNotificationFailure: Throwable? = null

    fun lastNotificationFailure(): Throwable? = lastNotificationFailure

    fun startTimer(seconds: Int) {
        require(seconds >= 0)
        clearStopwatch()
        val now = wallClock.nowMillis()
        activeRestTarget = RestTarget(
            startedAtEpochMs = now,
            startedAtMonotonicMs = monotonicClock.elapsedRealtimeMillis(),
            targetSeconds = seconds
        )
        refresh()
        startTickLoop()
        notifyRestSafe()
    }

    /**
     * Restore from a persisted rest target (process death / ViewModel recreation).
     * Does not create a duplicate rest interval.
     */
    fun restoreFrom(target: RestTarget) {
        clearStopwatch()
        activeRestTarget = target
        refresh()
        if (target.closedAtEpochMs == null) {
            startTickLoop()
            notifyRestSafe()
        }
    }

    fun startStopwatch() {
        activeRestTarget = null
        val now = wallClock.nowMillis()
        stopwatchStartedAtEpochMs = now
        stopwatchPausedTotalMs = 0L
        stopwatchPauseStartedAtEpochMs = null
        refresh()
        startTickLoop()
    }

    fun addSeconds(seconds: Int) {
        val target = activeRestTarget
        if (target != null && !_timerState.value.isStopwatch) {
            activeRestTarget = TimerCalculator.extendRestTarget(target, seconds.coerceAtLeast(0))
            refresh()
            notifyRestSafe()
        } else {
            startTimer(seconds)
        }
    }

    fun pauseTimer() {
        val target = activeRestTarget
        if (target != null) {
            activeRestTarget = TimerCalculator.beginRestPause(
                target = target,
                pauseStartedAtEpochMs = wallClock.nowMillis(),
                pauseStartedAtMonotonicMs = monotonicClock.elapsedRealtimeMillis()
            )
            refresh()
            return
        }
        if (_timerState.value.isStopwatch && stopwatchPauseStartedAtEpochMs == null) {
            stopwatchPauseStartedAtEpochMs = wallClock.nowMillis()
            refresh()
        }
    }

    fun resumeTimer() {
        val target = activeRestTarget
        if (target != null) {
            activeRestTarget = TimerCalculator.endRestPause(
                target = target,
                pauseEndedAtEpochMs = wallClock.nowMillis(),
                pauseEndedAtMonotonicMs = monotonicClock.elapsedRealtimeMillis()
            )
            refresh()
            return
        }
        val pauseStart = stopwatchPauseStartedAtEpochMs
        if (_timerState.value.isStopwatch && pauseStart != null) {
            stopwatchPausedTotalMs += (wallClock.nowMillis() - pauseStart).coerceAtLeast(0L)
            stopwatchPauseStartedAtEpochMs = null
            refresh()
        }
    }

    fun togglePauseResume() {
        if (_timerState.value.isPaused) resumeTimer() else pauseTimer()
    }

    fun stopTimer() {
        tickJob?.cancel()
        tickJob = null
        activeRestTarget = activeRestTarget?.copy(closedAtEpochMs = wallClock.nowMillis())
        activeRestTarget = null
        clearStopwatch()
        _timerState.value = RestTimerState.idle()
        runCatching { notificationScheduler.clearRestNotification() }
            .onFailure { lastNotificationFailure = it }
            .getOrNull()
    }

    /**
     * Recompute display state from anchors. Safe to call after missed ticks (TIME-02).
     */
    fun refresh() {
        val target = activeRestTarget
        if (target != null) {
            val now = wallClock.nowMillis()
            val remaining = TimerCalculator.restRemainingSeconds(target, now)
            val overtime = TimerCalculator.restOvertimeSeconds(target, now)
            val paused = target.pauseIntervals.any { it.isOpen() }
            _timerState.value = RestTimerState(
                isRunning = true,
                remainingSeconds = remaining,
                overtimeSeconds = overtime,
                isPaused = paused,
                totalDurationSeconds = target.targetSeconds,
                isStopwatch = false
            )
            return
        }
        val started = stopwatchStartedAtEpochMs
        if (started != null) {
            val now = wallClock.nowMillis()
            val openPause = stopwatchPauseStartedAtEpochMs
            val pausedExtra = if (openPause != null) (now - openPause).coerceAtLeast(0L) else 0L
            val elapsed = ((now - started - stopwatchPausedTotalMs - pausedExtra)
                .coerceAtLeast(0L) / 1000L).toInt()
            _timerState.value = RestTimerState(
                isRunning = true,
                remainingSeconds = elapsed,
                overtimeSeconds = 0,
                isPaused = openPause != null,
                totalDurationSeconds = 0,
                isStopwatch = true
            )
            return
        }
        _timerState.value = RestTimerState.idle()
    }

    private fun startTickLoop() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (true) {
                delay(1000L)
                refresh()
            }
        }
    }

    private fun clearStopwatch() {
        stopwatchStartedAtEpochMs = null
        stopwatchPausedTotalMs = 0L
        stopwatchPauseStartedAtEpochMs = null
    }

    private fun notifyRestSafe() {
        val target = activeRestTarget ?: return
        val state = _timerState.value
        val result = notificationScheduler.notifyRestActive(
            sessionId = sessionIdForNotifications,
            remainingSeconds = state.remainingSeconds,
            targetSeconds = target.targetSeconds
        )
        if (result.isFailure) {
            lastNotificationFailure = result.exceptionOrNull()
            // Local timer state is intentionally unchanged (TIME-08).
        }
    }
}

data class RestTimerState(
    val isRunning: Boolean,
    val remainingSeconds: Int,
    val isPaused: Boolean = false,
    val totalDurationSeconds: Int = 0,
    val isStopwatch: Boolean = false,
    val overtimeSeconds: Int = 0
) {
    companion object {
        fun idle() = RestTimerState(
            isRunning = false,
            remainingSeconds = 0,
            isPaused = false,
            totalDurationSeconds = 0,
            isStopwatch = false,
            overtimeSeconds = 0
        )
    }
}
