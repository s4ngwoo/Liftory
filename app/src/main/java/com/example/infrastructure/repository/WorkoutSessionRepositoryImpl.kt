package com.example.infrastructure.repository

import com.example.domain.model.WorkoutSession
import com.example.domain.repository.WorkoutSessionRepository
import com.example.infrastructure.db.dao.ExerciseSetDao
import com.example.infrastructure.db.dao.WorkoutSessionDao
import com.example.infrastructure.db.mapper.toDomain
import com.example.infrastructure.db.mapper.toEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class WorkoutSessionRepositoryImpl(
    private val sessionDao: WorkoutSessionDao,
    private val setDao: ExerciseSetDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : WorkoutSessionRepository {

    override suspend fun create(session: WorkoutSession): Result<WorkoutSession> = withContext(ioDispatcher) {
        runCatching {
            sessionDao.insert(session.toEntity())
            session
        }
    }

    override suspend fun getById(id: String): WorkoutSession? = withContext(ioDispatcher) {
        sessionDao.getById(id)?.toDomain()
    }

    override suspend fun update(session: WorkoutSession): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            sessionDao.update(session.toEntity())
        }
    }

    override suspend fun delete(id: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            setDao.deleteBySessionId(id)
            sessionDao.deleteById(id)
        }
    }

    override fun observeAll(): Flow<List<WorkoutSession>> {
        return sessionDao.observeAll().map { list ->
            list.map { it.toDomain() }
        }
    }
}
