package com.example.domain.model

data class ExerciseSetSummary(
    val setNumber: Int,
    val weight: Double,
    val reps: Int,
    val rpe: Double?
)

data class ExerciseHistoryRecord(
    val exerciseId: String,
    val sessionId: String,
    val sessionDate: Long,
    val sets: List<ExerciseSetSummary>
)
