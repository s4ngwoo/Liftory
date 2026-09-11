package com.example.domain.model

data class WorkoutVolume(
    val dateMillis: Long,
    val totalVolume: Double,
    val cardioDurationMinutes: Int = 0
)

data class PersonalRecord(
    val exerciseId: String,
    val exerciseName: String = "",
    val isCardio: Boolean = false,
    val maxWeight: Double = 0.0,
    val maxCardioLevel: Double? = null,
    val maxCardioMinutes: Int? = null,
    val achievedAt: Long
)
