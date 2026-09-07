package com.example.infrastructure.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Embedded
import androidx.room.Relation

@Entity(tableName = "routine_templates")
data class RoutineTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "exercise_presets",
    primaryKeys = ["templateId", "orderIndex"],
    foreignKeys = [
        ForeignKey(
            entity = RoutineTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("templateId"),
        Index("exerciseId")
    ]
)
data class ExercisePresetEntity(
    val templateId: String,
    val exerciseId: String,
    val defaultWeight: Double,
    val defaultReps: Int,
    val orderIndex: Int
)

data class RoutineTemplateWithPresets(
    @Embedded val template: RoutineTemplateEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "templateId"
    )
    val presets: List<ExercisePresetEntity>
)
