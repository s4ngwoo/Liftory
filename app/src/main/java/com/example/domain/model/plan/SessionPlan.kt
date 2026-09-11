package com.example.domain.model.plan

/**
 * An immutable plan snapshot for a workout session.
 * Detached from routine mutations/deletions once created (ADR-001, PLAN-01, PLAN-06).
 */
data class SessionPlan(
    val id: String,
    val routineId: String? = null,
    val routineVersion: Int? = null,
    val name: String,
    val exercises: List<PlannedExercise>,
    val isConfirmed: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)
