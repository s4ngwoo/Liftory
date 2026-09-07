package com.example.domain.model

/**
 * Represents a strength training exercise (e.g., Squat, Bench Press, Deadlift).
 */
data class Exercise(
    val id: String,
    val name: String,
    val isCustom: Boolean = false,
    val muscleGroup: String = "All",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
