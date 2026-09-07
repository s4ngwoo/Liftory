package com.example.domain.repository

import com.example.domain.model.Exercise
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    suspend fun create(exercise: Exercise): Result<Exercise>
    suspend fun getById(id: String): Exercise?
    suspend fun update(exercise: Exercise): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
    fun observeAll(): Flow<List<Exercise>>
    suspend fun search(query: String): List<Exercise>
}
