package com.example.domain.model

/**
 * Pure domain model for Authentication state.
 * Independent of Android, Firebase, and Presentation UI state.
 */
sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Authenticated(val userId: String, val email: String?) : AuthState()
    data class Error(val message: String) : AuthState()
}
