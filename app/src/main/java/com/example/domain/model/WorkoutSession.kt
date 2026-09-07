package com.example.domain.model

/**
 * Represents a single strength training workout session.
 * Core domain entity.
 */
data class WorkoutSession(
    val id: String,
    val startTime: Long,
    val endTime: Long? = null,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
