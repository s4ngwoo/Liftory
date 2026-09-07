package com.example.domain.model

data class ExercisePreset(
    val exerciseId: String,
    val defaultWeight: Double,
    val defaultReps: Int,
    val orderIndex: Int
)

data class RoutineTemplate(
    val id: String,
    val name: String,
    val exercises: List<ExercisePreset> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
