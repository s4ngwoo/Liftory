package com.example.domain.repository

import com.example.domain.model.execution.WorkoutExecution
import kotlinx.coroutines.flow.Flow

/**
 * Persistence port for active/completed workout execution aggregates (N04).
 */
interface WorkoutExecutionRepository {
    suspend fun save(execution: WorkoutExecution): Result<WorkoutExecution>
    suspend fun getBySessionId(sessionId: String): WorkoutExecution?
    fun observeBySessionId(sessionId: String): Flow<WorkoutExecution?>
    suspend fun getActiveExecution(): WorkoutExecution?
}
