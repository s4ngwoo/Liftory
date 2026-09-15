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

class FakeWorkoutSessionDao(
    private val onReplaceExisting: ((sessionId: String) -> Unit)? = null
) : WorkoutSessionDao {
    val sessions = mutableMapOf<String, WorkoutSessionEntity>()

    override fun observeAll(): Flow<List<WorkoutSessionEntity>> = flowOf(sessions.values.toList())
    override fun observeActiveSession(): Flow<WorkoutSessionEntity?> = flowOf(sessions.values.find { it.endTime == null })
    override suspend fun getActiveSession(): WorkoutSessionEntity? = sessions.values.find { it.endTime == null }
    override suspend fun getActiveSessions(): List<WorkoutSessionEntity> = sessions.values.filter { it.endTime == null }
    override suspend fun getById(id: String): WorkoutSessionEntity? = sessions[id]

    override suspend fun insert(session: WorkoutSessionEntity) {
        val replacing = sessions.containsKey(session.id)
        sessions[session.id] = session
        // Room OnConflictStrategy.REPLACE is DELETE + INSERT, which CASCADE-deletes sets.
        if (replacing) {
            onReplaceExisting?.invoke(session.id)
        }
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
        setDao = FakeExerciseSetDao()
        sessionDao = FakeWorkoutSessionDao { sessionId -> setDao.deleteBySessionId(sessionId) }
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

    @Test
    fun `importDataFromJson rejects unknown future schema version without modifying database (DATA-07)`() = runTest {
        val futureJson = """
            {
                "schemaVersion": 99,
                "exportedAt": 1700000000000,
                "sessions": [
                    {"id": "session_future", "startTime": 1690000000000, "createdAt": 1690000000000, "updatedAt": 1690000000000}
                ],
                "sets": []
            }
        """.trimIndent()

        val result = dataImporter.importDataFromJson(futureJson)

        assertTrue("Expected failure for unsupported schema version", result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(result.exceptionOrNull()?.message?.contains("Unsupported backup schema version: 99") == true)
        assertEquals("Database should remain unmodified", 0, sessionDao.sessions.size)
    }

    @Test
    fun `importDataFromJson rejects orphan sets missing valid session foreign key (DATA-08)`() = runTest {
        val orphanJson = """
            {
                "schemaVersion": 1,
                "exportedAt": 1700000000000,
                "sessions": [
                    {"id": "session_valid", "startTime": 1690000000000, "createdAt": 1690000000000, "updatedAt": 1690000000000}
                ],
                "sets": [
                    {"id": "set_orphan", "sessionId": "session_non_existent", "exerciseId": "ex_bench", "weight": 80.0, "reps": 5, "orderIndex": 1, "createdAt": 1690000500000, "updatedAt": 1690000500000}
                ]
            }
        """.trimIndent()

        val result = dataImporter.importDataFromJson(orphanJson)

        assertTrue("Expected failure due to invalid foreign key reference", result.isFailure)
        assertEquals("No sessions should be committed when validation fails", 0, sessionDao.sessions.size)
        assertEquals("No sets should be committed when validation fails", 0, setDao.sets.size)
    }

    @Test
    fun `export and import round-trip preserves all sessions and sets semantic equality (DATA-06)`() = runTest {
        val exporter = DataExporterImpl(sessionDao, setDao, Dispatchers.Unconfined)

        val session = WorkoutSessionEntity(
            id = "session_roundtrip",
            startTime = 1700000000000,
            endTime = 1700003600000,
            notes = "Roundtrip note",
            createdAt = 1700000000000,
            updatedAt = 1700003600000
        )
        val set1 = ExerciseSetEntity(
            id = "set_rt_1",
            sessionId = "session_roundtrip",
            exerciseId = "ex_bench",
            weight = 100.0,
            reps = 5,
            rpe = 9.0,
            restSeconds = 120,
            orderIndex = 1,
            isCompleted = true,
            targetReps = 5,
            createdAt = 1700000500000,
            updatedAt = 1700000500000
        )
        sessionDao.insert(session)
        setDao.insert(set1)

        val exportResult = exporter.exportDataAsJson()
        assertTrue(exportResult.isSuccess)
        val exportedJson = exportResult.getOrThrow()

        // Create fresh target DAOs
        val targetSessionDao = FakeWorkoutSessionDao()
        val targetSetDao = FakeExerciseSetDao()
        val targetImporter = DataImporterImpl(targetSessionDao, targetSetDao, Dispatchers.Unconfined)

        val importResult = targetImporter.importDataFromJson(exportedJson)
        assertTrue(importResult.isSuccess)
        assertEquals(2, importResult.getOrNull())

        assertEquals(1, targetSessionDao.sessions.size)
        assertEquals(1, targetSetDao.sets.size)
        assertEquals(session, targetSessionDao.sessions["session_roundtrip"])
        assertEquals(set1, targetSetDao.sets["set_rt_1"])
    }

    @Test
    fun `importDataFromJson skips existing sessions so local sets are not cascade-deleted`() = runTest {
        val localSession = WorkoutSessionEntity(
            id = "session_1",
            startTime = 1690000000000,
            endTime = 1690003600000,
            notes = "local notes",
            createdAt = 1690000000000,
            updatedAt = 1690003600000
        )
        val backupSet = ExerciseSetEntity(
            id = "set_1",
            sessionId = "session_1",
            exerciseId = "ex_bench",
            weight = 80.0,
            reps = 5,
            rpe = 8.0,
            restSeconds = 90,
            orderIndex = 1,
            createdAt = 1690000500000,
            updatedAt = 1690000500000
        )
        val localOnlySet = ExerciseSetEntity(
            id = "set_local",
            sessionId = "session_1",
            exerciseId = "ex_bench",
            weight = 85.0,
            reps = 5,
            rpe = 9.0,
            restSeconds = 90,
            orderIndex = 2,
            createdAt = 1690000800000,
            updatedAt = 1690000800000
        )
        sessionDao.insert(localSession)
        setDao.insert(backupSet.copy(weight = 82.5, reps = 6))
        setDao.insert(localOnlySet)

        val json = """
            {
                "schemaVersion": 1,
                "exportedAt": 1700000000000,
                "sessions": [
                    {"id": "session_1", "startTime": 1690000000000, "endTime": null, "notes": "stale backup", "createdAt": 1690000000000, "updatedAt": 1690000000000}
                ],
                "sets": [
                    {"id": "set_1", "sessionId": "session_1", "exerciseId": "ex_bench", "weight": 80.0, "reps": 5, "rpe": 8.5, "restSeconds": 90, "orderIndex": 1, "createdAt": 1690000500000, "updatedAt": 1690000500000}
                ]
            }
        """.trimIndent()

        val result = dataImporter.importDataFromJson(json)

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
        assertEquals("local notes", sessionDao.sessions["session_1"]?.notes)
        assertEquals(1690003600000, sessionDao.sessions["session_1"]?.endTime)
        assertEquals(2, setDao.sets.size)
        assertEquals(82.5, setDao.sets["set_1"]?.weight)
        assertEquals(6, setDao.sets["set_1"]?.reps)
        assertEquals(85.0, setDao.sets["set_local"]?.weight)
    }

    @Test
    fun `importDataFromJson merges new sessions and sets without touching existing rows`() = runTest {
        sessionDao.insert(
            WorkoutSessionEntity(
                id = "session_existing",
                startTime = 1690000000000,
                endTime = 1690003600000,
                notes = "keep me",
                createdAt = 1690000000000,
                updatedAt = 1690003600000
            )
        )
        setDao.insert(
            ExerciseSetEntity(
                id = "set_existing",
                sessionId = "session_existing",
                exerciseId = "ex_squat",
                weight = 120.0,
                reps = 3,
                rpe = 9.0,
                restSeconds = 120,
                orderIndex = 1,
                createdAt = 1690000500000,
                updatedAt = 1690000500000
            )
        )

        val json = """
            {
                "schemaVersion": 1,
                "exportedAt": 1700000000000,
                "sessions": [
                    {"id": "session_existing", "startTime": 1690000000000, "endTime": null, "notes": "overwrite", "createdAt": 1690000000000, "updatedAt": 1690000000000},
                    {"id": "session_new", "startTime": 1691000000000, "endTime": 1691003600000, "notes": "imported", "createdAt": 1691000000000, "updatedAt": 1691003600000}
                ],
                "sets": [
                    {"id": "set_existing", "sessionId": "session_existing", "exerciseId": "ex_squat", "weight": 1.0, "reps": 1, "orderIndex": 1, "createdAt": 1690000500000, "updatedAt": 1690000500000},
                    {"id": "set_new", "sessionId": "session_new", "exerciseId": "ex_bench", "weight": 80.0, "reps": 5, "orderIndex": 1, "createdAt": 1691000500000, "updatedAt": 1691000500000}
                ]
            }
        """.trimIndent()

        val result = dataImporter.importDataFromJson(json)

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())
        assertEquals("keep me", sessionDao.sessions["session_existing"]?.notes)
        assertEquals("imported", sessionDao.sessions["session_new"]?.notes)
        assertEquals(120.0, setDao.sets["set_existing"]?.weight)
        assertEquals(80.0, setDao.sets["set_new"]?.weight)
    }

    @Test
    fun `importDataFromCsv does not duplicate sets when the session already exists`() = runTest {
        sessionDao.insert(
            WorkoutSessionEntity(
                id = "session_csv_1",
                startTime = 1691000000000,
                endTime = 1691003600000,
                notes = "already on device",
                createdAt = 1691000000000,
                updatedAt = 1691003600000
            )
        )
        setDao.insert(
            ExerciseSetEntity(
                id = "set_original",
                sessionId = "session_csv_1",
                exerciseId = "ex_squat",
                weight = 120.0,
                reps = 3,
                rpe = 9.0,
                restSeconds = null,
                orderIndex = 1,
                createdAt = 1691000000000,
                updatedAt = 1691000000000
            )
        )

        val csv = """
            sessionId,sessionStartTime,exerciseId,weight,reps,rpe
            session_csv_1,1691000000000,ex_squat,120.0,3,9.0
            session_csv_1,1691000000000,ex_squat,120.0,3,9.5
        """.trimIndent()

        val result = dataImporter.importDataFromCsv(csv)

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull())
        assertEquals(1, setDao.sets.size)
        assertEquals("set_original", setDao.sets.values.single().id)
        assertEquals(1691003600000, sessionDao.sessions["session_csv_1"]?.endTime)
    }
}
