package com.example.infrastructure.export

import com.example.infrastructure.db.dao.ExerciseSetDao
import com.example.infrastructure.db.dao.WorkoutSessionDao
import com.example.infrastructure.db.entity.ExerciseSetEntity
import com.example.infrastructure.db.entity.PersonalRecordTuple
import com.example.infrastructure.db.entity.SessionVolumeTuple
import com.example.infrastructure.db.entity.WorkoutSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeWorkoutSessionDao : WorkoutSessionDao {
    val sessions = mutableMapOf<String, WorkoutSessionEntity>()

    override fun observeAll(): Flow<List<WorkoutSessionEntity>> = flowOf(sessions.values.toList())
    override fun observeActiveSession(): Flow<WorkoutSessionEntity?> = flowOf(sessions.values.find { it.endTime == null })
    override suspend fun getById(id: String): WorkoutSessionEntity? = sessions[id]
    override suspend fun insert(session: WorkoutSessionEntity) {
        sessions[session.id] = session
    }
    override suspend fun update(session: WorkoutSessionEntity) {
        sessions[session.id] = session
    }
    override suspend fun deleteById(id: String) {
        sessions.remove(id)
    }
    override suspend fun getCount(): Int = sessions.size
}

class FakeExerciseSetDao : ExerciseSetDao {
    val sets = mutableMapOf<String, ExerciseSetEntity>()

    override fun observeBySession(sessionId: String): Flow<List<ExerciseSetEntity>> =
        flowOf(sets.values.filter { it.sessionId == sessionId })

    override suspend fun getBySession(sessionId: String): List<ExerciseSetEntity> =
        sets.values.filter { it.sessionId == sessionId }

    override suspend fun getById(id: String): ExerciseSetEntity? = sets[id]
    override suspend fun insert(set: ExerciseSetEntity) {
        sets[set.id] = set
    }
    override suspend fun update(set: ExerciseSetEntity) {
        sets[set.id] = set
    }
    override suspend fun deleteById(id: String) {
        sets.remove(id)
    }
    override suspend fun deleteBySessionId(sessionId: String) {
        sets.values.removeAll { it.sessionId == sessionId }
    }
    override suspend fun getMaxWeightForExercise(exerciseId: String): Double? =
        sets.values.filter { it.exerciseId == exerciseId }.maxOfOrNull { it.weight }

    override suspend fun getVolumeForSessions(sessionIds: List<String>): Double? =
        sets.values.filter { it.sessionId in sessionIds }.sumOf { it.weight * it.reps }

    override fun observeVolumeByPeriod(startDate: Long, endDate: Long): Flow<List<SessionVolumeTuple>> = flowOf(emptyList())
    override fun observePersonalRecords(): Flow<List<PersonalRecordTuple>> = flowOf(emptyList())
    override suspend fun getPastSetsForExercise(exerciseId: String, currentSessionId: String?): List<com.example.infrastructure.db.entity.PastSetTuple> = emptyList()
}

class DataImporterImplTest {

    private lateinit var sessionDao: FakeWorkoutSessionDao
    private lateinit var setDao: FakeExerciseSetDao
    private lateinit var dataImporter: DataImporterImpl

    @Before
    fun setUp() {
        sessionDao = FakeWorkoutSessionDao()
        setDao = FakeExerciseSetDao()
        dataImporter = DataImporterImpl(sessionDao, setDao, Dispatchers.Unconfined)
    }

    @Test
    fun `importDataFromJson correctly inserts sessions and sets into DAOs`() = runTest {
        val json = """
            {
                "schemaVersion": 1,
                "exportedAt": 1700000000000,
                "sessions": [
                    {"id": "session_1", "startTime": 1690000000000, "endTime": 1690003600000, "notes": "Great workout", "createdAt": 1690000000000, "updatedAt": 1690000000000}
                ],
                "sets": [
                    {"id": "set_1", "sessionId": "session_1", "exerciseId": "ex_bench", "weight": 80.0, "reps": 5, "rpe": 8.5, "restSeconds": 90, "orderIndex": 1, "createdAt": 1690000500000, "updatedAt": 1690000500000}
                ]
            }
        """.trimIndent()

        val result = dataImporter.importDataFromJson(json)

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull()) // 1 session + 1 set = 2
        assertEquals(1, sessionDao.sessions.size)
        assertEquals(1, setDao.sets.size)
        assertEquals("Great workout", sessionDao.sessions["session_1"]?.notes)
        assertEquals(80.0, setDao.sets["set_1"]?.weight)
    }

    @Test
    fun `importDataFromCsv correctly parses lines and inserts into DAOs`() = runTest {
        val csv = """
            sessionId,sessionStartTime,exerciseId,weight,reps,rpe
            session_csv_1,1691000000000,ex_squat,120.0,3,9.0
            session_csv_1,1691000000000,ex_squat,120.0,3,9.5
        """.trimIndent()

        val result = dataImporter.importDataFromCsv(csv)

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull()) // 1 session + 2 sets = 3
        assertEquals(1, sessionDao.sessions.size)
        assertEquals(2, setDao.sets.size)
        assertTrue(setDao.sets.values.all { it.sessionId == "session_csv_1" && it.weight == 120.0 })
    }

    @Test
    fun `importDataFromJson fails on malformed json`() = runTest {
        val result = dataImporter.importDataFromJson("{ invalid json }")
        assertTrue(result.isFailure)
    }
}
