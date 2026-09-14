package com.example.infrastructure.repository

import androidx.room.Room
import com.example.domain.exception.ActiveSessionAlreadyExistsException
import com.example.domain.model.WorkoutSession
import com.example.infrastructure.db.StrengthLogDatabase
import com.example.infrastructure.db.entity.WorkoutSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WorkoutSessionRepositoryImplTest {

    private lateinit var db: StrengthLogDatabase
    private lateinit var repository: WorkoutSessionRepositoryImpl

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, StrengthLogDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        repository = WorkoutSessionRepositoryImpl(db, Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `getActiveSession does not fabricate endTime on extra active rows`() = runTest {
        val older = WorkoutSessionEntity(
            id = "sess_older",
            startTime = 1_000L,
            endTime = null,
            notes = "Imported history",
            createdAt = 1_000L,
            updatedAt = 1_000L
        )
        val newer = WorkoutSessionEntity(
            id = "sess_newer",
            startTime = 2_000L,
            endTime = null,
            notes = "Current",
            createdAt = 2_000L,
            updatedAt = 2_000L
        )
        db.workoutSessionDao().insert(older)
        db.workoutSessionDao().insert(newer)

        val active = repository.getActiveSession()

        assertEquals("sess_newer", active?.id)
        assertNull(db.workoutSessionDao().getById("sess_older")?.endTime)
        assertNull(db.workoutSessionDao().getById("sess_newer")?.endTime)
        assertEquals(2, repository.getActiveSessions().size)
    }

    @Test
    fun `create refuses a second active session inside the write transaction`() = runTest {
        val first = WorkoutSession(
            id = "sess_first",
            startTime = 1_000L,
            notes = "First"
        )
        assertTrue(repository.create(first).isSuccess)

        val second = WorkoutSession(
            id = "sess_second",
            startTime = 2_000L,
            notes = "Second"
        )
        val result = repository.create(second)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ActiveSessionAlreadyExistsException)
        assertNull(db.workoutSessionDao().getById("sess_second"))
        assertEquals(1, repository.getActiveSessions().size)
    }
}
