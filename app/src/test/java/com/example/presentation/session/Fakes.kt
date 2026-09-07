package com.example.presentation.session

import com.example.domain.model.ExerciseSet
import com.example.domain.model.WorkoutSession
import com.example.domain.repository.ExerciseSetRepository
import com.example.domain.repository.TransactionProvider
import com.example.domain.repository.WorkoutSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeSessionRepository : WorkoutSessionRepository {
    val sessions = mutableListOf<WorkoutSession>()
    var updatedSession: WorkoutSession? = null

    override suspend fun create(session: WorkoutSession): Result<WorkoutSession> = Result.success(session)
    override suspend fun getById(id: String): WorkoutSession? {
        return sessions.find { it.id == id } ?: WorkoutSession(id = id, startTime = 0L, endTime = null, notes = "", createdAt = 0L, updatedAt = 0L)
    }
    override suspend fun update(session: WorkoutSession): Result<Unit> {
        updatedSession = session
        return Result.success(Unit)
    }
    override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    override fun observeAll(): Flow<List<WorkoutSession>> = flowOf(emptyList())
}

class FakeSetRepository : ExerciseSetRepository {
    val createdSets = mutableListOf<ExerciseSet>()
    var createdSet: ExerciseSet? = null

    override suspend fun create(set: ExerciseSet): Result<ExerciseSet> {
        createdSet = set
        createdSets.add(set)
        return Result.success(set)
    }
    override suspend fun update(set: ExerciseSet): Result<Unit> = Result.success(Unit)
    override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    override fun observeBySession(sessionId: String): Flow<List<ExerciseSet>> = flowOf(emptyList())
}

class FakeTransactionProvider : TransactionProvider {
    override suspend fun <T> runAsTransaction(block: suspend () -> T): T {
        return block()
    }
}
