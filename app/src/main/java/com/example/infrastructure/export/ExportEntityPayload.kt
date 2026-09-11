package com.example.infrastructure.export

import com.example.infrastructure.db.entity.ExercisePresetEntity
import com.example.infrastructure.db.entity.ExerciseSetEntity
import com.example.infrastructure.db.entity.RoutineTemplateEntity
import com.example.infrastructure.db.entity.WorkoutSessionEntity
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ExportEntityPayload(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAt: Long,
    val sessions: List<WorkoutSessionEntity>,
    val sets: List<ExerciseSetEntity>,
    val routines: List<RoutineTemplateEntity>? = null,
    val presets: List<ExercisePresetEntity>? = null
) {
    companion object {
        const val SCHEMA_VERSION_V1 = 1
        const val SCHEMA_VERSION_V2 = 2
        const val CURRENT_SCHEMA_VERSION = SCHEMA_VERSION_V2
    }
}
