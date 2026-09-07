package com.example.infrastructure.repository

import androidx.room.withTransaction
import com.example.domain.model.EntityType
import com.example.domain.model.Exercise
import com.example.domain.model.PendingUpload
import com.example.domain.model.SyncOperation
import com.example.domain.repository.ExerciseRepository
import com.example.infrastructure.db.StrengthLogDatabase
import com.example.infrastructure.db.mapper.toDomain
import com.example.infrastructure.db.mapper.toEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class ExerciseRepositoryImpl(
    private val db: StrengthLogDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ExerciseRepository {

    private val exerciseDao = db.exerciseDao()
    private val pendingUploadDao = db.pendingUploadDao()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(Exercise::class.java)

    override suspend fun create(exercise: Exercise): Result<Exercise> = withContext(ioDispatcher) {
        runCatching {
            db.withTransaction {
                exerciseDao.insert(exercise.toEntity())
                if (exercise.isCustom) {
                    val pending = PendingUpload(
                        id = UUID.randomUUID().toString(),
                        entityType = EntityType.EXERCISE,
                        entityId = exercise.id,
                        operation = SyncOperation.CREATE,
                        payloadJson = adapter.toJson(exercise)
                    )
                    pendingUploadDao.insert(pending.toEntity())
                }
            }
            exercise
        }
    }

    override suspend fun update(exercise: Exercise): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            db.withTransaction {
                exerciseDao.update(exercise.toEntity())
                if (exercise.isCustom) {
                    val pending = PendingUpload(
                        id = UUID.randomUUID().toString(),
                        entityType = EntityType.EXERCISE,
                        entityId = exercise.id,
                        operation = SyncOperation.UPDATE,
                        payloadJson = adapter.toJson(exercise)
                    )
                    pendingUploadDao.insert(pending.toEntity())
                }
            }
        }
    }

    override suspend fun delete(id: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            db.withTransaction {
                val exercise = exerciseDao.getById(id)
                exerciseDao.deleteById(id)
                if (exercise?.isCustom == true) {
                    val pending = PendingUpload(
                        id = UUID.randomUUID().toString(),
                        entityType = EntityType.EXERCISE,
                        entityId = id,
                        operation = SyncOperation.DELETE,
                        payloadJson = "{}"
                    )
                    pendingUploadDao.insert(pending.toEntity())
                }
            }
        }
    }

    override fun observeAll(): Flow<List<Exercise>> {
        return exerciseDao.observeAll().map { list ->
            list.map { it.toDomain() }
        }
    }
}
