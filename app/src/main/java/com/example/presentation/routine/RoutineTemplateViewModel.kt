package com.example.presentation.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.application.usecase.routine.CreateRoutineTemplateUseCase
import com.example.application.usecase.routine.ObserveRoutineTemplatesUseCase
import com.example.domain.model.RoutineTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class RoutineTemplateViewModel(
    private val observeRoutineTemplatesUseCase: ObserveRoutineTemplatesUseCase,
    private val createRoutineTemplateUseCase: CreateRoutineTemplateUseCase,
    private val createWorkoutSessionUseCase: com.example.application.usecase.session.CreateWorkoutSessionUseCase? = null,
    private val applyRoutineTemplateUseCase: com.example.application.usecase.routine.ApplyRoutineTemplateUseCase? = null
) : ViewModel() {

    private val _templates = MutableStateFlow<List<RoutineTemplate>>(emptyList())
    val templates: StateFlow<List<RoutineTemplate>> = _templates.asStateFlow()

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
    }

    fun createEmptyTemplate(name: String) {
        viewModelScope.launch {
            createRoutineTemplateUseCase(name, emptyList())
        }
    }

    fun applyTemplate(templateId: String, onSessionCreated: (String) -> Unit) {
        viewModelScope.launch {
            val template = _templates.value.find { it.id == templateId }
            val sessionName = template?.name ?: "Routine Workout"
            val sessionResult = createWorkoutSessionUseCase?.invoke(sessionName)
            if (sessionResult != null && sessionResult.isSuccess) {
                val session = sessionResult.getOrThrow()
                applyRoutineTemplateUseCase?.invoke(session.id, templateId)
                onSessionCreated(session.id)
            }
        }
    }
}
