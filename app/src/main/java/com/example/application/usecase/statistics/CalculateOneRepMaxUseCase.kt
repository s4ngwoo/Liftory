package com.example.application.usecase.statistics

import kotlin.math.round

class CalculateOneRepMaxUseCase {

    operator fun invoke(weight: Double, reps: Int, rpe: Double? = null): Double {
        if (weight <= 0.0 || reps <= 0) return 0.0

        val rir = if (rpe != null) (10.0 - rpe).coerceAtLeast(0.0) else 0.0
        val effectiveReps = reps + rir

        if (effectiveReps <= 1.0) return weight

        // Epley formula: 1RM = Weight * (1 + EffectiveReps / 30.0)
        val calculated = weight * (1.0 + effectiveReps / 30.0)
        return round(calculated * 10.0) / 10.0
    }
}
