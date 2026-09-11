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

    override suspend fun create(session: WorkoutSession): Result<WorkoutSession> {
        sessions.add(session)
        return Result.success(session)
    }
    override suspend fun getById(id: String): WorkoutSession? {
        return sessions.find { it.id == id } ?: WorkoutSession(id = id, startTime = 0L, endTime = null, notes = "", createdAt = 0L, updatedAt = 0L)
    }
    override suspend fun update(session: WorkoutSession): Result<Unit> {
        updatedSession = session
        val idx = sessions.indexOfFirst { it.id == session.id }
        if (idx != -1) {
            sessions[idx] = session
        } else {
            sessions.add(session)
        }
        return Result.success(Unit)
    }
    override suspend fun delete(id: String): Result<Unit> {
        sessions.removeAll { it.id == id }
        return Result.success(Unit)
    }
    override fun observeAll(): Flow<List<WorkoutSession>> = flowOf(sessions)
    override fun observeActiveSession(): Flow<WorkoutSession?> = flowOf(sessions.find { it.endTime == null })
    override suspend fun getActiveSession(): WorkoutSession? = sessions.find { it.endTime == null }
    override suspend fun getActiveSessions(): List<WorkoutSession> = sessions.filter { it.endTime == null }
}


open class FakeSetRepository : ExerciseSetRepository {
    val createdSets = mutableListOf<ExerciseSet>()
    var createdSet: ExerciseSet? = null
    var updatedSet: ExerciseSet? = null

    override suspend fun create(set: ExerciseSet): Result<ExerciseSet> {
        createdSet = set
        createdSets.add(set)
        return Result.success(set)
    }
    override suspend fun update(set: ExerciseSet): Result<Unit> {
        updatedSet = set
        val idx = createdSets.indexOfFirst { it.id == set.id }
        if (idx != -1) {
            createdSets[idx] = set
        }
        return Result.success(Unit)
    }
    override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
    override fun observeBySession(sessionId: String): Flow<List<ExerciseSet>> = flowOf(createdSets)
    override suspend fun getBySession(sessionId: String): List<ExerciseSet> = createdSets.filter { it.sessionId == sessionId }
    override suspend fun getLastHistoryForExercise(
        exerciseId: String,
        currentSessionId: String?
    ): com.example.domain.model.ExerciseHistoryRecord? = null
}

/**
 * Test convenience transaction double.
 * WARNING (N01.6): This does NOT prove database rollback, isolation, or concurrency race safety.
 */
class FakeTransactionProvider : TransactionProvider {
    override suspend fun <T> runAsTransaction(block: suspend () -> T): T {
        return block()
    }
}
