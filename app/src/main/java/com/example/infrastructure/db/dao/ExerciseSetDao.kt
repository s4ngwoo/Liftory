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
        SELECT s.startTime as startTime, SUM(e.weight * e.reps) as totalVolume 
        FROM exercise_sets e 
        INNER JOIN workout_sessions s ON e.sessionId = s.id 
        WHERE s.startTime >= :startDate AND s.startTime <= :endDate 
        GROUP BY s.startTime 
        ORDER BY s.startTime ASC
    """)
    fun observeVolumeByPeriod(startDate: Long, endDate: Long): Flow<List<SessionVolumeTuple>>

    @Query("""
        SELECT e.exerciseId as exerciseId, MAX(e.weight) as maxWeight, MAX(s.startTime) as achievedAt
        FROM exercise_sets e
        INNER JOIN workout_sessions s ON e.sessionId = s.id
        GROUP BY e.exerciseId
    """)
    fun observePersonalRecords(): Flow<List<PersonalRecordTuple>>
}
