package com.example.infrastructure.repository

import com.example.presentation.auth.AuthState
import com.example.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import android.util.Log

class FirebaseAuthRepositoryImpl(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                _authState.value = AuthState.Authenticated(
                    userId = user.uid,
                    email = user.email
                    
                )
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        return try {
            _authState.value = AuthState.Loading
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseAuthRepo", "Sign in failed", e)
            _authState.value = AuthState.Error(e.message ?: "Authentication failed")
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
}
