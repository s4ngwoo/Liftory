package com.example.domain.repository

import com.example.domain.model.WorkoutSession
import kotlinx.coroutines.flow.Flow

interface WorkoutSessionRepository {
    suspend fun create(session: WorkoutSession): Result<WorkoutSession>
    suspend fun getById(id: String): WorkoutSession?
    suspend fun update(session: WorkoutSession): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
    fun observeAll(): Flow<List<WorkoutSession>>
    fun observeActiveSession(): Flow<WorkoutSession?>
}
