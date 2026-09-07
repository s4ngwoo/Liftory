package com.example.presentation.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.application.usecase.exercise.CreateExerciseUseCase
import com.example.application.usecase.exercise.ObserveExercisesUseCase
import com.example.application.usecase.exercise.SearchExercisesUseCase
import com.example.domain.model.Exercise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ExerciseViewModel(
    private val observeExercisesUseCase: ObserveExercisesUseCase,
    private val createExerciseUseCase: CreateExerciseUseCase,
    private val searchExercisesUseCase: SearchExercisesUseCase
) : ViewModel() {

    private val _exercises = MutableStateFlow<List<Exercise>>(emptyList())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    init {
        loadAllExercises()
    }

    private fun loadAllExercises() {
        viewModelScope.launch {
            observeExercisesUseCase()
                .catch { e -> 
                    // Handle error
                }
                .collect { list ->
                    _exercises.value = list
                }
        }
    }

    fun searchExercises(query: String) {
        if (query.isBlank()) {
            loadAllExercises()
            return
        }
        viewModelScope.launch {
            searchExercisesUseCase(query).collect { _exercises.value = it }
        }
    }

    fun createCustomExercise(name: String, muscleGroup: String) {
        viewModelScope.launch {
            createExerciseUseCase(name, muscleGroup)
            // observeAll will automatically update the list
        }
    }
}
