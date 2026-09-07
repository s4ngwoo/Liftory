package com.example.presentation.scaffold

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.application.usecase.exercise.ObserveExercisesUseCase
import com.example.application.usecase.session.CreateWorkoutSessionUseCase
import com.example.application.usecase.session.ObserveWorkoutSessionsUseCase
import com.example.domain.model.Exercise
import com.example.domain.model.WorkoutSession
import com.example.domain.repository.SyncQueueRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ScaffoldingUiState(
    val isDatabaseInitialized: Boolean = true,
    val sessions: List<WorkoutSession> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
    val pendingUploadsCount: Int = 0,
    val isCreatingSession: Boolean = false,
    val lastActionMessage: String? = null
)

class StrengthLogScaffoldViewModel(
    private val observeSessionsUseCase: ObserveWorkoutSessionsUseCase,
    private val observeExercisesUseCase: ObserveExercisesUseCase,
    private val createSessionUseCase: CreateWorkoutSessionUseCase,
    private val syncQueueRepository: SyncQueueRepository
) : ViewModel() {

    private val _actionMessage = MutableStateFlow<String?>("Clean Architecture Scaffolding Ready")

    val uiState: StateFlow<ScaffoldingUiState> = combine(
        observeSessionsUseCase(),
        observeExercisesUseCase(),
        syncQueueRepository.observePendingCount(),
        _actionMessage
    ) { sessions, exercises, pendingCount, message ->
        ScaffoldingUiState(
            isDatabaseInitialized = true,
            sessions = sessions,
            exercises = exercises,
            pendingUploadsCount = pendingCount,
            lastActionMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ScaffoldingUiState()
    )

    fun createSampleSession() {
        viewModelScope.launch {
            val result = createSessionUseCase(notes = "Sprint 0 Scaffolding Verification Session")
            if (result.isSuccess) {
                _actionMessage.value = "Session created successfully in Room DB: ${result.getOrNull()?.id?.take(8)}"
            } else {
                _actionMessage.value = "Error: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    class Factory(
        private val observeSessionsUseCase: ObserveWorkoutSessionsUseCase,
        private val observeExercisesUseCase: ObserveExercisesUseCase,
        private val createSessionUseCase: CreateWorkoutSessionUseCase,
        private val syncQueueRepository: SyncQueueRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StrengthLogScaffoldViewModel(
                observeSessionsUseCase,
                observeExercisesUseCase,
                createSessionUseCase,
                syncQueueRepository
            ) as T
        }
    }
}
