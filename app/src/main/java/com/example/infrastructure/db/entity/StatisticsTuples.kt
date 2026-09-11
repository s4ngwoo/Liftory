package com.example.infrastructure.db.entity

data class SessionVolumeTuple(
    val startTime: Long,
    val totalVolume: Double,
    val cardioMinutes: Int = 0
)

data class PersonalRecordTuple(
    val exerciseId: String,
    val exerciseName: String = "",
    val equipmentType: String = "FREE_WEIGHT",
    val maxWeight: Double = 0.0,
    val maxCardioLevel: Double? = null,
    val maxCardioMinutes: Int? = null,
    val achievedAt: Long
)

data class PastSetTuple(
    val sessionDate: Long,
    val sessionId: String,
    val id: String,
    val exerciseId: String,
    val weight: Double,
    val reps: Int,
    val rpe: Double?,
    val orderIndex: Int
)
