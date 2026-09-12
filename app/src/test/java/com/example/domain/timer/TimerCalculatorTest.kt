package com.example.domain.timer

import com.example.domain.model.timer.PauseInterval
import com.example.domain.model.timer.RestTarget
import com.example.domain.model.timer.TimerCalculator
import com.example.domain.model.timer.TimerConfidence
import com.example.testfixtures.SyntheticFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerCalculatorTest {

    private val fTime = SyntheticFixtures.fTimeSequence

    @Test
    fun `TIME-01 F-TIME performing confirm and next start match hand calculation`() {
        val performed = TimerCalculator.performedSeconds(
            startedAtEpochMs = fTime.setStartSec * 1000,
            completedAtEpochMs = fTime.setCompleteSec * 1000
        )
        assertEquals(30, performed)

        val rest = RestTarget(
            startedAtEpochMs = fTime.setCompleteSec * 1000,
            startedAtMonotonicMs = fTime.setCompleteSec * 1000,
            targetSeconds = fTime.restTargetSec.toInt()
        )

        // At confirmation (50s): rest remaining = 90 - 10 = 80
        val remainingAtConfirm = TimerCalculator.restRemainingSeconds(rest, nowEpochMs = fTime.setConfirmSec * 1000)
        assertEquals(80, remainingAtConfirm)

        // Next set at 100s: actual rest 60s, remaining 30s
        val remainingAtNext = TimerCalculator.restRemainingSeconds(rest, nowEpochMs = fTime.nextSetStartSec * 1000)
        assertEquals(fTime.expectedRemainingAtNextStart.toInt(), remainingAtNext)
        assertEquals(
            fTime.expectedActualRestSec.toInt(),
            TimerCalculator.restElapsedSeconds(rest, nowEpochMs = fTime.nextSetStartSec * 1000)
        )
    }

    @Test
    fun `TIME-02 after 60s missed ticks snapshot recovers from stored intervals`() {
        val rest = RestTarget(
            startedAtEpochMs = 40_000L,
            startedAtMonotonicMs = 40_000L,
            targetSeconds = 90
        )
        val snapshot = TimerCalculator.snapshotAfterMissedTicks(
            performedStartEpochMs = 10_000L,
            performedEndEpochMs = 40_000L,
            restTarget = rest,
            sessionStartEpochMs = 0L,
            sessionPauses = emptyList(),
            nowEpochMs = 100_000L,
            bootIdMatches = true
        )
        assertEquals(30, snapshot.performedSeconds)
        assertEquals(60, snapshot.restElapsedSeconds)
        assertEquals(30, snapshot.restRemainingSeconds)
        assertEquals(100, snapshot.sessionElapsedSeconds)
        assertTrue(snapshot.confidence is TimerConfidence.Trusted)
    }

    @Test
    fun `TIME-03 F-RESTART at 100s keeps remaining 30s without auto starting next set`() {
        val rest = RestTarget(
            startedAtEpochMs = 40_000L,
            startedAtMonotonicMs = 40_000L,
            targetSeconds = 90,
            bootId = "boot-1"
        )
        val snapshot = TimerCalculator.snapshotAfterMissedTicks(
            performedStartEpochMs = 10_000L,
            performedEndEpochMs = 40_000L,
            restTarget = rest,
            sessionStartEpochMs = 0L,
            sessionPauses = emptyList(),
            nowEpochMs = 100_000L,
            bootIdMatches = true
        )
        assertEquals(30, snapshot.restRemainingSeconds)
        assertEquals(0, snapshot.restOvertimeSeconds)
        // Recovery must not invent a next-set start; callers decide.
        assertEquals(30, snapshot.performedSeconds)
    }

    @Test
    fun `TIME-04 boot mismatch marks open intervals as estimated and never negative`() {
        val rest = RestTarget(
            startedAtEpochMs = 40_000L,
            startedAtMonotonicMs = 40_000L,
            targetSeconds = 90,
            bootId = "boot-old"
        )
        val snapshot = TimerCalculator.snapshotAfterMissedTicks(
            performedStartEpochMs = 10_000L,
            performedEndEpochMs = 40_000L,
            restTarget = rest,
            sessionStartEpochMs = 0L,
            sessionPauses = emptyList(),
            nowEpochMs = 20_000L, // wall jumped backward relative to open rest
            bootIdMatches = false
        )
        assertTrue(snapshot.confidence is TimerConfidence.Estimated)
        assertTrue(snapshot.restRemainingSeconds >= 0)
        assertTrue(snapshot.sessionElapsedSeconds >= 0)
    }

    @Test
    fun `TIME-05 plus 30s and rest pause do not affect performed duration`() {
        var rest = RestTarget(
            startedAtEpochMs = 40_000L,
            startedAtMonotonicMs = 40_000L,
            targetSeconds = 90
        )
        rest = TimerCalculator.extendRestTarget(rest, extraSeconds = 30)
        assertEquals(120, rest.targetSeconds)

        rest = TimerCalculator.beginRestPause(rest, pauseStartedAtEpochMs = 50_000L, pauseStartedAtMonotonicMs = 50_000L)
        rest = TimerCalculator.endRestPause(rest, pauseEndedAtEpochMs = 70_000L, pauseEndedAtMonotonicMs = 70_000L)

        // At 100s wall: raw rest elapsed 60s minus 20s pause = 40s; remaining 120-40=80
        assertEquals(40, TimerCalculator.restElapsedSeconds(rest, nowEpochMs = 100_000L))
        assertEquals(80, TimerCalculator.restRemainingSeconds(rest, nowEpochMs = 100_000L))

        val performed = TimerCalculator.performedSeconds(10_000L, 40_000L)
        assertEquals(30, performed)
    }

    @Test
    fun `TIME-06 session pause is tracked separately from wall elapsed`() {
        val pauses = listOf(
            PauseInterval(
                startedAtEpochMs = 20_000L,
                startedAtMonotonicMs = 20_000L,
                endedAtEpochMs = 40_000L,
                endedAtMonotonicMs = 40_000L
            )
        )
        val (wallElapsed, pauseTotal) = TimerCalculator.sessionElapsedSeconds(
            sessionStartedAtEpochMs = 0L,
            nowEpochMs = 100_000L,
            sessionPauses = pauses,
            excludePausesFromElapsed = false
        )
        assertEquals(100, wallElapsed)
        assertEquals(20, pauseTotal)

        val (activeElapsed, _) = TimerCalculator.sessionElapsedSeconds(
            sessionStartedAtEpochMs = 0L,
            nowEpochMs = 100_000L,
            sessionPauses = pauses,
            excludePausesFromElapsed = true
        )
        assertEquals(80, activeElapsed)
    }

    @Test
    fun `TIME-07 overtime after rest target does not auto-start next set`() {
        val rest = RestTarget(
            startedAtEpochMs = 40_000L,
            startedAtMonotonicMs = 40_000L,
            targetSeconds = 90
        )
        // Target ends at 130s; at 138s overtime is 8s
        assertEquals(0, TimerCalculator.restRemainingSeconds(rest, nowEpochMs = 138_000L))
        assertEquals(8, TimerCalculator.restOvertimeSeconds(rest, nowEpochMs = 138_000L))
    }
}
