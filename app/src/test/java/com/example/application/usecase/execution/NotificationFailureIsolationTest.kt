package com.example.application.usecase.execution

import com.example.domain.port.NotificationScheduler
import com.example.domain.port.NoOpNotificationScheduler
import com.example.testfixtures.FakeWallClock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TIME-08 / N05.6: notification permission or service failure must not fail local session records.
 */
class NotificationFailureIsolationTest {

    @Test
    fun `starting session notification failure leaves local execution intact`() = runTest {
        val wall = FakeWallClock(10_000L)
        val planRepo = com.example.application.usecase.plan.FakePlanRepository()
        val sessionRepo = FakeSessionRepoForExecution()
        val executionRepo = FakeExecutionRepository()
        val plan = SessionPlanFixture.confirmed()
        planRepo.plans[plan.id] = plan

        val failingNotifications = object : NotificationScheduler by NoOpNotificationScheduler {
            override fun startWorkoutOngoing(
                sessionId: String,
                title: String,
                startTimeEpochMs: Long
            ): Result<Unit> = Result.failure(SecurityException("POST_NOTIFICATIONS denied"))
        }

        val startSession = StartSessionUseCase(
            planRepository = planRepo,
            sessionRepository = sessionRepo,
            executionRepository = executionRepo,
            transactionProvider = com.example.testfixtures.FakeTransactionProvider(),
            idGenerator = com.example.testfixtures.FakeIdGenerator(),
            wallClock = wall
        )

        val execution = startSession(plan.id).getOrThrow()
        val notifyResult = failingNotifications.startWorkoutOngoing(
            sessionId = execution.sessionId,
            title = plan.name,
            startTimeEpochMs = execution.startedAtEpochMs ?: wall.nowMillis()
        )

        assertTrue(notifyResult.isFailure)
        assertEquals(execution.sessionId, executionRepo.getBySessionId(execution.sessionId)?.sessionId)
        assertTrue(sessionRepo.getById(execution.sessionId)?.endTime == null)
    }
}

private object SessionPlanFixture {
    fun confirmed() = com.example.domain.model.plan.SessionPlan(
        id = "plan_notify",
        routineId = "rt",
        routineVersion = 1,
        name = "Push",
        exercises = emptyList(),
        isConfirmed = true,
        createdAt = 1L,
        updatedAt = 1L
    )
}
