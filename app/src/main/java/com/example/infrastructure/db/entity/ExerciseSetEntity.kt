package com.example.infrastructure.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercise_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["exerciseId"]),
        Index(value = ["sessionId", "orderIndex"])
    ]
)
data class ExerciseSetEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val exerciseId: String,
    val weight: Double,
    val reps: Int,
    val rpe: Double?,
    val restSeconds: Int?,
    val orderIndex: Int,
    val isCompleted: Boolean = true,
    val targetReps: Int? = null,
    val createdAt: Long,
    val updatedAt: Long
)
