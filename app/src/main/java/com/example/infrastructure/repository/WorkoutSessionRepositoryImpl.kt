package com.example.infrastructure.repository

import androidx.room.withTransaction
import com.example.domain.model.EntityType
import com.example.domain.model.PendingUpload
import com.example.domain.model.SyncOperation
import com.example.domain.model.WorkoutSession
import com.example.domain.repository.WorkoutSessionRepository
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

class WorkoutSessionRepositoryImpl(
    private val db: StrengthLogDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : WorkoutSessionRepository {

    private val sessionDao = db.workoutSessionDao()
    private val setDao = db.exerciseSetDao()
    private val pendingUploadDao = db.pendingUploadDao()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(WorkoutSession::class.java)

    override suspend fun create(session: WorkoutSession): Result<WorkoutSession> = withContext(ioDispatcher) {
        runCatching {
            db.withTransaction {
                sessionDao.insert(session.toEntity())
                val pending = PendingUpload(
                    id = UUID.randomUUID().toString(),
                    entityType = EntityType.SESSION,
                    entityId = session.id,
                    operation = SyncOperation.CREATE,
                    payloadJson = adapter.toJson(session)
                )
                pendingUploadDao.insert(pending.toEntity())
            }
            session
        }
    }

    override suspend fun getById(id: String): WorkoutSession? = withContext(ioDispatcher) {
        sessionDao.getById(id)?.toDomain()
    }

    override suspend fun update(session: WorkoutSession): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            db.withTransaction {
                sessionDao.update(session.toEntity())
                val pending = PendingUpload(
                    id = UUID.randomUUID().toString(),
                    entityType = EntityType.SESSION,
                    entityId = session.id,
                    operation = SyncOperation.UPDATE,
                    payloadJson = adapter.toJson(session)
                )
                pendingUploadDao.insert(pending.toEntity())
            }
        }
    }

    override suspend fun delete(id: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            db.withTransaction {
                setDao.deleteBySessionId(id)
                sessionDao.deleteById(id)
                val pending = PendingUpload(
                    id = UUID.randomUUID().toString(),
                    entityType = EntityType.SESSION,
                    entityId = id,
                    operation = SyncOperation.DELETE,
                    payloadJson = "{}"
                )
                pendingUploadDao.insert(pending.toEntity())
            }
        }
    }

    override fun observeAll(): Flow<List<WorkoutSession>> {
        return sessionDao.observeAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun observeActiveSession(): Flow<WorkoutSession?> {
        return sessionDao.observeActiveSession().map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun getActiveSession(): WorkoutSession? = withContext(ioDispatcher) {
        val activeEntities = sessionDao.getActiveSessions()
        if (activeEntities.isEmpty()) return@withContext null

        if (activeEntities.size > 1) {
            val keepActive = activeEntities.first()
            val now = System.currentTimeMillis()
            for (i in 1 until activeEntities.size) {
                val older = activeEntities[i]
                val closeTime = if (older.updatedAt > older.startTime) older.updatedAt else older.startTime + 3600000L
                val closed = older.copy(
                    endTime = closeTime,
                    updatedAt = now
                )
                sessionDao.update(closed)
            }
            keepActive.toDomain()
        } else {
            activeEntities.first().toDomain()
        }
    }

    override suspend fun getActiveSessions(): List<WorkoutSession> = withContext(ioDispatcher) {
        sessionDao.getActiveSessions().map { it.toDomain() }
    }
}

