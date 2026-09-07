package com.example.domain.repository

import com.example.domain.model.Exercise
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun observeAll(): Flow<List<Exercise>>
    suspend fun getById(id: String): Result<Exercise>
    fun search(query: String): Flow<List<Exercise>>
    fun getExercisesByCategory(category: String): Flow<List<Exercise>>
    suspend fun create(exercise: Exercise): Result<Exercise>
    suspend fun update(exercise: Exercise): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
}
