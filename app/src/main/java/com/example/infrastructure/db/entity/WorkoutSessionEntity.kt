package com.example.infrastructure.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_sessions",
    indices = [
        Index(value = ["startTime"]),
        Index(value = ["updatedAt"])
    ]
)
data class WorkoutSessionEntity(
    @PrimaryKey
    val id: String,
    val startTime: Long,
    val endTime: Long?,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long
)
