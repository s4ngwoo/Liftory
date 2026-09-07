package com.example.domain.model

/**
 * Represents a single performed exercise set within a workout session.
 */
data class ExerciseSet(
    val id: String,
    val sessionId: String,
    val exerciseId: String,
    val weight: Double,
    val reps: Int,
    val rpe: Double? = null,
    val restSeconds: Int? = null,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
