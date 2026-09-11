package com.example.domain.model

/**
 * Lossless mapper for interpreting legacy weight/reps columns from v1/v2/v3 database schemas.
 * Adheres to DATA-04: preserves raw values in [MeasurementValue.LegacyUnknown] when equipment semantics are ambiguous.
 */
object LegacyMeasurementMapper {

    private val KNOWN_CARDIO_IDS = setOf(
        "ex_treadmill",
        "ex_stairmaster",
        "ex_cycle",
        "ex_incline_treadmill",
        "ex_elliptical",
        "ex_rowing_machine"
    )

    fun toMeasurement(exercise: Exercise, weight: Double, reps: Int): MeasurementValue {
        val isCardio = exercise.isCardio

        return if (isCardio) {
            if (KNOWN_CARDIO_IDS.contains(exercise.id)) {
                // Known cardio format: weight is speed/level, reps is minutes
                MeasurementValue.TimeAndLevel(
                    durationSeconds = reps * 60,
                    levelOrSpeed = weight
                )
            } else {
                // Ambiguous or custom cardio: preserve raw values in LegacyUnknown without guessing units
                MeasurementValue.LegacyUnknown(
                    rawValue1 = weight,
                    rawValue2 = reps.toDouble(),
                    note = "Legacy uncalibrated cardio data for ${exercise.name}"
                )
            }
        } else {
            // Standard strength exercise
            MeasurementValue.WeightAndReps(
                weightKg = weight,
                reps = reps
            )
        }
    }
}
