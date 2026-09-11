package com.example.presentation.timer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RestTimerManager {
    private val _timerState = MutableStateFlow(RestTimerState(isRunning = false, remainingSeconds = 0))
    val timerState: StateFlow<RestTimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun startTimer(seconds: Int) {
        timerJob?.cancel()
        _timerState.value = RestTimerState(
            isRunning = true,
            isPaused = false,
            remainingSeconds = seconds,
            totalDurationSeconds = seconds,
            isStopwatch = false
        )
        
        timerJob = scope.launch {
            while (_timerState.value.remainingSeconds > 0) {
                delay(1000L)
                if (!_timerState.value.isPaused) {
                    _timerState.value = _timerState.value.copy(
                        remainingSeconds = _timerState.value.remainingSeconds - 1
                    )
                }
            }
            _timerState.value = _timerState.value.copy(isRunning = false, isPaused = false, remainingSeconds = 0)
        }
    }

    fun startStopwatch() {
        timerJob?.cancel()
        _timerState.value = RestTimerState(
            isRunning = true,
            isPaused = false,
            remainingSeconds = 0,
            totalDurationSeconds = 0,
            isStopwatch = true
        )

        timerJob = scope.launch {
            while (true) {
                delay(1000L)
                if (!_timerState.value.isPaused) {
                    _timerState.value = _timerState.value.copy(
                        remainingSeconds = _timerState.value.remainingSeconds + 1
                    )
                }
            }
        }
    }

    fun addSeconds(seconds: Int) {
        val current = _timerState.value
        if (current.isRunning && !current.isStopwatch) {
            _timerState.value = current.copy(
                remainingSeconds = current.remainingSeconds + seconds,
                totalDurationSeconds = current.totalDurationSeconds + seconds
            )
        } else {
            startTimer(seconds)
        }
    }

    fun pauseTimer() {
        if (_timerState.value.isRunning) {
            _timerState.value = _timerState.value.copy(isPaused = true)
        }
    }

    fun resumeTimer() {
        if (_timerState.value.isRunning) {
            _timerState.value = _timerState.value.copy(isPaused = false)
        }
    }

    fun togglePauseResume() {
        if (_timerState.value.isPaused) {
            resumeTimer()
        } else {
            pauseTimer()
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        _timerState.value = RestTimerState(
            isRunning = false,
            isPaused = false,
            remainingSeconds = 0,
            totalDurationSeconds = 0,
            isStopwatch = false
        )
    }
}

data class RestTimerState(
    val isRunning: Boolean,
    val remainingSeconds: Int,
    val isPaused: Boolean = false,
    val totalDurationSeconds: Int = 0,
    val isStopwatch: Boolean = false
)
