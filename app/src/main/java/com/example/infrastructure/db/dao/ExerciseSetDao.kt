package com.example.infrastructure.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.infrastructure.db.entity.ExerciseSetEntity
import kotlinx.coroutines.flow.Flow
import com.example.infrastructure.db.entity.SessionVolumeTuple
import com.example.infrastructure.db.entity.PersonalRecordTuple

@Dao
interface ExerciseSetDao {
    @Query("SELECT * FROM exercise_sets WHERE sessionId = :sessionId ORDER BY orderIndex ASC, createdAt ASC")
    fun observeBySession(sessionId: String): Flow<List<ExerciseSetEntity>>

    @Query("SELECT * FROM exercise_sets WHERE sessionId = :sessionId ORDER BY orderIndex ASC, createdAt ASC")
    suspend fun getBySession(sessionId: String): List<ExerciseSetEntity>

    @Query("SELECT * FROM exercise_sets WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ExerciseSetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(set: ExerciseSetEntity)

    @Update
    suspend fun update(set: ExerciseSetEntity)

    @Query("DELETE FROM exercise_sets WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM exercise_sets WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: String)

    @Query("SELECT MAX(weight) FROM exercise_sets WHERE exerciseId = :exerciseId")
    suspend fun getMaxWeightForExercise(exerciseId: String): Double?

    // [Sprint 2 Extension Point]: This query is prepared for volume calculations in the Statistics sprint.
    // It groups by sessionIds or can be adjusted to group by date/exerciseId for charts.
    @Query("SELECT SUM(weight * reps) FROM exercise_sets WHERE sessionId IN (:sessionIds)")
    suspend fun getVolumeForSessions(sessionIds: List<String>): Double?

    @Query("""
        SELECT s.startTime as startTime, 
               COALESCE(SUM(CASE WHEN ex.equipmentType != 'CARDIO' OR ex.equipmentType IS NULL THEN e.weight * e.reps ELSE 0 END), 0.0) as totalVolume,
               COALESCE(SUM(CASE WHEN ex.equipmentType = 'CARDIO' THEN CAST(e.reps AS INTEGER) ELSE 0 END), 0) as cardioMinutes
        FROM exercise_sets e 
        INNER JOIN workout_sessions s ON e.sessionId = s.id 
        LEFT JOIN exercises ex ON e.exerciseId = ex.id
        WHERE s.startTime >= :startDate AND s.startTime <= :endDate 
        GROUP BY s.startTime 
        ORDER BY s.startTime ASC
    """)
    fun observeVolumeByPeriod(startDate: Long, endDate: Long): Flow<List<SessionVolumeTuple>>

    @Query("""
        SELECT e.exerciseId as exerciseId, 
               COALESCE(ex.name, e.exerciseId) as exerciseName,
               COALESCE(ex.equipmentType, 'FREE_WEIGHT') as equipmentType,
               MAX(CASE WHEN ex.equipmentType != 'CARDIO' OR ex.equipmentType IS NULL THEN e.weight ELSE 0.0 END) as maxWeight,
               MAX(CASE WHEN ex.equipmentType = 'CARDIO' THEN e.weight ELSE NULL END) as maxCardioLevel,
               MAX(CASE WHEN ex.equipmentType = 'CARDIO' THEN CAST(e.reps AS INTEGER) ELSE NULL END) as maxCardioMinutes,
               MAX(s.startTime) as achievedAt
        FROM exercise_sets e
        INNER JOIN workout_sessions s ON e.sessionId = s.id
        LEFT JOIN exercises ex ON e.exerciseId = ex.id
        GROUP BY e.exerciseId, ex.name, ex.equipmentType
    """)
    fun observePersonalRecords(): Flow<List<PersonalRecordTuple>>

    @Query("""
        SELECT s.startTime as sessionDate, s.id as sessionId, e.id as id, e.exerciseId as exerciseId, e.weight as weight, e.reps as reps, e.rpe as rpe, e.orderIndex as orderIndex
        FROM exercise_sets e
        INNER JOIN workout_sessions s ON e.sessionId = s.id
        WHERE e.exerciseId = :exerciseId AND (:currentSessionId IS NULL OR s.id != :currentSessionId)
        ORDER BY s.startTime DESC, e.orderIndex ASC
    """)
    suspend fun getPastSetsForExercise(exerciseId: String, currentSessionId: String? = null): List<com.example.infrastructure.db.entity.PastSetTuple>
}
