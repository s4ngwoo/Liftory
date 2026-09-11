package com.example.domain.model.plan

import com.example.domain.model.MeasurementValue

/**
 * A single planned target set within a planned exercise.
 */
data class PlannedSet(
    val id: String,
    val orderIndex: Int,
    val targetMeasurement: MeasurementValue,
    val targetRestSeconds: Int? = null,
    val isCompleted: Boolean = false
)
