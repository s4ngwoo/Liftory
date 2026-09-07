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
