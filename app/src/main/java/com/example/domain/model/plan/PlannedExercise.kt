package com.example.domain.model.plan

/**
 * An exercise planned in a workout session.
 * [id] is distinct per placement in the routine, so the same exercise can appear at multiple positions (PLAN-04).
 */
data class PlannedExercise(
    val id: String,
    val exerciseId: String,
    val exerciseName: String,
    val orderIndex: Int,
    val plannedSets: List<PlannedSet>,
    val notes: String = ""
)
