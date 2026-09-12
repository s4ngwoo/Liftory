package com.example.infrastructure.export

import com.example.infrastructure.db.entity.ExerciseSetEntity
import com.example.infrastructure.db.entity.WorkoutSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DataExporterImplTest {

    private lateinit var sessionDao: FakeWorkoutSessionDao
    private lateinit var setDao: FakeExerciseSetDao
    private lateinit var dataExporter: DataExporterImpl

    @Before
    fun setUp() {
        sessionDao = FakeWorkoutSessionDao()
        setDao = FakeExerciseSetDao()
        dataExporter = DataExporterImpl(sessionDao, setDao, Dispatchers.Unconfined)
    }

    @Test
    fun `exportDataAsCsv includes sessionEndTime so restore can keep completion state`() = runTest {
        sessionDao.sessions["s1"] = WorkoutSessionEntity(
            id = "s1",
            startTime = 1000L,
            endTime = 2500L,
            notes = "",
            createdAt = 1000L,
            updatedAt = 2500L
        )
        setDao.sets["set1"] = ExerciseSetEntity(
            id = "set1",
            sessionId = "s1",
            exerciseId = "ex_squat",
            weight = 120.0,
            reps = 5,
            rpe = 8.0,
            restSeconds = null,
            orderIndex = 1,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val result = dataExporter.exportDataAsCsv()

        assertTrue(result.isSuccess)
        val csv = result.getOrThrow()
        val header = csv.lines().first()
        assertEquals("sessionId,sessionStartTime,sessionEndTime,exerciseId,weight,reps,rpe", header)
        assertTrue(csv.contains("s1,1000,2500,ex_squat,120.0,5,8.0"))
    }

    @Test
    fun `exportDataAsCsv leaves endTime empty for an in-progress session`() = runTest {
        sessionDao.sessions["active"] = WorkoutSessionEntity(
            id = "active",
            startTime = 1000L,
            endTime = null,
            notes = "",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        setDao.sets["set1"] = ExerciseSetEntity(
            id = "set1",
            sessionId = "active",
            exerciseId = "ex_bench",
            weight = 80.0,
            reps = 5,
            rpe = null,
            restSeconds = null,
            orderIndex = 1,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val csv = dataExporter.exportDataAsCsv().getOrThrow()
        assertTrue(csv.contains("active,1000,,ex_bench,80.0,5,"))
    }

    @Test
    fun `csv export import round trip keeps finished sessions finished`() = runTest {
        sessionDao.sessions["s1"] = WorkoutSessionEntity(
            id = "s1",
            startTime = 1000L,
            endTime = 2500L,
            notes = "",
            createdAt = 1000L,
            updatedAt = 2500L
        )
        setDao.sets["set1"] = ExerciseSetEntity(
            id = "set1",
            sessionId = "s1",
            exerciseId = "ex_squat",
            weight = 120.0,
            reps = 5,
            rpe = 8.0,
            restSeconds = null,
            orderIndex = 1,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val csv = dataExporter.exportDataAsCsv().getOrThrow()

        val importSessions = FakeWorkoutSessionDao()
        val importSets = FakeExerciseSetDao()
        val importer = DataImporterImpl(importSessions, importSets, Dispatchers.Unconfined)
        val importResult = importer.importDataFromCsv(csv)

        assertTrue(importResult.isSuccess)
        assertEquals(2500L, importSessions.sessions["s1"]?.endTime)
    }
}
