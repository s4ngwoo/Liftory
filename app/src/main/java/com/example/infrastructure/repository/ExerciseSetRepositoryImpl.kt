package com.example.infrastructure.repository

import androidx.room.withTransaction
import com.example.domain.model.EntityType
import com.example.domain.model.ExerciseSet
import com.example.domain.model.SyncOperation
import com.example.domain.repository.ExerciseSetRepository
import com.example.infrastructure.db.StrengthLogDatabase
import com.example.infrastructure.db.mapper.toDomain
import com.example.infrastructure.db.mapper.toEntity
import com.example.infrastructure.sync.enqueueOwnedBy
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ExerciseSetRepositoryImpl(
    private val db: StrengthLogDatabase,
    private val currentUserId: () -> String?,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ExerciseSetRepository {

    private val setDao = db.exerciseSetDao()
    private val pendingUploadDao = db.pendingUploadDao()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(ExerciseSet::class.java)

    override suspend fun create(set: ExerciseSet): Result<ExerciseSet> = withContext(ioDispatcher) {
        runCatching {
            db.withTransaction {
                setDao.insert(set.toEntity())
                pendingUploadDao.enqueueOwnedBy(
                    userId = currentUserId(),
                    entityType = EntityType.SET,
                    entityId = set.id,
                    operation = SyncOperation.CREATE,
                    payloadJson = adapter.toJson(set)
                )
            }
            set
        }
    }

    override suspend fun update(set: ExerciseSet): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            db.withTransaction {
                setDao.update(set.toEntity())
                pendingUploadDao.enqueueOwnedBy(
                    userId = currentUserId(),
                    entityType = EntityType.SET,
                    entityId = set.id,
                    operation = SyncOperation.UPDATE,
                    payloadJson = adapter.toJson(set)
                )
            }
        }
    }

    override suspend fun delete(id: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            db.withTransaction {
                setDao.deleteById(id)
                pendingUploadDao.enqueueOwnedBy(
                    userId = currentUserId(),
                    entityType = EntityType.SET,
                    entityId = id,
                    operation = SyncOperation.DELETE,
                    payloadJson = "{}"
                )
            }
        }
    }

    override fun observeBySession(sessionId: String): Flow<List<ExerciseSet>> {
        return setDao.observeBySession(sessionId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getBySession(sessionId: String): List<ExerciseSet> = withContext(ioDispatcher) {
        setDao.getBySession(sessionId).map { it.toDomain() }
    }

    override suspend fun getLastHistoryForExercise(
        exerciseId: String,
        currentSessionId: String?
    ): com.example.domain.model.ExerciseHistoryRecord? = withContext(ioDispatcher) {
        val tuples = setDao.getPastSetsForExercise(exerciseId, currentSessionId)
        if (tuples.isEmpty()) return@withContext null

        val firstSessionId = tuples.first().sessionId
        val sessionDate = tuples.first().sessionDate
        val sessionSets = tuples.filter { it.sessionId == firstSessionId }
            .sortedBy { it.orderIndex }
            .mapIndexed { index, tuple ->
                com.example.domain.model.ExerciseSetSummary(
                    setNumber = index + 1,
                    weight = tuple.weight,
                    reps = tuple.reps,
                    rpe = tuple.rpe
                )
            }

        com.example.domain.model.ExerciseHistoryRecord(
            exerciseId = exerciseId,
            sessionId = firstSessionId,
            sessionDate = sessionDate,
            sets = sessionSets
        )
    }
}
