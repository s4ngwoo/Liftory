package com.example.infrastructure.export

import com.example.domain.model.ExerciseSet
import com.example.domain.model.WorkoutSession
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExportDataPayload(
    val schemaVersion: Int = 1,
    val exportedAt: Long,
    val sessions: List<WorkoutSession>,
    val sets: List<ExerciseSet>
)
