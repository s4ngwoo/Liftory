package com.example.testfixtures

import com.example.domain.model.Exercise
import com.example.domain.model.ExerciseHistoryRecord
import com.example.domain.model.ExerciseSet
import com.example.domain.model.WorkoutSession
import com.example.domain.port.DispatcherProvider
import com.example.domain.port.IdGenerator
import com.example.domain.port.MonotonicClock
import com.example.domain.port.WallClock
import com.example.domain.repository.ExerciseRepository
import com.example.domain.repository.ExerciseSetRepository
import com.example.domain.repository.TransactionProvider
import com.example.domain.repository.WorkoutSessionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Standard test Fake implementations for Domain interfaces.
 *
 * NOTE ON CONCURRENCY & ATOMICITY (N01.6):
 * [FakeTransactionProvider] is an in-memory test double executing blocks sequentially.
 * It DOES NOT prove database atomic rollback, transaction isolation levels, or concurrent write races.
 * Real DB atomicity must be verified with Room in-memory SQLite integration tests.
 */
class FakeTransactionProvider : TransactionProvider {
    override suspend fun <T> runAsTransaction(block: suspend () -> T): T {
        return block()
    }
}

class FakeWallClock(var currentEpochMillis: Long = 1000000L) : WallClock {
    override fun epochMillis(): Long = currentEpochMillis
    fun advanceByMillis(delta: Long) {
        currentEpochMillis += delta
    }
}

class FakeMonotonicClock(var currentNanos: Long = 0L) : MonotonicClock {
    override fun elapsedRealtimeNanos(): Long = currentNanos
    override fun elapsedRealtimeMillis(): Long = currentNanos / 1_000_000L
    fun advanceByMillis(millis: Long) {
        currentNanos += millis * 1_000_000L
    }
}

class FakeIdGenerator(private var nextId: Int = 1) : IdGenerator {
    override fun generateId(): String = "generated_id_${nextId++}"
}

class TestDispatcherProvider(
    override val main: CoroutineDispatcher = Dispatchers.Unconfined,
    override val io: CoroutineDispatcher = Dispatchers.Unconfined,
    override val default: CoroutineDispatcher = Dispatchers.Unconfined
) : DispatcherProvider
