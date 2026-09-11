package com.example.domain.repository

import com.example.domain.model.ExerciseSet
import kotlinx.coroutines.flow.Flow

interface ExerciseSetRepository {
    suspend fun create(set: ExerciseSet): Result<ExerciseSet>
    suspend fun update(set: ExerciseSet): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
    fun observeBySession(sessionId: String): Flow<List<ExerciseSet>>
    suspend fun getBySession(sessionId: String): List<ExerciseSet>
    suspend fun getLastHistoryForExercise(exerciseId: String, currentSessionId: String? = null): com.example.domain.model.ExerciseHistoryRecord?
}
