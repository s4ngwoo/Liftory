package com.example.domain.statistics

import com.example.domain.model.BodySide
import com.example.domain.model.MeasurementValue
import com.example.domain.model.execution.ConfirmedSetRecord
import com.example.domain.model.plan.PlannedExercise
import com.example.domain.model.plan.PlannedSet
import com.example.domain.model.plan.SessionPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * N08 Workout Statistics and Plan Comparison Tests (STAT-01~09).
 */
class WorkoutStatisticsTest {

    @Test
    fun `STAT-01 mixed session aggregates 800kg strength, 15min cardio, and 60s stretching separately`() {
        val records = listOf(
            // Strength: 80kg x 10 reps = 800kg
            ConfirmedSetRecord(
                plannedExerciseId = "pe_1",
                exerciseId = "ex_bench",
                exerciseIndex = 0,
                setIndex = 0,
                measurement = MeasurementValue.WeightAndReps(80.0, 10),
                performedDurationSeconds = 35,
                startedAtEpochMs = 10_000L,
                completedAtEpochMs = 45_000L
            ),
            // Cardio: 15 min (900s) on stepmill
            ConfirmedSetRecord(
                plannedExerciseId = "pe_2",
                exerciseId = "ex_stepmill",
                exerciseIndex = 1,
                setIndex = 0,
                measurement = MeasurementValue.TimeAndLevel(900, 8.0),
                performedDurationSeconds = 900,
                startedAtEpochMs = 60_000L,
                completedAtEpochMs = 960_000L
            ),
            // Stretching: 30s Left + 30s Right = 60s
            ConfirmedSetRecord(
                plannedExerciseId = "pe_3",
                exerciseId = "ex_stretch",
                exerciseIndex = 2,
                setIndex = 0,
                measurement = MeasurementValue.TimedHold(30, BodySide.LEFT),
                performedDurationSeconds = 30,
                startedAtEpochMs = 1000_000L,
                completedAtEpochMs = 1030_000L
            ),
            ConfirmedSetRecord(
                plannedExerciseId = "pe_3",
                exerciseId = "ex_stretch",
                exerciseIndex = 2,
                setIndex = 1,
                measurement = MeasurementValue.TimedHold(30, BodySide.RIGHT),
                performedDurationSeconds = 30,
                startedAtEpochMs = 1040_000L,
                completedAtEpochMs = 1070_000L
            )
        )

        val summary = WorkoutStatisticsCalculator.calculateSessionSummary(records)

        assertEquals(800.0, summary.totalStrengthVolumeKg, 0.001)
        assertEquals(900, summary.totalCardioDurationSeconds)
        assertEquals(60, summary.totalStretchingDurationSeconds)
        assertEquals(1, summary.effectiveWorkSetsCount) // 1 strength set
    }

    @Test
    fun `STAT-02 compare plan to actual distinguishes completed, unperformed, and extra sets`() {
        val plan = SessionPlan(
            id = "plan_stat",
            routineId = "rt_stat",
            routineVersion = 1,
            name = "Stat Plan",
            exercises = listOf(
                PlannedExercise(
                    id = "pe_squat",
                    exerciseId = "ex_squat",
                    exerciseName = "스쿼트",
                    orderIndex = 0,
                    plannedSets = listOf(
                        PlannedSet("ps1", 0, MeasurementValue.WeightAndReps(100.0, 10)), // 1000kg planned
                        PlannedSet("ps2", 1, MeasurementValue.WeightAndReps(100.0, 10)), // 1000kg planned
                        PlannedSet("ps3", 2, MeasurementValue.WeightAndReps(100.0, 10))  // 1000kg planned -> total planned 3000kg
                    )
                )
            ),
            isConfirmed = true,
            createdAt = 0L,
            updatedAt = 0L
        )

        val records = listOf(
            ConfirmedSetRecord("pe_squat", "ex_squat", 0, 0, MeasurementValue.WeightAndReps(100.0, 10), 40, 100L, 140L),
            ConfirmedSetRecord("pe_squat", "ex_squat", 0, 1, MeasurementValue.WeightAndReps(108.0, 10), 45, 200L, 245L)
            // 3rd set unperformed! Total actual = 1000 + 1080 = 2080kg
        )

        val comparison = WorkoutStatisticsCalculator.comparePlanToActual(plan, records)

        assertEquals(3000.0, comparison.plannedStrengthVolumeKg, 0.001)
        assertEquals(2080.0, comparison.actualStrengthVolumeKg, 0.001)
        assertEquals(1, comparison.unperformedSetsCount)
        assertEquals(0, comparison.extraSetsCount)
        assertEquals(2080.0 / 3000.0, comparison.achievementRatio!!, 0.001)
    }

    @Test
    fun `STAT-03 only fully confirmed records are included in statistics`() {
        val confirmed = ConfirmedSetRecord("pe_1", "ex_bench", 0, 0, MeasurementValue.WeightAndReps(60.0, 10), 30, 0L, 30L)
        // Only confirmed records list is passed to calculator
        val summary = WorkoutStatisticsCalculator.calculateSessionSummary(listOf(confirmed))
        assertEquals(600.0, summary.totalStrengthVolumeKg, 0.001)
    }

    @Test
    fun `STAT-04 different equipment models for same exercise separate comparison scope`() {
        val recordBarbell = ConfirmedSetRecord("pe_1", "ex_bench", 0, 0, MeasurementValue.WeightAndReps(100.0, 5), 30, 0L, 30L, equipmentModelId = "barbell_eleiko")
        val recordDumbbell = ConfirmedSetRecord("pe_1", "ex_bench", 0, 1, MeasurementValue.WeightAndReps(30.0, 10), 30, 40L, 70L, equipmentModelId = "dumbbell_rogue")

        val grouped = WorkoutStatisticsCalculator.groupByEquipmentScope(listOf(recordBarbell, recordDumbbell))
        assertEquals(2, grouped.size)
        assertTrue(grouped.containsKey("barbell_eleiko"))
        assertTrue(grouped.containsKey("dumbbell_rogue"))
    }

    @Test
    fun `STAT-05 warmup sets are excluded from effective work sets volume`() {
        val warmup = ConfirmedSetRecord(
            plannedExerciseId = "pe_1",
            exerciseId = "ex_deadlift",
            exerciseIndex = 0,
            setIndex = 0,
            measurement = MeasurementValue.WeightAndReps(60.0, 10),
            performedDurationSeconds = 20,
            startedAtEpochMs = 0L,
            completedAtEpochMs = 20L,
            isWarmup = true
        )
        val workSet = ConfirmedSetRecord(
            plannedExerciseId = "pe_1",
            exerciseId = "ex_deadlift",
            exerciseIndex = 0,
            setIndex = 1,
            measurement = MeasurementValue.WeightAndReps(140.0, 5),
            performedDurationSeconds = 25,
            startedAtEpochMs = 60L,
            completedAtEpochMs = 85L,
            isWarmup = false
        )

        val summary = WorkoutStatisticsCalculator.calculateSessionSummary(listOf(warmup, workSet))
        assertEquals(1, summary.effectiveWorkSetsCount)
        assertEquals(700.0, summary.totalStrengthVolumeKg, 0.001) // only 140*5
        assertEquals(600.0, summary.warmupVolumeKg, 0.001)
    }

    @Test
    fun `STAT-06 muscle group contribution weighting allocates fractional sets without inflating total`() {
        // Bench press: Chest (Direct 1.0), Triceps (Indirect 0.5)
        val contributions = mapOf("Chest" to 1.0, "Triceps" to 0.5)
        val result = WorkoutStatisticsCalculator.calculateMuscleGroupVolume(
            contributions = contributions,
            performedWorkSetsCount = 3
        )

        assertEquals(3.0, result["Chest"]!!, 0.001)
        assertEquals(1.5, result["Triceps"]!!, 0.001)
    }

    @Test
    fun `STAT-07 e1RM calculation handles zero, negative, out of range reps and distinguishes null RIR from RIR 0`() {
        // Normal 100kg x 1 rep = 100kg
        assertEquals(100.0, WorkoutStatisticsCalculator.calculateE1RM(100.0, 1)!!, 0.01)

        // Normal 100kg x 10 reps (Epley: 100 * (1 + 10/30) = 133.33)
        assertEquals(133.33, WorkoutStatisticsCalculator.calculateE1RM(100.0, 10)!!, 0.1)

        // RIR = 0 (RPE 10): reps=8, rir=0 -> effective reps = 8
        val e1rmRir0 = WorkoutStatisticsCalculator.calculateE1RM(100.0, 8, rir = 0)
        assertEquals(126.67, e1rmRir0!!, 0.1)

        // RIR = 2 (RPE 8): reps=8, rir=2 -> effective reps = 10
        val e1rmRir2 = WorkoutStatisticsCalculator.calculateE1RM(100.0, 8, rir = 2)
        assertEquals(133.33, e1rmRir2!!, 0.1)

        // Invalid reps > 12 or <= 0
        assertNull(WorkoutStatisticsCalculator.calculateE1RM(100.0, 0))
        assertNull(WorkoutStatisticsCalculator.calculateE1RM(100.0, -1))
        assertNull(WorkoutStatisticsCalculator.calculateE1RM(100.0, 16))

        // Invalid weight <= 0 or NaN
        assertNull(WorkoutStatisticsCalculator.calculateE1RM(0.0, 5))
        assertNull(WorkoutStatisticsCalculator.calculateE1RM(Double.NaN, 5))
    }

    @Test
    fun `STAT-09 empty plan or zero goal yields null achievement ratio without divide by zero`() {
        val plan = SessionPlan(
            id = "plan_zero",
            routineId = "rt_zero",
            routineVersion = 1,
            name = "Zero Goal Plan",
            exercises = emptyList(),
            isConfirmed = true,
            createdAt = 0L,
            updatedAt = 0L
        )

        val comparison = WorkoutStatisticsCalculator.comparePlanToActual(plan, emptyList())
        assertNull(comparison.achievementRatio)
        assertEquals(0.0, comparison.plannedStrengthVolumeKg, 0.001)
        assertEquals(0.0, comparison.actualStrengthVolumeKg, 0.001)
    }
}
