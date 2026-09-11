package com.example.presentation.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.application.usecase.session.CreateWorkoutSessionUseCase
import com.example.application.usecase.session.DeleteWorkoutSessionUseCase
import com.example.application.usecase.session.GetWorkoutSessionUseCase
import com.example.application.usecase.session.ObserveWorkoutSessionsUseCase
import com.example.application.usecase.session.UpdateWorkoutSessionUseCase
import com.example.application.usecase.set.AddExerciseSetUseCase
import com.example.application.usecase.set.ObserveExerciseSetsUseCase
import com.example.domain.model.ExerciseSet
import com.example.domain.model.WorkoutSession
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.example.application.usecase.set.GetLastExerciseHistoryUseCase
import com.example.domain.model.ExerciseHistoryRecord

import com.example.domain.exception.ActiveSessionAlreadyExistsException
import kotlinx.coroutines.flow.map

class WorkoutSessionViewModel(
    private val observeSessionsUseCase: ObserveWorkoutSessionsUseCase,
    private val observeExerciseSetsUseCase: ObserveExerciseSetsUseCase,
    private val createSessionUseCase: CreateWorkoutSessionUseCase,
    private val addExerciseSetUseCase: AddExerciseSetUseCase,
    private val updateWorkoutSessionUseCase: UpdateWorkoutSessionUseCase,
    private val deleteWorkoutSessionUseCase: DeleteWorkoutSessionUseCase,
    private val getWorkoutSessionUseCase: GetWorkoutSessionUseCase,
    private val getLastExerciseHistoryUseCase: GetLastExerciseHistoryUseCase? = null
) : ViewModel() {

    val sessionListUiState: StateFlow<List<WorkoutSession>> = observeSessionsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeSession: StateFlow<WorkoutSession?> = sessionListUiState.map { list ->
        list.firstOrNull { it.endTime == null }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _selectedSessionId = MutableStateFlow<String?>(null)
    val selectedSessionId: StateFlow<String?> = _selectedSessionId.asStateFlow()

    val currentSession: StateFlow<WorkoutSession?> = combine(
        _selectedSessionId,
        sessionListUiState
    ) { id, sessions ->
        sessions.find { it.id == id }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentSessionSets: StateFlow<List<ExerciseSet>> = _selectedSessionId
        .filterNotNull()
        .flatMapLatest { sessionId -> observeExerciseSetsUseCase(sessionId) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _exerciseHistoryMap = MutableStateFlow<Map<String, ExerciseHistoryRecord>>(emptyMap())
    val exerciseHistoryMap: StateFlow<Map<String, ExerciseHistoryRecord>> = _exerciseHistoryMap.asStateFlow()

    init {
        viewModelScope.launch {
            currentSessionSets.collect { sets ->
                val distinctExerciseIds = sets.map { it.exerciseId }.distinct()
                distinctExerciseIds.forEach { exId ->
                    if (!_exerciseHistoryMap.value.containsKey(exId)) {
                        loadHistoryForExercise(exId)
                    }
                }
            }
        }
    }

    fun loadHistoryForExercise(exerciseId: String) {
        if (getLastExerciseHistoryUseCase == null) return
        viewModelScope.launch {
            val record = getLastExerciseHistoryUseCase(exerciseId, _selectedSessionId.value)
            if (record != null) {
                _exerciseHistoryMap.value = _exerciseHistoryMap.value + (exerciseId to record)
            }
        }
    }

    fun createNewSession(
        notes: String,
        finishExistingActive: Boolean = false,
        onActiveConflict: ((WorkoutSession) -> Unit)? = null,
        onCreated: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val result = createSessionUseCase(notes = notes, finishExistingActive = finishExistingActive)
            if (result.isSuccess) {
                val session = result.getOrThrow()
                selectSession(session.id)
                onCreated?.invoke(session.id)
            } else {
                val exception = result.exceptionOrNull()
                if (exception is ActiveSessionAlreadyExistsException) {
                    onActiveConflict?.invoke(exception.activeSession)
                }
            }
        }
    }

    fun createNewSession(
        notes: String,
        onCreated: ((String) -> Unit)? = null
    ) = createNewSession(
        notes = notes,
        finishExistingActive = false,
        onActiveConflict = null,
        onCreated = onCreated
    )


    fun finishSession(sessionId: String, onFinished: (() -> Unit)? = null) {
        viewModelScope.launch {
            val session = sessionListUiState.value.find { it.id == sessionId }
                ?: getWorkoutSessionUseCase(sessionId)
            if (session != null) {
                updateWorkoutSessionUseCase(session.copy(endTime = System.currentTimeMillis()))
                onFinished?.invoke()
            }
        }
    }

    fun selectSession(id: String) {
        _selectedSessionId.value = id
    }


    fun updateSessionNotes(sessionId: String, newNotes: String) {
        viewModelScope.launch {
            val session = sessionListUiState.value.find { it.id == sessionId }
                ?: getWorkoutSessionUseCase(sessionId)
                ?: return@launch
            updateWorkoutSessionUseCase(session.copy(notes = newNotes))
        }
    }

    fun updateSession(session: WorkoutSession) {
        viewModelScope.launch {
            updateWorkoutSessionUseCase(session)
        }
    }

    fun deleteSession(sessionId: String, onDeleted: (() -> Unit)? = null) {
        viewModelScope.launch {
            val result = deleteWorkoutSessionUseCase(sessionId)
            if (result.isSuccess) {
                if (_selectedSessionId.value == sessionId) {
                    _selectedSessionId.value = null
                }
                onDeleted?.invoke()
            }
        }
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
        private val addExerciseSetUseCase: AddExerciseSetUseCase,
        private val updateWorkoutSessionUseCase: UpdateWorkoutSessionUseCase,
        private val deleteWorkoutSessionUseCase: DeleteWorkoutSessionUseCase,
        private val getWorkoutSessionUseCase: GetWorkoutSessionUseCase,
        private val getLastExerciseHistoryUseCase: GetLastExerciseHistoryUseCase? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WorkoutSessionViewModel(
                observeSessionsUseCase,
                observeExerciseSetsUseCase,
                createSessionUseCase,
                addExerciseSetUseCase,
                updateWorkoutSessionUseCase,
                deleteWorkoutSessionUseCase,
                getWorkoutSessionUseCase,
                getLastExerciseHistoryUseCase
            ) as T
        }
    }
}
