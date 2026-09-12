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

    override suspend fun importDataFromJson(jsonString: String): Result<Int> = withContext(ioDispatcher) {
        try {
            val adapter = moshi.adapter(ExportEntityPayload::class.java)
            val payload = adapter.fromJson(jsonString)
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid or empty JSON payload"))

            var importedCount = 0
            for (session in payload.sessions) {
                sessionDao.insert(session)
                importedCount++
            }

            for (set in payload.sets) {
                setDao.insert(set)
                importedCount++
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

            // Expected header: sessionId,sessionStartTime[,sessionEndTime],exerciseId,weight,reps,rpe
            val header = lines.first().split(",").map { it.trim() }
            val columns = CsvColumns.fromHeader(header)
            val dataLines = lines.drop(1)

            val sessionMap = mutableMapOf<String, ImportedSessionTimes>()
            val setsToInsert = mutableListOf<ExerciseSetEntity>()

            for (line in dataLines) {
                val tokens = line.split(",").map { it.trim() }
                if (tokens.size <= columns.requiredLastIndex) continue

                val sessionId = tokens[columns.sessionId]
                val startTime = tokens[columns.startTime].toLongOrNull() ?: System.currentTimeMillis()
                val exerciseId = tokens[columns.exerciseId]
                val weight = tokens[columns.weight].toDoubleOrNull() ?: 0.0
                val reps = tokens[columns.reps].toIntOrNull() ?: 0
                val rpe = columns.rpe.takeIf { it >= 0 }?.let { tokens.getOrNull(it)?.toDoubleOrNull() }

                val now = System.currentTimeMillis()
                if (sessionId !in sessionMap) {
                    sessionMap[sessionId] = ImportedSessionTimes(
                        startTime = startTime,
                        endTime = columns.resolveEndTime(tokens, startTime)
                    )
                }
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

            var importedCount = 0
            val now = System.currentTimeMillis()
            for ((sessionId, times) in sessionMap) {
                val existing = sessionDao.getById(sessionId)
                if (existing == null) {
                    sessionDao.insert(
                        WorkoutSessionEntity(
                            id = sessionId,
                            startTime = times.startTime,
                            endTime = times.endTime,
                            notes = "",
                            createdAt = times.startTime,
                            updatedAt = now
                        )
                    )
                }
                importedCount++
            }

            for (set in setsToInsert) {
                setDao.insert(set)
                importedCount++
            }

            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private data class ImportedSessionTimes(
    val startTime: Long,
    val endTime: Long?
)

private data class CsvColumns(
    val sessionId: Int,
    val startTime: Int,
    val endTime: Int,
    val exerciseId: Int,
    val weight: Int,
    val reps: Int,
    val rpe: Int
) {
    val requiredLastIndex: Int = maxOf(sessionId, startTime, exerciseId, weight, reps)

    fun resolveEndTime(tokens: List<String>, startTime: Long): Long? {
        if (endTime < 0) {
            // Legacy CSV has no completion column. Treat imported history as finished
            // so restore cannot revive every workout as the active timer session.
            return startTime
        }
        return tokens.getOrNull(endTime)?.takeIf { it.isNotEmpty() }?.toLongOrNull()
    }

    companion object {
        fun fromHeader(header: List<String>): CsvColumns {
            val sessionId = header.indexOf("sessionId")
            val startTime = header.indexOf("sessionStartTime")
            val exerciseId = header.indexOf("exerciseId")
            val weight = header.indexOf("weight")
            val reps = header.indexOf("reps")
            if (sessionId >= 0 && startTime >= 0 && exerciseId >= 0 && weight >= 0 && reps >= 0) {
                return CsvColumns(
                    sessionId = sessionId,
                    startTime = startTime,
                    endTime = header.indexOf("sessionEndTime"),
                    exerciseId = exerciseId,
                    weight = weight,
                    reps = reps,
                    rpe = header.indexOf("rpe")
                )
            }
            return CsvColumns(
                sessionId = 0,
                startTime = 1,
                endTime = -1,
                exerciseId = 2,
                weight = 3,
                reps = 4,
                rpe = 5
            )
        }
    }
}
