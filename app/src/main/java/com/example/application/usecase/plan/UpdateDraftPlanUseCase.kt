package com.example.application.usecase.plan

import com.example.domain.model.plan.SessionPlan
import com.example.domain.port.WallClock
import com.example.domain.repository.SessionPlanRepository

class UpdateDraftPlanUseCase(
    private val planRepository: SessionPlanRepository,
    private val wallClock: WallClock
) {
    suspend operator fun invoke(plan: SessionPlan): Result<SessionPlan> {
        val updatedPlan = plan.copy(
            updatedAt = wallClock.nowMillis()
        )
        return planRepository.savePlan(updatedPlan)
    }
}
