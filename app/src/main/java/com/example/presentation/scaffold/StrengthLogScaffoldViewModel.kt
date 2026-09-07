package com.example.presentation.scaffold

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.application.usecase.exercise.CreateExerciseUseCase
import com.example.application.usecase.exercise.ObserveExercisesUseCase
import com.example.application.usecase.session.CreateWorkoutSessionUseCase
import com.example.application.usecase.session.ObserveWorkoutSessionsUseCase
import com.example.application.usecase.set.AddExerciseSetUseCase
import com.example.application.usecase.sync.StartSyncWorkUseCase
import com.example.domain.repository.SyncQueueRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StrengthLogScaffoldViewModel(
    private val observeSessionsUseCase: ObserveWorkoutSessionsUseCase,
    private val observeExercisesUseCase: ObserveExercisesUseCase,
    private val createSessionUseCase: CreateWorkoutSessionUseCase,
    private val syncQueueRepository: SyncQueueRepository,
    private val startSyncWorkUseCase: StartSyncWorkUseCase,
    private val createExerciseUseCase: CreateExerciseUseCase,
    private val addExerciseSetUseCase: AddExerciseSetUseCase
) : ViewModel() {

    val sessionCount = observeSessionsUseCase().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _pendingUploadCount = MutableStateFlow(0)
    val pendingUploadCount: StateFlow<Int> = _pendingUploadCount.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    init {
        viewModelScope.launch {
            syncQueueRepository.observePendingCount().collect { count ->
                _pendingUploadCount.value = count
            }
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    fun createSampleSession() {
        viewModelScope.launch {
            val sessionResult = createSessionUseCase("Leg Day")
            if (sessionResult.isSuccess) {
                val session = sessionResult.getOrThrow()
                
                // Add default exercises if missing
                val exercises = observeExercisesUseCase().first()
                var squatEx = exercises.find { it.name.contains("Squat", ignoreCase = true) }
                var benchEx = exercises.find { it.name.contains("Bench", ignoreCase = true) }
                
                if (squatEx == null) {
                    val createResult = createExerciseUseCase("Squat", "Legs")
                    if (createResult.isSuccess) squatEx = createResult.getOrThrow()
                }
                if (benchEx == null) {
                    val createResult = createExerciseUseCase("Bench Press", "Chest")
                    if (createResult.isSuccess) benchEx = createResult.getOrThrow()
                }
                
                if (squatEx != null) {
                    addExerciseSetUseCase(session.id, squatEx.id, 100.0, 5, null, 120, 1)
                    addExerciseSetUseCase(session.id, squatEx.id, 110.0, 5, null, 120, 2)
                    addExerciseSetUseCase(session.id, squatEx.id, 120.0, 3, 8.5, 180, 3)
                }
                
                if (benchEx != null) {
                    addExerciseSetUseCase(session.id, benchEx.id, 80.0, 10, null, 90, 4)
                    addExerciseSetUseCase(session.id, benchEx.id, 90.0, 5, null, 120, 5)
                    addExerciseSetUseCase(session.id, benchEx.id, 100.0, 5, 9.0, 180, 6)
                }
                
                _actionMessage.value = "실제 운동 기록 생성 완료 (Session: ${session.id.take(8)})"
            } else {
                _actionMessage.value = "Error: ${sessionResult.exceptionOrNull()?.message}"
            }
        }
    }

    fun triggerSync() {
        startSyncWorkUseCase()
        _actionMessage.value = "Sync worker enqueued"
    }

    class Factory(
        private val observeSessionsUseCase: ObserveWorkoutSessionsUseCase,
        private val observeExercisesUseCase: ObserveExercisesUseCase,
        private val createSessionUseCase: CreateWorkoutSessionUseCase,
        private val syncQueueRepository: SyncQueueRepository,
        private val startSyncWorkUseCase: StartSyncWorkUseCase,
        private val createExerciseUseCase: CreateExerciseUseCase,
        private val addExerciseSetUseCase: AddExerciseSetUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StrengthLogScaffoldViewModel(
                observeSessionsUseCase,
                observeExercisesUseCase,
                createSessionUseCase,
                syncQueueRepository,
                startSyncWorkUseCase,
                createExerciseUseCase,
                addExerciseSetUseCase
            ) as T
        }
    }
}
