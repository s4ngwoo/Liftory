package com.example.domain.repository

import com.example.presentation.auth.AuthState
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val authState: StateFlow<AuthState>
    fun getCurrentUserId(): String?
    suspend fun signInWithGoogle(idToken: String): Result<Unit>
    suspend fun signOut(): Result<Unit>
}
