package com.example.infrastructure.repository

import android.util.Log
import com.example.domain.model.PendingUpload
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.RemoteSyncDataSource
import com.example.domain.sync.SyncOutboxOwnerGate
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreSyncDataSource(
    private val firestore: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull(),
    private val authRepository: AuthRepository
) : RemoteSyncDataSource {

    override suspend fun sync(pendingUpload: PendingUpload): Result<Unit> {
        val currentUserId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("Not logged in"))
        if (!SyncOutboxOwnerGate.canPush(pendingUpload.userId, currentUserId)) {
            Log.w(
                "FirestoreSync",
                "Refusing to sync ${pendingUpload.id}: outbox owner ${pendingUpload.userId} != current $currentUserId"
            )
            return Result.failure(Exception("Outbox owner mismatch"))
        }

        val firestoreInstance = firestore ?: run {
            Log.w("FirestoreSync", "Firebase is not initialized. Skipping remote sync for ${pendingUpload.id}")
            return Result.failure(IllegalStateException("Firebase is not initialized. Remote sync skipped."))
        }

        return try {
            val userRef = firestoreInstance.collection("users").document(pendingUpload.userId)
            val collectionRef = when (pendingUpload.entityType) {
                com.example.domain.model.EntityType.SESSION -> userRef.collection("sessions")
                com.example.domain.model.EntityType.SET -> userRef.collection("sets")
                com.example.domain.model.EntityType.EXERCISE -> userRef.collection("exercises")
                com.example.domain.model.EntityType.ROUTINE -> userRef.collection("routines")
                else -> return Result.failure(Exception("Unknown entity type: ${pendingUpload.entityType}"))
            }

            val docRef = collectionRef.document(pendingUpload.entityId)
            
            if (pendingUpload.operation == com.example.domain.model.SyncOperation.DELETE) {
                docRef.delete().await()
                return Result.success(Unit)
            }
            
            val dataMap = try {
                val jsonObject = org.json.JSONObject(pendingUpload.payloadJson)
                val map = mutableMapOf<String, Any>()
                val keys = jsonObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = jsonObject.get(key)
                }
                map
            } catch (e: Exception) {
                Log.e("FirestoreSync", "Failed to parse payload JSON", e)
                return Result.failure(e)
            }

            docRef.set(dataMap, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirestoreSync", "Sync failed for upload: ${pendingUpload.id}", e)
            Result.failure(e)
        }
    }
}
