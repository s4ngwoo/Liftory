package com.example.infrastructure.export

import com.example.infrastructure.db.entity.ExerciseSetEntity
import com.example.infrastructure.db.entity.WorkoutSessionEntity
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExportEntityPayload(
    val schemaVersion: Int = 1,
    val exportedAt: Long,
    val sessions: List<WorkoutSessionEntity>,
    val sets: List<ExerciseSetEntity>
)
