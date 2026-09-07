package com.example.infrastructure.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercises",
    indices = [
        Index(value = ["name"]),
        Index(value = ["muscleGroup"])
    ]
)
data class ExerciseEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val isCustom: Boolean,
    val muscleGroup: String,
    val createdAt: Long,
    val updatedAt: Long
)
