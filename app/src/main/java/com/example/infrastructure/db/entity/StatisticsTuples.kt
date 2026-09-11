package com.example.infrastructure.db.entity

data class SessionVolumeTuple(
    val startTime: Long,
    val totalVolume: Double
)

data class PersonalRecordTuple(
    val exerciseId: String,
    val maxWeight: Double,
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
