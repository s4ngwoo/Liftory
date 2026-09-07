package com.example.infrastructure.export

import com.example.domain.repository.DataExporter
import com.example.infrastructure.db.dao.ExerciseSetDao
import com.example.infrastructure.db.dao.WorkoutSessionDao
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class DataExporterImpl(
    private val sessionDao: WorkoutSessionDao,
    private val setDao: ExerciseSetDao,
    private val ioDispatcher: CoroutineDispatcher
) : DataExporter {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    override suspend fun exportDataAsJson(): Result<String> = withContext(ioDispatcher) {
        try {
            val sessionsEntity = sessionDao.observeAll().first()
            // We need to map entities to domain models or just export entities.
            // Since ExportDataPayload uses Domain models, we should map them or use entities.
            // For simplicity, let's export entities directly to preserve all fields precisely.
            
            val setsEntity = mutableListOf<com.example.infrastructure.db.entity.ExerciseSetEntity>()
            for (session in sessionsEntity) {
                setsEntity.addAll(setDao.getBySession(session.id))
            }
            
            // Actually, we can just use the entities. Let's create an Entity Payload instead.
            val payload = ExportEntityPayload(
                schemaVersion = 1,
                exportedAt = System.currentTimeMillis(),
                sessions = sessionsEntity,
                sets = setsEntity
            )
            
            val adapter = moshi.adapter(ExportEntityPayload::class.java)
            val json = adapter.toJson(payload)
            Result.success(json)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exportDataAsCsv(): Result<String> = withContext(ioDispatcher) {
        try {
            val sessionsEntity = sessionDao.observeAll().first()
            val sb = java.lang.StringBuilder()
            sb.append("sessionId,sessionStartTime,exerciseId,weight,reps,rpe\n")
            
            for (session in sessionsEntity) {
                val sets = setDao.getBySession(session.id)
                for (set in sets) {
                    sb.append("${session.id},${session.startTime},${set.exerciseId},${set.weight},${set.reps},${set.rpe ?: ""}\n")
                }
            }
            Result.success(sb.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
