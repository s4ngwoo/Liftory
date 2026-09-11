package com.example.infrastructure.repository

import android.util.Log
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.SyncListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FirestoreSyncListener(
    private val firestore: FirebaseFirestore? = runCatching { FirebaseFirestore.getInstance() }.getOrNull(),
    private val authRepository: AuthRepository
) : SyncListener {

    private var sessionsListener: ListenerRegistration? = null
    
    override fun startListening() {
        val firestoreInstance = firestore ?: run {
            Log.w("FirestoreSync", "Firebase is not initialized. Listening disabled.")
            return
        }
        val userId = authRepository.getCurrentUserId() ?: return
        
        sessionsListener = firestoreInstance.collection("users").document(userId)
            .collection("sessions")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("FirestoreSync", "Listen failed.", e)
                    return@addSnapshotListener
                }
                
                // For simplicity, just log changes. In full implementation,
                // this would parse snapshots, compare `updatedAt`, and save to Room.
                for (dc in snapshots!!.documentChanges) {
                    Log.d("FirestoreSync", "Detected change: ${dc.type} - ${dc.document.id}")
                }
            }
    }

    override fun stopListening() {
        sessionsListener?.remove()
    }
}
