package com.example.domain.model

/**
 * Type-safe, lossless measurement value representation for performed or planned sets.
 */
sealed interface MeasurementValue {

    data class WeightAndReps(
        val weightKg: Double,
        val reps: Int
    ) : MeasurementValue

    data class BodyweightPlusReps(
        val additionalWeightKg: Double = 0.0,
        val reps: Int,
        val isAssisted: Boolean = false
    ) : MeasurementValue

    data class TimeAndLevel(
        val durationSeconds: Int,
        val levelOrSpeed: Double
    ) : MeasurementValue

    data class TimeAndDistance(
        val durationSeconds: Int,
        val distanceMeters: Double,
        val inclinePercent: Double? = null
    ) : MeasurementValue

    data class TimedHold(
        val durationSeconds: Int,
        val side: BodySide = BodySide.NONE
    ) : MeasurementValue

    data class LegacyUnknown(
        val rawValue1: Double,
        val rawValue2: Double,
        val note: String? = null
    ) : MeasurementValue
}
