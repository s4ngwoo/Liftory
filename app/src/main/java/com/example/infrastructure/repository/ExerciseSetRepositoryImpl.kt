package com.example.infrastructure.repository

import com.example.domain.model.ExerciseSet
import com.example.domain.repository.ExerciseSetRepository
import com.example.infrastructure.db.dao.ExerciseSetDao
import com.example.infrastructure.db.mapper.toDomain
import com.example.infrastructure.db.mapper.toEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ExerciseSetRepositoryImpl(
    private val setDao: ExerciseSetDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ExerciseSetRepository {

    override suspend fun create(set: ExerciseSet): Result<ExerciseSet> = withContext(ioDispatcher) {
        runCatching {
            setDao.insert(set.toEntity())
            set
        }
    }

    override suspend fun update(set: ExerciseSet): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            setDao.update(set.toEntity())
        }
    }

    override suspend fun delete(id: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            setDao.deleteById(id)
        }
    }

    override fun observeBySession(sessionId: String): Flow<List<ExerciseSet>> {
        return setDao.observeBySession(sessionId).map { list ->
            list.map { it.toDomain() }
        }
    }
}
