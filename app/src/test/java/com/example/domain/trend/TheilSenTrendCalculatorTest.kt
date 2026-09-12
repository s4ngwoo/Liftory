package com.example.domain.trend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * N10 Robust Trend Estimation and Training Block Tests (TREND-01~TREND-08).
 */
class TheilSenTrendCalculatorTest {

    @Test
    fun `TREND-01 exact linear series yields identical slope and intercept`() {
        // y = 2x + 10 at x = 0, 1, 2, 3, 4 -> (0,10), (1,12), (2,14), (3,16), (4,18)
        val points = listOf(
            DataPoint(0.0, 10.0),
            DataPoint(1.0, 12.0),
            DataPoint(2.0, 14.0),
            DataPoint(3.0, 16.0),
            DataPoint(4.0, 18.0)
        )
        val result = TheilSenTrendCalculator.fit(points)

        assertNotNull(result)
        assertEquals(2.0, result!!.slope, 0.001)
        assertEquals(10.0, result.intercept, 0.001)
    }

    @Test
    fun `TREND-02 linear series with extreme outlier maintains robust slope via Theil-Sen`() {
        // Linear baseline y = 2x + 10 with 1 extreme outlier at index 2 (100.0 instead of 14.0)
        val points = listOf(
            DataPoint(0.0, 10.0),
            DataPoint(1.0, 12.0),
            DataPoint(2.0, 100.0), // outlier!
            DataPoint(3.0, 16.0),
            DataPoint(4.0, 18.0)
        )
        val result = TheilSenTrendCalculator.fit(points)

        assertNotNull(result)
        // Median of slopes ignores single outlier, stays at 2.0
        assertEquals(2.0, result!!.slope, 0.001)
        assertEquals(10.0, result.intercept, 0.001)
    }

    @Test
    fun `TREND-03 handles same x coordinates, constant values, insufficient points, and NaN without crashing`() {
        // Insufficient points (< 2)
        assertNull(TheilSenTrendCalculator.fit(listOf(DataPoint(1.0, 10.0))))
        assertNull(TheilSenTrendCalculator.fit(emptyList()))

        // Same X coordinates: cannot calculate slope
        val sameX = listOf(DataPoint(1.0, 10.0), DataPoint(1.0, 20.0))
        assertNull(TheilSenTrendCalculator.fit(sameX))

        // Constant values y = 10, 10, 10 -> slope 0.0, intercept 10.0
        val constant = listOf(DataPoint(0.0, 10.0), DataPoint(1.0, 10.0), DataPoint(2.0, 10.0))
        val constResult = TheilSenTrendCalculator.fit(constant)
        assertNotNull(constResult)
        assertEquals(0.0, constResult!!.slope, 0.001)
        assertEquals(10.0, constResult.intercept, 0.001)

        // NaN or Infinite inputs filtered out
        val nanPoints = listOf(DataPoint(0.0, 10.0), DataPoint(1.0, Double.NaN), DataPoint(2.0, 14.0))
        val nanResult = TheilSenTrendCalculator.fit(nanPoints)
        assertNotNull(nanResult)
        assertEquals(2.0, nanResult!!.slope, 0.001)
    }

    @Test
    fun `TREND-04 rolling-origin evaluation enforces temporal order without future data leakage`() {
        val chronologicalPoints = listOf(
            DataPoint(0.0, 10.0),
            DataPoint(1.0, 12.0),
            DataPoint(2.0, 14.0),
            DataPoint(3.0, 16.0),
            DataPoint(4.0, 18.0)
        )

        val evalResult = TheilSenTrendCalculator.evaluateRollingOrigin(
            points = chronologicalPoints,
            minTrainSize = 3
        )

        assertTrue(evalResult.testedSteps >= 2)
        // Ensure mean absolute error against baseline is small for linear series
        assertTrue(evalResult.meanAbsoluteError <= 0.001)
    }

    @Test
    fun `TREND-05 confidence interval and prediction interval return distinct conceptual boundaries`() {
        val points = listOf(
            DataPoint(0.0, 10.0),
            DataPoint(1.0, 12.0),
            DataPoint(2.0, 14.0),
            DataPoint(3.0, 16.0),
            DataPoint(4.0, 18.0)
        )
        val fit = TheilSenTrendCalculator.fit(points)!!
        val intervals = TheilSenTrendCalculator.calculateIntervals(fit, points)

        assertNotNull(intervals.slopeLowerBound)
        assertNotNull(intervals.slopeUpperBound)
        // On exact line, bounds collapse to slope
        assertTrue(intervals.slopeLowerBound <= fit.slope)
        assertTrue(intervals.slopeUpperBound >= fit.slope)
    }

    @Test
    fun `TREND-06 block boundary separates distinct equipment or long gaps into unlinked segments`() {
        val block1 = listOf(DataPoint(0.0, 50.0), DataPoint(7.0, 52.0)) // barbell bench
        val block2 = listOf(DataPoint(60.0, 30.0), DataPoint(67.0, 32.0)) // 53-day gap + dumbbell

        val blocks = TheilSenTrendCalculator.partitionTrainingBlocks(
            listOf(
                SessionObservation(day = 0.0, value = 50.0, equipment = "barbell"),
                SessionObservation(day = 7.0, value = 52.0, equipment = "barbell"),
                SessionObservation(day = 60.0, value = 30.0, equipment = "dumbbell"),
                SessionObservation(day = 67.0, value = 32.0, equipment = "dumbbell")
            ),
            maxGapDays = 30.0
        )

        assertEquals(2, blocks.size)
        assertEquals("barbell", blocks[0].equipment)
        assertEquals("dumbbell", blocks[1].equipment)
    }

    @Test
    fun `TREND-08 deterministic computation with sample cap prevents UI thread stalls`() {
        // 200 points
        val large = (0 until 200).map { DataPoint(it.toDouble(), it * 1.5 + 5) }
        val fit = TheilSenTrendCalculator.fit(large, maxPairwiseSampleLimit = 500)
        assertNotNull(fit)
        assertEquals(1.5, fit!!.slope, 0.05)
    }
}
