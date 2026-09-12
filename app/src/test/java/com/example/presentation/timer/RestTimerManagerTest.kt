package com.example.presentation.timer

import com.example.domain.model.timer.RestTarget
import com.example.domain.port.MonotonicClock
import com.example.domain.port.NotificationScheduler
import com.example.domain.port.NoOpNotificationScheduler
import com.example.domain.port.WallClock
import com.example.testfixtures.FakeMonotonicClock
import com.example.testfixtures.FakeWallClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RestTimerManagerTest {

    @Test
    fun `startTimer initializes from RestTarget and refresh after missed ticks recovers remaining`() = runTest {
        val wall = FakeWallClock(0L)
        val mono = FakeMonotonicClock(0L)
        val manager = manager(wall, mono, this)

        manager.startTimer(90)
        assertEquals(90, manager.timerState.value.remainingSeconds)
        assertTrue(manager.timerState.value.isRunning)
        assertNotNull(manager.activeRestTarget)

        // TIME-02: skip 60s of UI ticks; recompute from anchors.
        wall.advanceByMillis(60_000L)
        mono.advanceByMillis(60_000L)
        manager.refresh()

        assertEquals(30, manager.timerState.value.remainingSeconds)
        assertEquals(0, manager.timerState.value.overtimeSeconds)
        assertTrue(manager.timerState.value.isRunning)
    }

    @Test
    fun `stopTimer clears state immediately`() = runTest {
        val wall = FakeWallClock(0L)
        val manager = manager(wall, FakeMonotonicClock(0L), this)
        manager.startTimer(60)
        manager.stopTimer()
        assertEquals(0, manager.timerState.value.remainingSeconds)
        assertFalse(manager.timerState.value.isRunning)
        assertNull(manager.activeRestTarget)
    }

    @Test
    fun `TIME-05 plus 30 and pause do not invent a next set`() = runTest {
        val wall = FakeWallClock(40_000L)
        val mono = FakeMonotonicClock(40_000L * 1_000_000L)
        val manager = manager(wall, mono, this)

        manager.restoreFrom(
            RestTarget(
                startedAtEpochMs = 40_000L,
                startedAtMonotonicMs = 40_000L,
                targetSeconds = 90
            )
        )
        manager.addSeconds(30)
        assertEquals(120, manager.activeRestTarget?.targetSeconds)

        wall.advanceByMillis(10_000L)
        mono.advanceByMillis(10_000L)
        manager.pauseTimer()
        wall.advanceByMillis(20_000L)
        mono.advanceByMillis(20_000L)
        manager.resumeTimer()
        wall.currentEpochMillis = 100_000L
        mono.currentNanos = 100_000L * 1_000_000L
        manager.refresh()

        assertEquals(80, manager.timerState.value.remainingSeconds)
        assertTrue(manager.timerState.value.isRunning)
    }

    @Test
    fun `TIME-07 overtime shows excess without stopping or auto-starting`() = runTest {
        val wall = FakeWallClock(40_000L)
        val manager = manager(wall, FakeMonotonicClock(40_000L * 1_000_000L), this)
        manager.restoreFrom(
            RestTarget(
                startedAtEpochMs = 40_000L,
                startedAtMonotonicMs = 40_000L,
                targetSeconds = 90
            )
        )
        wall.currentEpochMillis = 138_000L
        manager.refresh()

        assertEquals(0, manager.timerState.value.remainingSeconds)
        assertEquals(8, manager.timerState.value.overtimeSeconds)
        assertTrue(manager.timerState.value.isRunning)
        assertNotNull(manager.activeRestTarget)
    }

    @Test
    fun `TIME-08 notification failure does not clear local rest state`() = runTest {
        val wall = FakeWallClock(0L)
        val failing = object : NotificationScheduler by NoOpNotificationScheduler {
            override fun notifyRestActive(
                sessionId: String,
                remainingSeconds: Int,
                targetSeconds: Int
            ): Result<Unit> = Result.failure(SecurityException("denied"))

            override fun clearRestNotification(): Result<Unit> =
                Result.failure(IllegalStateException("service failed"))
        }
        val manager = RestTimerManager(
            wallClock = wall,
            monotonicClock = FakeMonotonicClock(0L),
            notificationScheduler = failing,
            scope = backgroundScope
        )

        manager.startTimer(90)
        assertEquals(90, manager.timerState.value.remainingSeconds)
        assertTrue(manager.timerState.value.isRunning)
        assertNotNull(manager.lastNotificationFailure())

        manager.stopTimer()
        assertFalse(manager.timerState.value.isRunning)
        assertNull(manager.activeRestTarget)
    }

    @Test
    fun `restoreFrom does not duplicate RestTarget anchors`() = runTest {
        val target = RestTarget(
            startedAtEpochMs = 40_000L,
            startedAtMonotonicMs = 40_000L,
            targetSeconds = 90,
            bootId = "boot-1"
        )
        val wall = FakeWallClock(100_000L)
        val manager = manager(wall, FakeMonotonicClock(100_000L * 1_000_000L), this)
        manager.restoreFrom(target)
        manager.restoreFrom(target)

        assertEquals(40_000L, manager.activeRestTarget?.startedAtEpochMs)
        assertEquals(30, manager.timerState.value.remainingSeconds)
    }

    private fun manager(
        wall: FakeWallClock,
        mono: FakeMonotonicClock,
        scope: TestScope
    ): RestTimerManager = RestTimerManager(
        wallClock = wall,
        monotonicClock = mono,
        notificationScheduler = NoOpNotificationScheduler,
        // backgroundScope: infinite tick loop must not block runTest completion
        scope = scope.backgroundScope
    )
}
