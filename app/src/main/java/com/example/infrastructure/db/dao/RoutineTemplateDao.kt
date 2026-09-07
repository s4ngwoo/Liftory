package com.example.infrastructure.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.infrastructure.db.entity.ExercisePresetEntity
import com.example.infrastructure.db.entity.RoutineTemplateEntity
import com.example.infrastructure.db.entity.RoutineTemplateWithPresets
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineTemplateDao {
    @Transaction
    @Query("SELECT * FROM routine_templates ORDER BY name ASC")
    fun observeAll(): Flow<List<RoutineTemplateWithPresets>>

    @Transaction
    @Query("SELECT * FROM routine_templates WHERE id = :id")
    suspend fun getById(id: String): RoutineTemplateWithPresets?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: RoutineTemplateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresets(presets: List<ExercisePresetEntity>)

    @Update
    suspend fun updateTemplate(template: RoutineTemplateEntity)

    @Query("DELETE FROM exercise_presets WHERE templateId = :templateId")
    suspend fun deletePresetsByTemplateId(templateId: String)

    @Query("DELETE FROM routine_templates WHERE id = :id")
    suspend fun deleteTemplateById(id: String)
}
