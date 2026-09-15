package com.example.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasurementProfileTest {

    @Test
    fun `strength exercise maps weight and reps faithfully`() {
        val exercise = Exercise(
            id = "ex_bench",
            name = "벤치프레스",
            muscleGroup = "Chest",
            equipmentType = EquipmentType.FREE_WEIGHT
        )

        val measurement = LegacyMeasurementMapper.toMeasurement(
            exercise = exercise,
            weight = 80.0,
            reps = 10
        )

        assertTrue(measurement is MeasurementValue.WeightAndReps)
        val strength = measurement as MeasurementValue.WeightAndReps
        assertEquals(80.0, strength.weightKg, 0.001)
        assertEquals(10, strength.reps)
    }

    @Test
    fun `stairmaster cardio maps to TimeAndLevel with seconds duration`() {
        val exercise = Exercise(
            id = "ex_stairmaster",
            name = "StairMaster (천국의 계단 / 스텝밀)",
            muscleGroup = "Cardio",
            equipmentType = EquipmentType.CARDIO
        )

        // Legacy format: weight was Level (8.0), reps was minutes (15)
        val measurement = LegacyMeasurementMapper.toMeasurement(
            exercise = exercise,
            weight = 8.0,
            reps = 15
        )

        assertTrue(measurement is MeasurementValue.TimeAndLevel)
        val cardio = measurement as MeasurementValue.TimeAndLevel
        assertEquals(15 * 60, cardio.durationSeconds)
        assertEquals(8.0, cardio.levelOrSpeed, 0.001)
    }

    @Test
    fun `ambiguous or unknown cardio preserves raw values without guessing units`() {
        // DATA-04: 모호한 레거시 유산소 값 전환 - 원본 보존+LegacyUnknown, 임의 km/h 또는 kg 확정 없음
        val unknownCardio = Exercise(
            id = "custom_unknown_cardio",
            name = "알 수 없는 특수 유산소",
            muscleGroup = "Cardio",
            equipmentType = EquipmentType.CARDIO
        )

        val measurement = LegacyMeasurementMapper.toMeasurement(
            exercise = unknownCardio,
            weight = 12.5,
            reps = 25
        )

        assertTrue(measurement is MeasurementValue.LegacyUnknown)
        val unknown = measurement as MeasurementValue.LegacyUnknown
        assertEquals(12.5, unknown.rawValue1, 0.001)
        assertEquals(25.0, unknown.rawValue2, 0.001)
        assertTrue(unknown.note?.isNotEmpty() == true)
    }

    @Test
    fun `known treadmill cardio maps minutes to TimeAndLevel seconds`() {
        val exercise = Exercise(
            id = "ex_treadmill",
            name = "트레드밀",
            muscleGroup = "Cardio",
            equipmentType = EquipmentType.CARDIO
        )

        val measurement = LegacyMeasurementMapper.toMeasurement(
            exercise = exercise,
            weight = 6.5,
            reps = 20
        )

        val cardio = measurement as MeasurementValue.TimeAndLevel
        assertEquals(20 * 60, cardio.durationSeconds)
        assertEquals(6.5, cardio.levelOrSpeed, 0.001)
    }

    @Test
    fun `stretching exercise maps to TimedHold with side support`() {
        val stretchingExercise = Exercise(
            id = "ex_stretch_hamstring",
            name = "햄스트링 스트레칭",
            muscleGroup = "Legs",
            equipmentType = EquipmentType.FREE_WEIGHT
        )

        val measurementLeft = MeasurementValue.TimedHold(
            durationSeconds = 30,
            side = BodySide.LEFT
        )
        val measurementRight = MeasurementValue.TimedHold(
            durationSeconds = 30,
            side = BodySide.RIGHT
        )

        assertEquals(30, measurementLeft.durationSeconds)
        assertEquals(BodySide.LEFT, measurementLeft.side)
        assertEquals(BodySide.RIGHT, measurementRight.side)
    }
}
