package com.example.domain.trend

import kotlin.math.abs

data class DataPoint(val x: Double, val y: Double)

data class TrendFitResult(
    val slope: Double,
    val intercept: Double
)

data class RollingOriginEvalResult(
    val testedSteps: Int,
    val meanAbsoluteError: Double
)

data class TrendIntervals(
    val slopeLowerBound: Double,
    val slopeUpperBound: Double
)

data class SessionObservation(
    val day: Double,
    val value: Double,
    val equipment: String
)

data class TrainingBlock(
    val equipment: String,
    val observations: List<SessionObservation>
)

/**
 * Pure deterministic Theil-Sen estimator for robust trend fitting (N10).
 * Handles extreme outliers, duplicate X coordinates, sample caps, and block segmentation.
 */
object TheilSenTrendCalculator {

    fun fit(
        rawPoints: List<DataPoint>,
        maxPairwiseSampleLimit: Int = 10000
    ): TrendFitResult? {
        val points = rawPoints.filter {
            !it.x.isNaN() && !it.x.isInfinite() && !it.y.isNaN() && !it.y.isInfinite()
        }
        if (points.size < 2) return null

        val slopes = ArrayList<Double>()
        val n = points.size

        // Calculate all pairwise slopes m_ij = (y_j - y_i) / (x_j - x_i) where x_j != x_i
        var count = 0
        outer@ for (i in 0 until n - 1) {
            for (j in i + 1 until n) {
                val dx = points[j].x - points[i].x
                if (abs(dx) > 0.000001) {
                    slopes.add((points[j].y - points[i].y) / dx)
                    count++
                    if (count >= maxPairwiseSampleLimit) break@outer
                }
            }
        }

        if (slopes.isEmpty()) return null

        slopes.sort()
        val medianSlope = median(slopes)

        val intercepts = points.map { it.y - medianSlope * it.x }.sorted()
        val medianIntercept = median(intercepts)

        return TrendFitResult(slope = medianSlope, intercept = medianIntercept)
    }

    fun evaluateRollingOrigin(
        points: List<DataPoint>,
        minTrainSize: Int = 3
    ): RollingOriginEvalResult {
        if (points.size <= minTrainSize) {
            return RollingOriginEvalResult(0, 0.0)
        }
        var totalAbsError = 0.0
        var steps = 0

        for (t in minTrainSize until points.size) {
            val trainSet = points.subList(0, t)
            val testPoint = points[t]
            val fit = fit(trainSet)
            if (fit != null) {
                val predictedY = fit.slope * testPoint.x + fit.intercept
                totalAbsError += abs(predictedY - testPoint.y)
                steps++
            }
        }

        val mae = if (steps > 0) totalAbsError / steps else 0.0
        return RollingOriginEvalResult(testedSteps = steps, meanAbsoluteError = mae)
    }

    fun calculateIntervals(fit: TrendFitResult, points: List<DataPoint>): TrendIntervals {
        // Deterministic bounds around median slope based on IQR / range
        val residuals = points.map { (it.y - (fit.slope * it.x + fit.intercept)) }
        val maxDev = residuals.maxOfOrNull { abs(it) } ?: 0.0
        val margin = (maxDev * 0.1).coerceAtLeast(0.0)
        return TrendIntervals(
            slopeLowerBound = fit.slope - margin,
            slopeUpperBound = fit.slope + margin
        )
    }

    fun partitionTrainingBlocks(
        observations: List<SessionObservation>,
        maxGapDays: Double = 30.0
    ): List<TrainingBlock> {
        if (observations.isEmpty()) return emptyList()
        val blocks = ArrayList<TrainingBlock>()
        var currentEquipment = observations.first().equipment
        var currentList = ArrayList<SessionObservation>()
        var prevDay = observations.first().day

        for (obs in observations) {
            val equipmentChanged = obs.equipment != currentEquipment
            val gapExceeded = (obs.day - prevDay) > maxGapDays

            if (equipmentChanged || gapExceeded) {
                if (currentList.isNotEmpty()) {
                    blocks.add(TrainingBlock(currentEquipment, currentList))
                }
                currentEquipment = obs.equipment
                currentList = ArrayList()
            }
            currentList.add(obs)
            prevDay = obs.day
        }
        if (currentList.isNotEmpty()) {
            blocks.add(TrainingBlock(currentEquipment, currentList))
        }
        return blocks
    }

    private fun median(sortedList: List<Double>): Double {
        val size = sortedList.size
        return if (size % 2 == 1) {
            sortedList[size / 2]
        } else {
            (sortedList[size / 2 - 1] + sortedList[size / 2]) / 2.0
        }
    }
}
