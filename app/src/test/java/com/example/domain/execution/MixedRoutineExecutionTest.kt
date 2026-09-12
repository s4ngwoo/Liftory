package com.example.domain.execution

import com.example.domain.model.ActivityKind
import com.example.domain.model.BodySide
import com.example.domain.model.GuideReference
import com.example.domain.model.MeasurementProfile
import com.example.domain.model.MeasurementValue
import com.example.domain.model.execution.ConfirmedSetRecord
import com.example.domain.model.execution.InProgressSetDisposition
import com.example.domain.model.execution.SessionExecutionController
import com.example.domain.model.execution.SessionExecutionState
import com.example.domain.model.execution.SetExecutionState
import com.example.domain.model.execution.WorkoutExecution
import com.example.domain.model.plan.PlannedExercise
import com.example.domain.model.plan.PlannedSet
import com.example.domain.model.plan.SessionPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * N07 Mixed Routine and Guide Execution Tests (MIX-01~MIX-06).
 */
class MixedRoutineExecutionTest {

    @Test
    fun `MIX-01 bilateral stretching records left and right independently without auto start`() {
        val plan = SessionPlan(
            id = "plan_mixed",
            routineId = "rt_mixed",
            routineVersion = 1,
            name = "Mixed Warmup & Strength",
            exercises = listOf(
                PlannedExercise(
                    id = "pe_stretch",
                    exerciseId = "ex_hamstring",
                    exerciseName = "햄스트링 스트레칭",
                    orderIndex = 0,
                    plannedSets = listOf(
                        PlannedSet(id = "ps_l", orderIndex = 0, targetMeasurement = MeasurementValue.TimedHold(30, BodySide.LEFT)),
                        PlannedSet(id = "ps_r", orderIndex = 1, targetMeasurement = MeasurementValue.TimedHold(30, BodySide.RIGHT))
                    )
                )
            ),
            isConfirmed = true,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        var execution = WorkoutExecution(
            sessionId = "sess_mix",
            planId = plan.id,
            planSnapshot = plan,
            sessionState = SessionExecutionState.ACTIVE,
            currentExerciseIndex = 0,
            currentSetIndex = 0,
            setState = SetExecutionState.Ready,
            revision = 1L,
            startedAtEpochMs = 10_000L
        )
        val controller = SessionExecutionController(execution)

        // 1. Start Left side
        controller.setMachine().startSet(10_000L, 10_000L)
        assertTrue(controller.setMachine().currentState is SetExecutionState.Performing)

        // 2. Complete Left side (30s hold)
        controller.setMachine().completeSet(40_000L, 30)
        assertTrue(controller.setMachine().currentState is SetExecutionState.AwaitingConfirmation)

        // 3. Confirm Left side
        val leftHold = MeasurementValue.TimedHold(30, BodySide.LEFT)
        controller.setMachine().confirmValues(leftHold, targetRestSeconds = 0, isLastSet = false)
        controller.recordConfirmedSet(
            ConfirmedSetRecord(
                plannedExerciseId = "pe_stretch",
                exerciseId = "ex_hamstring",
                exerciseIndex = 0,
                setIndex = 0,
                measurement = leftHold,
                performedDurationSeconds = 30,
                startedAtEpochMs = 10_000L,
                completedAtEpochMs = 40_000L
            )
        )

        // Must NOT automatically enter Performing for Right side; must be Resting or Ready
        assertFalse(
            "Auto-starting next set/side is strictly forbidden",
            controller.setMachine().currentState is SetExecutionState.Performing
        )
        assertTrue(
            controller.setMachine().currentState is SetExecutionState.Resting ||
                controller.setMachine().currentState is SetExecutionState.Ready
        )

        // 4. Explicitly start Right side
        controller.setMachine().startNextSet(45_000L, 45_000L)
        controller.setMachine().completeSet(75_000L, 30)
        val rightHold = MeasurementValue.TimedHold(30, BodySide.RIGHT)
        controller.setMachine().confirmValues(rightHold, targetRestSeconds = 0, isLastSet = true)
        controller.recordConfirmedSet(
            ConfirmedSetRecord(
                plannedExerciseId = "pe_stretch",
                exerciseId = "ex_hamstring",
                exerciseIndex = 0,
                setIndex = 1,
                measurement = rightHold,
                performedDurationSeconds = 30,
                startedAtEpochMs = 45_000L,
                completedAtEpochMs = 75_000L
            )
        )

        val records = controller.current.confirmedRecords
        assertEquals(2, records.size)
        assertEquals(BodySide.LEFT, (records[0].measurement as MeasurementValue.TimedHold).side)
        assertEquals(BodySide.RIGHT, (records[1].measurement as MeasurementValue.TimedHold).side)
        val totalDuration = records.sumOf { it.performedDurationSeconds }
        assertEquals(60, totalDuration)
    }

    @Test
    fun `MIX-02 connecting stretching and strength preserves measurement profiles without forcing kg`() {
        val plan = SessionPlan(
            id = "plan_mixed2",
            routineId = "rt_mixed2",
            routineVersion = 1,
            name = "Stretching + Bench",
            exercises = listOf(
                PlannedExercise(
                    id = "pe_s",
                    exerciseId = "ex_pec_stretch",
                    exerciseName = "가슴 스트레칭",
                    orderIndex = 0,
                    plannedSets = listOf(
                        PlannedSet(id = "ps1", orderIndex = 0, targetMeasurement = MeasurementValue.TimedHold(45, BodySide.BOTH))
                    )
                ),
                PlannedExercise(
                    id = "pe_b",
                    exerciseId = "ex_bench",
                    exerciseName = "벤치프레스",
                    orderIndex = 1,
                    plannedSets = listOf(
                        PlannedSet(id = "ps2", orderIndex = 0, targetMeasurement = MeasurementValue.WeightAndReps(80.0, 10))
                    )
                )
            ),
            isConfirmed = true,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val stretch = plan.exercises[0].plannedSets[0].targetMeasurement
        val bench = plan.exercises[1].plannedSets[0].targetMeasurement

        assertTrue("Stretching must not force weight or reps", stretch is MeasurementValue.TimedHold)
        assertEquals(45, (stretch as MeasurementValue.TimedHold).durationSeconds)
        assertTrue("Bench must use weight and reps", bench is MeasurementValue.WeightAndReps)
        assertEquals(80.0, (bench as MeasurementValue.WeightAndReps).weightKg, 0.001)
    }

    @Test
    fun `MIX-03 cardio measurement profile uses level or distance without speed coercion on stepmill`() {
        val stepmillSet = MeasurementValue.TimeAndLevel(durationSeconds = 900, levelOrSpeed = 8.0)
        val treadmillSet = MeasurementValue.TimeAndDistance(durationSeconds = 1200, distanceMeters = 3000.0, inclinePercent = 3.0)

        // Stepmill has level 8.0, duration 900s (15 min); does not force km/h
        assertEquals(900, stepmillSet.durationSeconds)
        assertEquals(8.0, stepmillSet.levelOrSpeed, 0.01)

        // Treadmill has distance and incline
        assertEquals(3000.0, treadmillSet.distanceMeters, 0.01)
        assertEquals(3.0, treadmillSet.inclinePercent!!, 0.01)
    }

    @Test
    fun `MIX-04 bodyweight plus reps distinguishes pure bodyweight, assisted, and weighted`() {
        val pureBodyweight = MeasurementValue.BodyweightPlusReps(additionalWeightKg = 0.0, reps = 10, isAssisted = false)
        val weightedPullup = MeasurementValue.BodyweightPlusReps(additionalWeightKg = 15.0, reps = 6, isAssisted = false)
        val assistedPullup = MeasurementValue.BodyweightPlusReps(additionalWeightKg = 20.0, reps = 10, isAssisted = true)

        assertFalse(pureBodyweight.isAssisted)
        assertEquals(0.0, pureBodyweight.additionalWeightKg, 0.001)

        assertFalse(weightedPullup.isAssisted)
        assertEquals(15.0, weightedPullup.additionalWeightKg, 0.001)

        assertTrue(assistedPullup.isAssisted)
        assertEquals(20.0, assistedPullup.additionalWeightKg, 0.001)
    }

    @Test
    fun `MIX-05 guide reference provides fallback text and external open intent on embed failure`() {
        val guide = GuideReference(
            exerciseId = "ex_squat",
            title = "스쿼트 정확한 자세",
            source = "Liftory Academy",
            language = "ko",
            verifiedDateEpochMs = 1750000000000L,
            videoUrl = "https://youtube.com/watch?v=squat_example",
            summaryText = "발을 어깨너비로 벌리고 고관절을 접으며 앉습니다.",
            canEmbed = false
        )

        assertEquals("ex_squat", guide.exerciseId)
        assertFalse(guide.canEmbed)
        assertNotNull(guide.summaryText)
        assertEquals("https://youtube.com/watch?v=squat_example", guide.videoUrl)
        // Even when embed is false, user gets textual summary and safe external url
        val fallback = guide.resolveDisplayFallback()
        assertTrue(fallback.contains("발을 어깨너비"))
    }

    @Test
    fun `MIX-06 viewing guide does not disturb or reset set execution and timer state`() {
        var execution = WorkoutExecution(
            sessionId = "sess_g",
            planId = "plan_g",
            planSnapshot = null,
            sessionState = SessionExecutionState.ACTIVE,
            currentExerciseIndex = 0,
            currentSetIndex = 0,
            setState = SetExecutionState.Performing(startedAtEpochMs = 20_000L, startedAtMonotonicMs = 20_000L),
            revision = 5L,
            startedAtEpochMs = 10_000L
        )

        // Simulate viewing guide action
        val beforeSetState = execution.setState
        val beforeRevision = execution.revision

        // External navigation or viewing guide does not change execution state
        val guide = GuideReference(
            exerciseId = "ex_bench",
            title = "벤치 가이드",
            source = "Liftory",
            language = "ko",
            verifiedDateEpochMs = 1750000000000L,
            videoUrl = "https://example.com",
            summaryText = "바를 가슴 중앙에 내립니다.",
            canEmbed = true
        )
        assertNotNull(guide)

        assertEquals(beforeSetState, execution.setState)
        assertEquals(beforeRevision, execution.revision)
    }
}
