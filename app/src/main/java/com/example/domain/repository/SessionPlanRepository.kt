package com.example.domain.repository

import com.example.domain.model.plan.SessionPlan
import kotlinx.coroutines.flow.Flow

interface SessionPlanRepository {
    suspend fun savePlan(plan: SessionPlan): Result<SessionPlan>
    suspend fun getPlanById(id: String): SessionPlan?
    fun observePlan(id: String): Flow<SessionPlan?>
    suspend fun deletePlan(id: String): Result<Unit>
}
