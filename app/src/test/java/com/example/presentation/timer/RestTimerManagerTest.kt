package com.example.presentation.timer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RestTimerManagerTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startTimer should initialize state with seconds and decrement every second`() = runTest {
        val timerManager = RestTimerManager()
        timerManager.startTimer(3)

        assertEquals(3, timerManager.timerState.value.remainingSeconds)
        assertTrue(timerManager.timerState.value.isRunning)

        testDispatcher.scheduler.advanceTimeBy(1001L)
        assertEquals(2, timerManager.timerState.value.remainingSeconds)

        testDispatcher.scheduler.advanceTimeBy(1001L)
        assertEquals(1, timerManager.timerState.value.remainingSeconds)

        testDispatcher.scheduler.advanceTimeBy(1001L)
        assertEquals(0, timerManager.timerState.value.remainingSeconds)
        assertFalse(timerManager.timerState.value.isRunning)
    }

    @Test
    fun `stopTimer should immediately cancel and reset state`() = runTest {
        val timerManager = RestTimerManager()
        timerManager.startTimer(60)

        assertEquals(60, timerManager.timerState.value.remainingSeconds)
        assertTrue(timerManager.timerState.value.isRunning)

        timerManager.stopTimer()
        assertEquals(0, timerManager.timerState.value.remainingSeconds)
        assertFalse(timerManager.timerState.value.isRunning)
    }
}
