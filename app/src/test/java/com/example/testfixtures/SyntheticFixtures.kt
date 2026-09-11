package com.example.testfixtures

import com.example.domain.model.EquipmentType
import com.example.domain.model.Exercise
import com.example.domain.model.ExerciseSet
import com.example.domain.model.WorkoutSession

/**
 * Common Synthetic Fixtures defined in development-tdd-test-catalog.md.
 * These fixtures provide isolated, deterministic test data for TDD cycles.
 */
object SyntheticFixtures {

    /**
     * F-MIXED:
     * - Stretching: Left 30s + Right 30s (total 60s)
     * - Bench Press (Strength): 80kg × 10 reps (800 kg volume)
     * - StairMaster (Cardio): Level 8, 15 minutes (15 min cardio)
     *
     * Invariants:
     * - Strength volume: 800.0 kg
     * - Cardio duration: 15 minutes
     * - Stretching duration: 60 seconds
     * - Total volume MUST NOT sum cardio to 920 kg!
     */
    val mixedSessionId = "session_f_mixed"

    val mixedBenchPress = Exercise(
        id = "ex_bench_f_mixed",
        name = "벤치프레스",
        muscleGroup = "CHEST",
        equipmentType = EquipmentType.FREE_WEIGHT
    )

    val mixedStairMaster = Exercise(
        id = "ex_stairmaster_f_mixed",
        name = "천국의 계단 (스텝밀)",
        muscleGroup = "CARDIO",
        equipmentType = EquipmentType.CARDIO
    )

    val mixedStretching = Exercise(
        id = "ex_stretch_f_mixed",
        name = "흉근 스트레칭",
        muscleGroup = "CHEST",
        equipmentType = EquipmentType.FREE_WEIGHT
    )

    val mixedSession = WorkoutSession(
        id = mixedSessionId,
        startTime = 1000L,
        endTime = 4600L,
        notes = "F-MIXED Synthetic Session"
    )

    val mixedSets = listOf(
        // Bench Press: 80kg x 10 reps
        ExerciseSet(
            id = "set_bench_1",
            sessionId = mixedSessionId,
            exerciseId = mixedBenchPress.id,
            weight = 80.0,
            reps = 10,
            orderIndex = 0,
            isCompleted = true
        ),
        // StairMaster: Level 8, 15 minutes
        ExerciseSet(
            id = "set_stairmaster_1",
            sessionId = mixedSessionId,
            exerciseId = mixedStairMaster.id,
            weight = 8.0, // level 8
            reps = 15,    // 15 minutes
            orderIndex = 1,
            isCompleted = true
        )
    )

    /**
     * F-TIME:
     * - Entire session starts at 0s
     * - First set starts at 10s
     * - First set completes at 40s (duration = 30s)
     * - Set values confirmed at 50s (input duration = 10s, excluded from work)
     * - Rest target = 90s (target elapsed from completion 40s is 40s + 90s = 130s)
     * - Next set starts at 100s (actual rest taken = 60s, remaining rest when starting next was 30s)
     */
    data class TimeSequence(
        val sessionStartSec: Long = 0L,
        val setStartSec: Long = 10L,
        val setCompleteSec: Long = 40L,
        val setConfirmSec: Long = 50L,
        val restTargetSec: Long = 90L,
        val nextSetStartSec: Long = 100L
    ) {
        val expectedPerformingSec: Long = setCompleteSec - setStartSec // 30s
        val expectedRestTargetEndSec: Long = setCompleteSec + restTargetSec // 130s
        val expectedActualRestSec: Long = nextSetStartSec - setCompleteSec // 60s
        val expectedRemainingAtNextStart: Long = expectedRestTargetEndSec - nextSetStartSec // 30s
    }

    val fTimeSequence = TimeSequence()
}
