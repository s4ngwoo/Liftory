package com.example.infrastructure.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.infrastructure.db.entity.PendingUploadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingUploadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pendingUpload: PendingUploadEntity)

    @Query("DELETE FROM pending_uploads WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun deleteByEntity(entityType: String, entityId: String)

    @Transaction
    suspend fun replaceForEntity(pendingUpload: PendingUploadEntity) {
        deleteByEntity(pendingUpload.entityType, pendingUpload.entityId)
        insert(pendingUpload)
    }

    @Query("SELECT * FROM pending_uploads ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getNextPending(limit: Int): List<PendingUploadEntity>

    @Query("DELETE FROM pending_uploads WHERE id = :id")
    suspend fun deleteById(id: String)

    @Update
    suspend fun update(pendingUpload: PendingUploadEntity)

    @Query("SELECT COUNT(*) FROM pending_uploads")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_uploads")
    suspend fun getPendingCount(): Int
}
