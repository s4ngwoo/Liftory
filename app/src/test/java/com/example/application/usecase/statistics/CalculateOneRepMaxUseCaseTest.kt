package com.example.application.usecase.statistics

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CalculateOneRepMaxUseCaseTest {

    private lateinit var calculateOneRepMaxUseCase: CalculateOneRepMaxUseCase

    @Before
    fun setUp() {
        calculateOneRepMaxUseCase = CalculateOneRepMaxUseCase()
    }

    @Test
    fun `1 rep at RPE 10 returns exact weight`() {
        val oneRm = calculateOneRepMaxUseCase(weight = 100.0, reps = 1, rpe = 10.0)
        assertEquals(100.0, oneRm, 0.1)
    }

    @Test
    fun `5 reps without RPE uses standard Epley formula`() {
        // 100 * (1 + 5 / 30.0) = 116.666...
        val oneRm = calculateOneRepMaxUseCase(weight = 100.0, reps = 5, rpe = null)
        assertEquals(116.7, oneRm, 0.1)
    }

    @Test
    fun `5 reps with RPE 8 accounts for 2 reps in reserve`() {
        // Effective reps = 5 + (10 - 8) = 7
        // 100 * (1 + 7 / 30.0) = 123.333...
        val oneRm = calculateOneRepMaxUseCase(weight = 100.0, reps = 5, rpe = 8.0)
        assertEquals(123.3, oneRm, 0.1)
    }

    @Test
    fun `invalid inputs return zero`() {
        assertEquals(0.0, calculateOneRepMaxUseCase(weight = 0.0, reps = 5), 0.0)
        assertEquals(0.0, calculateOneRepMaxUseCase(weight = 100.0, reps = 0), 0.0)
        assertEquals(0.0, calculateOneRepMaxUseCase(weight = -50.0, reps = 5), 0.0)
    }
}
