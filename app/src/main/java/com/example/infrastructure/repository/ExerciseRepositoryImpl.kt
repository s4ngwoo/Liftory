package com.example.infrastructure.repository

import com.example.domain.model.Exercise
import com.example.domain.repository.ExerciseRepository
import com.example.infrastructure.db.dao.ExerciseDao
import com.example.infrastructure.db.mapper.toDomain
import com.example.infrastructure.db.mapper.toEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ExerciseRepositoryImpl(
    private val exerciseDao: ExerciseDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ExerciseRepository {

    override suspend fun create(exercise: Exercise): Result<Exercise> = withContext(ioDispatcher) {
        runCatching {
            exerciseDao.insert(exercise.toEntity())
            exercise
        }
    }

    override suspend fun getById(id: String): Exercise? = withContext(ioDispatcher) {
        exerciseDao.getById(id)?.toDomain()
    }

    override suspend fun update(exercise: Exercise): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            exerciseDao.update(exercise.toEntity())
        }
    }

    override suspend fun delete(id: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            exerciseDao.deleteById(id)
        }
    }

    override fun observeAll(): Flow<List<Exercise>> {
        return exerciseDao.observeAll().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun search(query: String): List<Exercise> = withContext(ioDispatcher) {
        exerciseDao.searchByName(query).map { it.toDomain() }
    }
}
