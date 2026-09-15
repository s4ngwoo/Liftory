package com.example.infrastructure.export

import com.example.domain.repository.DataImporter
import com.example.infrastructure.db.dao.ExerciseSetDao
import com.example.infrastructure.db.dao.WorkoutSessionDao
import com.example.infrastructure.db.entity.ExerciseSetEntity
import com.example.infrastructure.db.entity.WorkoutSessionEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.UUID

class DataImporterImpl(
    private val sessionDao: WorkoutSessionDao,
    private val setDao: ExerciseSetDao,
    private val ioDispatcher: CoroutineDispatcher
) : DataImporter {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    companion object {
        const val MAX_SUPPORTED_SCHEMA_VERSION = ExportEntityPayload.CURRENT_SCHEMA_VERSION
    }

    override suspend fun importDataFromJson(jsonString: String): Result<Int> = withContext(ioDispatcher) {
        try {
            // 1. Version gate: inspect top-level schemaVersion before full deserialization (N02.6, DATA-07)
            val mapAdapter = moshi.adapter(Map::class.java)
            val rawMap = try {
                mapAdapter.fromJson(jsonString) as? Map<*, *>
            } catch (e: Exception) {
                return@withContext Result.failure(IllegalArgumentException("Malformed JSON: ${e.message}", e))
            } ?: return@withContext Result.failure(IllegalArgumentException("Invalid or empty JSON payload"))

            val rawVersion = (rawMap["schemaVersion"] as? Number)?.toInt() ?: 1
            if (rawVersion > MAX_SUPPORTED_SCHEMA_VERSION || rawVersion < 1) {
                return@withContext Result.failure(
                    IllegalArgumentException("Unsupported backup schema version: $rawVersion. Supported versions: 1..$MAX_SUPPORTED_SCHEMA_VERSION")
                )
            }

            val adapter = moshi.adapter(ExportEntityPayload::class.java)
            val payload = adapter.fromJson(jsonString)
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid or empty JSON payload"))

            // 2. Pre-validation of foreign keys: all sets must reference an existing or payload session (N02.7, DATA-08)
            val payloadSessionIds = payload.sessions.map { it.id }.toSet()
            for (set in payload.sets) {
                if (!payloadSessionIds.contains(set.sessionId)) {
                    val existingSession = sessionDao.getById(set.sessionId)
                    if (existingSession == null) {
                        return@withContext Result.failure(
                            IllegalArgumentException("Foreign key violation: set '${set.id}' references non-existent session '${set.sessionId}'")
                        )
                    }
                }
            }

            // 3. Merge inserts: skip rows that already exist.
            // WorkoutSessionDao.insert uses OnConflictStrategy.REPLACE, which SQLite
            // implements as DELETE + INSERT. That fires ON DELETE CASCADE on
            // exercise_sets and would wipe local sets (including ones not in the file)
            // if we replaced an existing session. Set REPLACE would also overwrite
            // locally edited weight/reps with stale backup values.
            var importedCount = 0
            for (session in payload.sessions) {
                if (sessionDao.getById(session.id) == null) {
                    sessionDao.insert(session)
                    importedCount++
                }
            }

            for (set in payload.sets) {
                if (setDao.getById(set.id) == null) {
                    setDao.insert(set)
                    importedCount++
                }
            }

            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun importDataFromCsv(csvString: String): Result<Int> = withContext(ioDispatcher) {
        try {
            val lines = csvString.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Empty CSV content"))
            }

            // Expected header: sessionId,sessionStartTime,exerciseId,weight,reps,rpe
            val header = lines.first().split(",").map { it.trim() }
            val dataLines = lines.drop(1)

            val sessionMap = mutableMapOf<String, Long>()
            val setsToInsert = mutableListOf<ExerciseSetEntity>()

            for (line in dataLines) {
                val tokens = line.split(",").map { it.trim() }
                if (tokens.size >= 5) {
                    val sessionId = tokens[0]
                    val startTime = tokens[1].toLongOrNull() ?: System.currentTimeMillis()
                    val exerciseId = tokens[2]
                    val weight = tokens[3].toDoubleOrNull() ?: 0.0
                    val reps = tokens[4].toIntOrNull() ?: 0
                    val rpe = if (tokens.size > 5) tokens[5].toDoubleOrNull() else null

                    val now = System.currentTimeMillis()
                    sessionMap[sessionId] = startTime
                    setsToInsert.add(
                        ExerciseSetEntity(
                            id = UUID.randomUUID().toString(),
                            sessionId = sessionId,
                            exerciseId = exerciseId,
                            weight = weight,
                            reps = reps,
                            rpe = rpe,
                            restSeconds = null,
                            orderIndex = setsToInsert.size + 1,
                            createdAt = startTime,
                            updatedAt = now
                        )
                    )
                }
            }

            var importedCount = 0
            val now = System.currentTimeMillis()
            val newSessionIds = mutableSetOf<String>()
            for ((sessionId, startTime) in sessionMap) {
                val existing = sessionDao.getById(sessionId)
                if (existing == null) {
                    sessionDao.insert(
                        WorkoutSessionEntity(
                            id = sessionId,
                            startTime = startTime,
                            endTime = null,
                            notes = "",
                            createdAt = startTime,
                            updatedAt = now
                        )
                    )
                    newSessionIds.add(sessionId)
                    importedCount++
                }
            }

            // CSV rows have no stable set ids, so each restore mints new UUIDs.
            // Re-importing into a session that already exists would duplicate every
            // performed set and inflate volume/PRs. Only attach CSV sets to sessions
            // created by this restore.
            for (set in setsToInsert) {
                if (set.sessionId in newSessionIds) {
                    setDao.insert(set)
                    importedCount++
                }
            }

            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
