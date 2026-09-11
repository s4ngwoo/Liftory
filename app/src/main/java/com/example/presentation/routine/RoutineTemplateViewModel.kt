package com.example.presentation.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.application.usecase.routine.ApplyRoutineTemplateUseCase
import com.example.application.usecase.routine.CreateRoutineTemplateUseCase
import com.example.application.usecase.routine.DeleteRoutineTemplateUseCase
import com.example.application.usecase.routine.ObserveRoutineTemplatesUseCase
import com.example.application.usecase.routine.UpdateRoutineTemplateUseCase
import com.example.application.usecase.session.CreateWorkoutSessionUseCase
import com.example.domain.model.ExercisePreset
import com.example.domain.model.RoutineTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.example.application.usecase.session.ObserveWorkoutSessionsUseCase
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

import com.example.domain.exception.ActiveSessionAlreadyExistsException
import com.example.domain.model.WorkoutSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class RoutineTemplateViewModel(

    private val observeRoutineTemplatesUseCase: ObserveRoutineTemplatesUseCase,
    private val createRoutineTemplateUseCase: CreateRoutineTemplateUseCase,
    private val createWorkoutSessionUseCase: CreateWorkoutSessionUseCase? = null,
    private val applyRoutineTemplateUseCase: ApplyRoutineTemplateUseCase? = null,
    private val deleteRoutineTemplateUseCase: DeleteRoutineTemplateUseCase? = null,
    private val updateRoutineTemplateUseCase: UpdateRoutineTemplateUseCase? = null,
    private val observeWorkoutSessionsUseCase: ObserveWorkoutSessionsUseCase? = null
) : ViewModel() {

    private val _templates = MutableStateFlow<List<RoutineTemplate>>(emptyList())
    val templates: StateFlow<List<RoutineTemplate>> = _templates.asStateFlow()

    private val _routineLastWorkoutMap = MutableStateFlow<Map<String, Long>>(emptyMap())
    val routineLastWorkoutMap: StateFlow<Map<String, Long>> = _routineLastWorkoutMap.asStateFlow()

    val activeSession: StateFlow<WorkoutSession?> = observeWorkoutSessionsUseCase?.invoke()
        ?.map { list -> list.firstOrNull { it.endTime == null } }
        ?.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        ?: MutableStateFlow(null)

    init {
        viewModelScope.launch {
            observeRoutineTemplatesUseCase()
                .catch { e -> 
                    // Handle error
                }
                .collect { list ->
                    _templates.value = list
                }
        }

        if (observeWorkoutSessionsUseCase != null) {
            viewModelScope.launch {
                combine(
                    _templates,
                    observeWorkoutSessionsUseCase.invoke().catch { emit(emptyList()) }
                ) { currentTemplates, sessions ->
                    val map = mutableMapOf<String, Long>()
                    currentTemplates.forEach { template ->
                        val matchingSession = sessions.firstOrNull { it.notes.contains(template.name) }
                        if (matchingSession != null) {
                            map[template.id] = matchingSession.startTime
                        }
                    }
                    map
                }.collect {
                    _routineLastWorkoutMap.value = it
                }
            }
        }
    }

    fun createTemplate(name: String, presets: List<ExercisePreset> = emptyList()) {
        viewModelScope.launch {
            createRoutineTemplateUseCase(name, presets)
        }
    }

    fun createEmptyTemplate(name: String) {
        createTemplate(name, emptyList())
    }

    fun deleteTemplate(templateId: String) {
        viewModelScope.launch {
            deleteRoutineTemplateUseCase?.invoke(templateId)
        }
    }

    fun updateTemplate(
        templateId: String,
        name: String,
        exercises: List<ExercisePreset>,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val result = updateRoutineTemplateUseCase?.invoke(templateId, name, exercises)
            if (result != null && result.isSuccess) {
                onSuccess?.invoke()
            }
        }
    }

    fun applyTemplate(
        templateId: String,
        finishExistingActive: Boolean = false,
        onActiveConflict: ((WorkoutSession) -> Unit)? = null,
        onSessionCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val template = _templates.value.find { it.id == templateId }
            val sessionName = template?.name ?: "Routine Workout"
            val sessionResult = createWorkoutSessionUseCase?.invoke(
                notes = sessionName,
                finishExistingActive = finishExistingActive
            )
            if (sessionResult != null) {
                if (sessionResult.isSuccess) {
                    val session = sessionResult.getOrThrow()
                    applyRoutineTemplateUseCase?.invoke(session.id, templateId)
                    onSessionCreated(session.id)
                } else {
                    val ex = sessionResult.exceptionOrNull()
                    if (ex is ActiveSessionAlreadyExistsException) {
                        onActiveConflict?.invoke(ex.activeSession)
                    }
                }
            }
        }
    }

    fun applyTemplate(
        templateId: String,
        onSessionCreated: (String) -> Unit
    ) = applyTemplate(
        templateId = templateId,
        finishExistingActive = false,
        onActiveConflict = null,
        onSessionCreated = onSessionCreated
    )
}


