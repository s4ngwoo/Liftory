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
        _timerState.value = RestTimerState(isRunning = true, remainingSeconds = seconds)
        
        timerJob = scope.launch {
            while (_timerState.value.remainingSeconds > 0) {
                delay(1000L)
                _timerState.value = _timerState.value.copy(
                    remainingSeconds = _timerState.value.remainingSeconds - 1
                )
            }
            _timerState.value = _timerState.value.copy(isRunning = false, remainingSeconds = 0)
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
        _timerState.value = _timerState.value.copy(isRunning = false, remainingSeconds = 0)
    }
}

data class RestTimerState(
    val isRunning: Boolean,
    val remainingSeconds: Int
)
