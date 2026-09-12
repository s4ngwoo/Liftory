package com.example.domain.load

import kotlin.math.sqrt

enum class DailyObservationKind {
    OBSERVED_TRAINING,
    CONFIRMED_REST,
    PARTIALLY_OBSERVED,
    MISSING_LOG
}

data class SessionLoadInput(
    val sessionId: String,
    val durationMinutes: Double,
    val sessionRpe: Double?
)

data class DailyLoadSummary(
    val kind: DailyObservationKind,
    val totalObservedLoadAu: Double,
    val isPartiallyObserved: Boolean
)

data class TrendMetrics(
    val mean: Double,
    val standardDeviation: Double,
    val zScore: Double?
)

data class RecoveryMetrics(
    val mean: Double,
    val standardDeviation: Double,
    val zScore: Double?,
    val isInsufficientData: Boolean
)

/**
 * Pure domain calculator for training load (sRPE * duration in AU),
 * wellness recovery scores (Hooper-style 4~28 scale), and EWMA trends (N09).
 */
object TrainingLoadCalculator {

    fun calculateSessionRpeLoad(sessionRpe: Double, durationMinutes: Double): Double {
        return sessionRpe * durationMinutes
    }

    fun aggregateDailyLoad(sessions: List<SessionLoadInput>): DailyLoadSummary {
        if (sessions.isEmpty()) {
            return DailyLoadSummary(DailyObservationKind.MISSING_LOG, 0.0, false)
        }
        var totalLoad = 0.0
        var hasMissingRpe = false
        var hasValidRpe = false

        for (s in sessions) {
            if (s.sessionRpe != null) {
                totalLoad += calculateSessionRpeLoad(s.sessionRpe, s.durationMinutes)
                hasValidRpe = true
            } else {
                hasMissingRpe = true
            }
        }

        val isPartial = hasValidRpe && hasMissingRpe
        val kind = when {
            isPartial -> DailyObservationKind.PARTIALLY_OBSERVED
            hasValidRpe -> DailyObservationKind.OBSERVED_TRAINING
            else -> DailyObservationKind.PARTIALLY_OBSERVED
        }

        return DailyLoadSummary(kind, totalLoad, isPartial)
    }

    fun resolveDailyStatus(hasLog: Boolean, isExplicitRest: Boolean): DailyLoadSummary {
        return when {
            isExplicitRest -> DailyLoadSummary(DailyObservationKind.CONFIRMED_REST, 0.0, false)
            hasLog -> DailyLoadSummary(DailyObservationKind.OBSERVED_TRAINING, 0.0, false)
            else -> DailyLoadSummary(DailyObservationKind.MISSING_LOG, 0.0, false)
        }
    }

    fun calculateEwma(previousEwma: Double, currentLoad: Double, alpha: Double): Double {
        return alpha * currentLoad + (1.0 - alpha) * previousEwma
    }

    fun computeEwmaSeries(history: List<Double>, alpha: Double): List<Double> {
        if (history.isEmpty()) return emptyList()
        val result = ArrayList<Double>(history.size)
        var current = history.first()
        result.add(current)
        for (i in 1 until history.size) {
            current = calculateEwma(current, history[i], alpha)
            result.add(current)
        }
        return result
    }

    fun calculateLoadTrendMetrics(history: List<Double>): TrendMetrics {
        if (history.isEmpty()) return TrendMetrics(0.0, 0.0, null)
        val mean = history.average()
        val variance = history.sumOf { (it - mean) * (it - mean) } / history.size
        val sd = sqrt(variance)
        val z = if (sd > 0.000001) {
            (history.last() - mean) / sd
        } else null
        return TrendMetrics(mean, sd, z)
    }

    /**
     * Recovery score: 4 items (Sleep, Stress, Fatigue, Soreness).
     * Scale: 1 to 7.
     * Aligned positive direction:
     * - sleepQuality: 7 (best) to 1 (worst) -> score = sleepQuality
     * - stress: 1 (low=best) to 7 (severe=worst) -> score = 8 - stress
     * - fatigue: 1 (low=best) to 7 (severe=worst) -> score = 8 - fatigue
     * - muscleSoreness: 1 (low=best) to 7 (severe=worst) -> score = 8 - muscleSoreness
     * Total score range: 4 (worst possible) to 28 (best possible).
     */
    fun calculateRecoveryScore(
        sleepQuality: Int,
        stressLevel: Int,
        fatigueLevel: Int,
        muscleSoreness: Int
    ): Int {
        val s = sleepQuality.coerceIn(1, 7)
        val st = (8 - stressLevel.coerceIn(1, 7))
        val f = (8 - fatigueLevel.coerceIn(1, 7))
        val m = (8 - muscleSoreness.coerceIn(1, 7))
        return s + st + f + m
    }

    fun calculateRecoveryMetrics(scores: List<Int>): RecoveryMetrics {
        if (scores.isEmpty()) {
            return RecoveryMetrics(0.0, 0.0, null, isInsufficientData = true)
        }
        val mean = scores.map { it.toDouble() }.average()
        val variance = scores.sumOf { (it - mean) * (it - mean) } / scores.size
        val sd = sqrt(variance)
        val z = if (sd > 0.000001) {
            (scores.last() - mean) / sd
        } else null
        return RecoveryMetrics(mean, sd, z, isInsufficientData = false)
    }
}
