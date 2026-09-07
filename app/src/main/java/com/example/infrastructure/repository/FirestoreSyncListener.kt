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
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val authRepository: AuthRepository
) : SyncListener {

    private var sessionsListener: ListenerRegistration? = null
    
    override fun startListening() {
        val userId = authRepository.getCurrentUserId() ?: return
        
        sessionsListener = firestore.collection("users").document(userId)
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
