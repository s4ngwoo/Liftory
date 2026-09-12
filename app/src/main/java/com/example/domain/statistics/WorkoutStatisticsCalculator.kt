package com.example.domain.statistics

import com.example.domain.model.BodySide
import com.example.domain.model.MeasurementValue
import com.example.domain.model.execution.ConfirmedSetRecord
import com.example.domain.model.plan.SessionPlan

data class WorkoutSessionSummary(
    val totalStrengthVolumeKg: Double,
    val warmupVolumeKg: Double,
    val effectiveWorkSetsCount: Int,
    val totalCardioDurationSeconds: Int,
    val totalCardioDistanceMeters: Double,
    val totalStretchingDurationSeconds: Int,
    val totalSetsCount: Int
)

data class PlanComparisonResult(
    val plannedStrengthVolumeKg: Double,
    val actualStrengthVolumeKg: Double,
    val plannedSetsCount: Int,
    val actualSetsCount: Int,
    val unperformedSetsCount: Int,
    val extraSetsCount: Int,
    val achievementRatio: Double?
)

/**
 * Pure domain calculator for workout statistics and plan comparisons (N08).
 * Enforces metric separation, e1RM validation rules, and divide-by-zero protection.
 */
object WorkoutStatisticsCalculator {

    fun calculateSessionSummary(records: List<ConfirmedSetRecord>): WorkoutSessionSummary {
        val validRecords = records.filter { it.isIncludedInStatistics }

        var strengthVol = 0.0
        var warmupVol = 0.0
        var effectiveWorkSets = 0
        var cardioDuration = 0
        var cardioDistance = 0.0
        var stretchingDuration = 0

        for (record in validRecords) {
            when (val m = record.measurement) {
                is MeasurementValue.WeightAndReps -> {
                    val vol = m.weightKg * m.reps
                    if (record.isWarmup) {
                        warmupVol += vol
                    } else {
                        strengthVol += vol
                        effectiveWorkSets++
                    }
                }
                is MeasurementValue.BodyweightPlusReps -> {
                    // Only non-assisted additional weight counts towards external volume
                    if (!m.isAssisted && m.additionalWeightKg > 0.0) {
                        val vol = m.additionalWeightKg * m.reps
                        if (record.isWarmup) warmupVol += vol else strengthVol += vol
                    }
                    if (!record.isWarmup) effectiveWorkSets++
                }
                is MeasurementValue.TimeAndLevel -> {
                    cardioDuration += m.durationSeconds
                }
                is MeasurementValue.TimeAndDistance -> {
                    cardioDuration += m.durationSeconds
                    cardioDistance += m.distanceMeters
                }
                is MeasurementValue.TimedHold -> {
                    stretchingDuration += m.durationSeconds
                }
                is MeasurementValue.LegacyUnknown -> Unit
            }
        }

        return WorkoutSessionSummary(
            totalStrengthVolumeKg = strengthVol,
            warmupVolumeKg = warmupVol,
            effectiveWorkSetsCount = effectiveWorkSets,
            totalCardioDurationSeconds = cardioDuration,
            totalCardioDistanceMeters = cardioDistance,
            totalStretchingDurationSeconds = stretchingDuration,
            totalSetsCount = validRecords.size
        )
    }

    fun comparePlanToActual(
        plan: SessionPlan,
        records: List<ConfirmedSetRecord>
    ): PlanComparisonResult {
        val validRecords = records.filter { it.isIncludedInStatistics }

        var plannedVolume = 0.0
        var plannedSetsTotal = 0
        for (exercise in plan.exercises) {
            for (set in exercise.plannedSets) {
                plannedSetsTotal++
                when (val target = set.targetMeasurement) {
                    is MeasurementValue.WeightAndReps -> plannedVolume += target.weightKg * target.reps
                    is MeasurementValue.BodyweightPlusReps -> {
                        if (!target.isAssisted && target.additionalWeightKg > 0.0) {
                            plannedVolume += target.additionalWeightKg * target.reps
                        }
                    }
                    else -> Unit
                }
            }
        }

        var actualVolume = 0.0
        var actualSetsTotal = 0
        for (record in validRecords) {
            actualSetsTotal++
            when (val m = record.measurement) {
                is MeasurementValue.WeightAndReps -> if (!record.isWarmup) actualVolume += m.weightKg * m.reps
                is MeasurementValue.BodyweightPlusReps -> {
                    if (!m.isAssisted && m.additionalWeightKg > 0.0 && !record.isWarmup) {
                        actualVolume += m.additionalWeightKg * m.reps
                    }
                }
                else -> Unit
            }
        }

        val unperformed = (plannedSetsTotal - actualSetsTotal).coerceAtLeast(0)
        val extra = (actualSetsTotal - plannedSetsTotal).coerceAtLeast(0)

        val ratio = if (plannedVolume > 0.0) {
            actualVolume / plannedVolume
        } else null

        return PlanComparisonResult(
            plannedStrengthVolumeKg = plannedVolume,
            actualStrengthVolumeKg = actualVolume,
            plannedSetsCount = plannedSetsTotal,
            actualSetsCount = actualSetsTotal,
            unperformedSetsCount = unperformed,
            extraSetsCount = extra,
            achievementRatio = ratio
        )
    }

    fun groupByEquipmentScope(records: List<ConfirmedSetRecord>): Map<String?, List<ConfirmedSetRecord>> {
        return records.groupBy { it.equipmentModelId }
    }

    fun calculateMuscleGroupVolume(
        contributions: Map<String, Double>,
        performedWorkSetsCount: Int
    ): Map<String, Double> {
        return contributions.mapValues { (_, weight) ->
            weight * performedWorkSetsCount
        }
    }

    /**
     * Estimates 1RM using Epley formula with optional RIR adjustment.
     * Valid range: 1 <= reps <= 12, weightKg > 0.0.
     * Returns null for invalid values or out of bound reps.
     */
    fun calculateE1RM(weightKg: Double, reps: Int, rir: Int? = null): Double? {
        if (weightKg.isNaN() || weightKg <= 0.0) return null
        val effectiveReps = reps + (rir ?: 0)
        if (reps <= 0 || effectiveReps <= 0 || effectiveReps > 12) return null
        if (effectiveReps == 1) return weightKg
        return weightKg * (1.0 + effectiveReps / 30.0)
    }
}
