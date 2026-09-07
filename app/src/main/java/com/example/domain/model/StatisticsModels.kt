package com.example.domain.model

data class WorkoutVolume(
    val dateMillis: Long,
    val totalVolume: Double
)

data class PersonalRecord(
    val exerciseId: String,
    val maxWeight: Double,
    val achievedAt: Long
)
