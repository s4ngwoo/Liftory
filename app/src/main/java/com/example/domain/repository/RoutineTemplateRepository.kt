package com.example.domain.repository

import com.example.domain.model.RoutineTemplate
import kotlinx.coroutines.flow.Flow

interface RoutineTemplateRepository {
    fun observeAll(): Flow<List<RoutineTemplate>>
    suspend fun getById(id: String): RoutineTemplate?
    suspend fun create(template: RoutineTemplate): Result<RoutineTemplate>
    suspend fun update(template: RoutineTemplate): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
}
