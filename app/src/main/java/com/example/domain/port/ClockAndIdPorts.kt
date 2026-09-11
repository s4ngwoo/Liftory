package com.example.domain.port

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * WallClock port providing epoch timestamps (milliseconds since Unix epoch).
 * Used for user-visible dates, calendar queries, and audit timestamps.
 */
interface WallClock {
    fun nowMillis(): Long

    object System : WallClock {
        override fun nowMillis(): Long = java.lang.System.currentTimeMillis()
    }
}

/**
 * MonotonicClock port providing elapsed time unaffected by system wall-clock changes or timezones.
 * Used for stopwatch elapsed time and active duration calculations.
 */
interface MonotonicClock {
    fun elapsedRealtimeMillis(): Long
}

/**
 * IdGenerator port for creating unique identifiers.
 */
interface IdGenerator {
    fun generate(): String

    object Uuid : IdGenerator {
        override fun generate(): String = java.util.UUID.randomUUID().toString()
    }
}

/**
 * DispatcherProvider port for injecting coroutine dispatchers into UseCases and ViewModels.
 */
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher

    object Default : DispatcherProvider {
        override val main: CoroutineDispatcher = Dispatchers.Main
        override val io: CoroutineDispatcher = Dispatchers.IO
        override val default: CoroutineDispatcher = Dispatchers.Default
    }
}
