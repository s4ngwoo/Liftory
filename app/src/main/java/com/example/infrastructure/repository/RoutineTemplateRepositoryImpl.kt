package com.example.infrastructure.repository

import androidx.room.withTransaction
import com.example.domain.model.EntityType
import com.example.domain.model.ExercisePreset
import com.example.domain.model.PendingUpload
import com.example.domain.model.RoutineTemplate
import com.example.domain.model.SyncOperation
import com.example.domain.repository.RoutineTemplateRepository
import com.example.infrastructure.db.StrengthLogDatabase
import com.example.infrastructure.db.entity.ExercisePresetEntity
import com.example.infrastructure.db.entity.RoutineTemplateEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import com.example.infrastructure.db.mapper.toEntity
import com.example.infrastructure.db.mapper.toDomain

class RoutineTemplateRepositoryImpl(
    private val database: StrengthLogDatabase,
    private val ioDispatcher: CoroutineDispatcher
) : RoutineTemplateRepository {

    private val dao = database.routineTemplateDao()
    private val pendingUploadDao = database.pendingUploadDao()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(RoutineTemplate::class.java)

    override fun observeAll(): Flow<List<RoutineTemplate>> {
        return dao.observeAll().map { list ->
            list.map { entity ->
                RoutineTemplate(
                    id = entity.template.id,
                    name = entity.template.name,
                    exercises = entity.presets.map { preset ->
                        ExercisePreset(
                            exerciseId = preset.exerciseId,
                            defaultWeight = preset.defaultWeight,
                            defaultReps = preset.defaultReps,
                            orderIndex = preset.orderIndex
                        )
                    }.sortedBy { it.orderIndex },
                    createdAt = entity.template.createdAt,
                    updatedAt = entity.template.updatedAt
                )
            }
        }
    }

    override suspend fun getById(id: String): RoutineTemplate? = withContext(ioDispatcher) {
        val entity = dao.getById(id) ?: return@withContext null
        RoutineTemplate(
            id = entity.template.id,
            name = entity.template.name,
            exercises = entity.presets.map { preset ->
                ExercisePreset(
                    exerciseId = preset.exerciseId,
                    defaultWeight = preset.defaultWeight,
                    defaultReps = preset.defaultReps,
                    orderIndex = preset.orderIndex
                )
            }.sortedBy { it.orderIndex },
            createdAt = entity.template.createdAt,
            updatedAt = entity.template.updatedAt
        )
    }

    override suspend fun create(template: RoutineTemplate): Result<RoutineTemplate> = withContext(ioDispatcher) {
        try {
            database.withTransaction {
                dao.insertTemplate(
                    RoutineTemplateEntity(
                        id = template.id,
                        name = template.name,
                        createdAt = template.createdAt,
                        updatedAt = template.updatedAt
                    )
                )
                dao.insertPresets(
                    template.exercises.map { preset ->
                        ExercisePresetEntity(
                            templateId = template.id,
                            exerciseId = preset.exerciseId,
                            defaultWeight = preset.defaultWeight,
                            defaultReps = preset.defaultReps,
                            orderIndex = preset.orderIndex
                        )
                    }
                )
                
                val pending = PendingUpload(
                    id = UUID.randomUUID().toString(),
                    entityType = EntityType.ROUTINE,
                    entityId = template.id,
                    operation = SyncOperation.CREATE,
                    payloadJson = adapter.toJson(template)
                )
                pendingUploadDao.replaceForEntity(pending.toEntity())
            }
            Result.success(template)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun update(template: RoutineTemplate): Result<Unit> = withContext(ioDispatcher) {
        try {
            database.withTransaction {
                dao.updateTemplate(
                    RoutineTemplateEntity(
                        id = template.id,
                        name = template.name,
                        createdAt = template.createdAt,
                        updatedAt = template.updatedAt
                    )
                )
                dao.deletePresetsByTemplateId(template.id)
                dao.insertPresets(
                    template.exercises.map { preset ->
                        ExercisePresetEntity(
                            templateId = template.id,
                            exerciseId = preset.exerciseId,
                            defaultWeight = preset.defaultWeight,
                            defaultReps = preset.defaultReps,
                            orderIndex = preset.orderIndex
                        )
                    }
                )
                
                val pending = PendingUpload(
                    id = UUID.randomUUID().toString(),
                    entityType = EntityType.ROUTINE,
                    entityId = template.id,
                    operation = SyncOperation.UPDATE,
                    payloadJson = adapter.toJson(template)
                )
                pendingUploadDao.replaceForEntity(pending.toEntity())
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun delete(id: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            database.withTransaction {
                dao.deleteTemplateById(id)
                
                val pending = PendingUpload(
                    id = UUID.randomUUID().toString(),
                    entityType = EntityType.ROUTINE,
                    entityId = id,
                    operation = SyncOperation.DELETE,
                    payloadJson = "{}"
                )
                pendingUploadDao.replaceForEntity(pending.toEntity())
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
