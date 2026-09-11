package com.example.application.usecase.plan

import com.example.domain.model.plan.SessionPlan
import com.example.domain.port.WallClock
import com.example.domain.repository.SessionPlanRepository

class ConfirmPlanUseCase(
    private val planRepository: SessionPlanRepository,
    private val wallClock: WallClock
) {
    suspend operator fun invoke(planId: String): Result<SessionPlan> {
        val plan = planRepository.getPlanById(planId)
            ?: return Result.failure(IllegalArgumentException("Plan not found: $planId"))

        val confirmedPlan = plan.copy(
            isConfirmed = true,
            updatedAt = wallClock.nowMillis()
        )

        return planRepository.savePlan(confirmedPlan)
    }
}
