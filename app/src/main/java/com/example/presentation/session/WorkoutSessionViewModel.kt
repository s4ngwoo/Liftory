package com.example.presentation.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.application.usecase.session.CreateWorkoutSessionUseCase
import com.example.application.usecase.session.ObserveWorkoutSessionsUseCase
import com.example.application.usecase.set.AddExerciseSetUseCase
import com.example.application.usecase.set.ObserveExerciseSetsUseCase
import com.example.domain.model.ExerciseSet
import com.example.domain.model.WorkoutSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkoutSessionViewModel(
    private val observeSessionsUseCase: ObserveWorkoutSessionsUseCase,
    private val observeExerciseSetsUseCase: ObserveExerciseSetsUseCase,
    private val createSessionUseCase: CreateWorkoutSessionUseCase,
    private val addExerciseSetUseCase: AddExerciseSetUseCase
) : ViewModel() {

    val sessionListUiState: StateFlow<List<WorkoutSession>> = observeSessionsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedSessionId = MutableStateFlow<String?>(null)
    val selectedSessionId: StateFlow<String?> = _selectedSessionId.asStateFlow()

    val currentSessionSets: StateFlow<List<ExerciseSet>> = _selectedSessionId
        .filterNotNull()
        .flatMapLatest { sessionId -> observeExerciseSetsUseCase(sessionId) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createNewSession(notes: String) {
        viewModelScope.launch {
            createSessionUseCase(notes = notes)
        }
    }

    fun selectSession(id: String) {
        _selectedSessionId.value = id
    }

    fun addSet(exerciseId: String, weight: Double, reps: Int, rpe: Double?) {
        val sessionId = _selectedSessionId.value ?: return
        viewModelScope.launch {
            addExerciseSetUseCase(
                sessionId = sessionId,
                exerciseId = exerciseId,
                weight = weight,
                reps = reps,
                rpe = rpe
            )
        }
    }

    class Factory(
        private val observeSessionsUseCase: ObserveWorkoutSessionsUseCase,
        private val observeExerciseSetsUseCase: ObserveExerciseSetsUseCase,
        private val createSessionUseCase: CreateWorkoutSessionUseCase,
        private val addExerciseSetUseCase: AddExerciseSetUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WorkoutSessionViewModel(
                observeSessionsUseCase,
                observeExerciseSetsUseCase,
                createSessionUseCase,
                addExerciseSetUseCase
            ) as T
        }
    }
}
