package com.example.domain.load

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * N09 Training Load and Recovery Condition Tests (LOAD-01~LOAD-07, LOAD-09).
 */
class TrainingLoadCalculatorTest {

    @Test
    fun `LOAD-01 session-RPE 6 with 60min duration yields 360 AU independent of kg volume`() {
        val loadAu = TrainingLoadCalculator.calculateSessionRpeLoad(sessionRpe = 6.0, durationMinutes = 60.0)
        assertEquals(360.0, loadAu, 0.001)
    }

    @Test
    fun `LOAD-02 multiple sessions on same day with partial sRPE marks daily status as partially observed`() {
        val session1 = SessionLoadInput(sessionId = "s1", durationMinutes = 45.0, sessionRpe = 7.0) // 315 AU
        val session2 = SessionLoadInput(sessionId = "s2", durationMinutes = 30.0, sessionRpe = null) // missing RPE

        val dailyStatus = TrainingLoadCalculator.aggregateDailyLoad(listOf(session1, session2))
        assertEquals(315.0, dailyStatus.totalObservedLoadAu, 0.001)
        assertTrue(dailyStatus.isPartiallyObserved)
        assertEquals(DailyObservationKind.PARTIALLY_OBSERVED, dailyStatus.kind)
    }

    @Test
    fun `LOAD-03 missing log day is distinguished from zero load confirmed rest`() {
        val restDay = TrainingLoadCalculator.resolveDailyStatus(hasLog = true, isExplicitRest = true)
        val missingDay = TrainingLoadCalculator.resolveDailyStatus(hasLog = false, isExplicitRest = false)

        assertEquals(DailyObservationKind.CONFIRMED_REST, restDay.kind)
        assertEquals(DailyObservationKind.MISSING_LOG, missingDay.kind)
        assertFalse("Missing log must never be treated as confirmed rest", missingDay.kind == DailyObservationKind.CONFIRMED_REST)
    }

    @Test
    fun `LOAD-04 EWMA calculates correct decaying load with alpha 0_25`() {
        // prev 100, current load 0 -> next 75 (alpha * 0 + (1 - alpha) * 100)
        val next = TrainingLoadCalculator.calculateEwma(previousEwma = 100.0, currentLoad = 0.0, alpha = 0.25)
        assertEquals(75.0, next, 0.001)
    }

    @Test
    fun `LOAD-05 constant load across window with SD 0 returns null monotonicity without NaN or danger warning`() {
        val constantHistory = listOf(300.0, 300.0, 300.0, 300.0, 300.0, 300.0, 300.0)
        val result = TrainingLoadCalculator.calculateLoadTrendMetrics(constantHistory)

        assertEquals(0.0, result.standardDeviation, 0.001)
        assertNull("Monotonicity or z-score with SD 0 must be null to prevent division by zero", result.zScore)
        assertFalse(result.standardDeviation.isNaN())
    }

    @Test
    fun `LOAD-06 wellness recovery score aligns positive direction where 28 is best and 4 is worst`() {
        // sleep 7(best), stress 1(low=best), fatigue 1(low=best), doms 1(low=best) -> 7 + 7 + 7 + 7 = 28
        val bestRecovery = TrainingLoadCalculator.calculateRecoveryScore(
            sleepQuality = 7,
            stressLevel = 1,
            fatigueLevel = 1,
            muscleSoreness = 1
        )
        assertEquals(28, bestRecovery)

        // sleep 1(worst), stress 7(severe=worst), fatigue 7(severe=worst), doms 7(severe=worst) -> 1 + 1 + 1 + 1 = 4
        val worstRecovery = TrainingLoadCalculator.calculateRecoveryScore(
            sleepQuality = 1,
            stressLevel = 7,
            fatigueLevel = 7,
            muscleSoreness = 7
        )
        assertEquals(4, worstRecovery)
    }

    @Test
    fun `LOAD-07 identical recovery scores or insufficient data separates inability to compute z-score from missing data`() {
        val identicalScores = listOf(20, 20, 20)
        val metrics = TrainingLoadCalculator.calculateRecoveryMetrics(identicalScores)

        assertEquals(0.0, metrics.standardDeviation, 0.001)
        assertNull(metrics.zScore)
        assertFalse(metrics.isInsufficientData)

        val emptyScores = emptyList<Int>()
        val emptyMetrics = TrainingLoadCalculator.calculateRecoveryMetrics(emptyScores)
        assertTrue(emptyMetrics.isInsufficientData)
        assertNull(emptyMetrics.zScore)
    }

    @Test
    fun `LOAD-09 modifying past RPE recomputes downstream EWMA window without exposing stale results`() {
        val initialHistory = listOf(100.0, 100.0, 100.0)
        val alpha = 0.2
        val initialEwma = TrainingLoadCalculator.computeEwmaSeries(initialHistory, alpha)

        // Modify 2nd day from 100 to 200
        val updatedHistory = listOf(100.0, 200.0, 100.0)
        val recomputedEwma = TrainingLoadCalculator.computeEwmaSeries(updatedHistory, alpha)

        assertEquals(initialEwma[0], recomputedEwma[0], 0.001)
        assertTrue("Recomputed 2nd day EWMA must reflect updated RPE", recomputedEwma[1] > initialEwma[1])
        assertTrue("Recomputed 3rd day EWMA must reflect ripple effect of 2nd day", recomputedEwma[2] > initialEwma[2])
    }
}
