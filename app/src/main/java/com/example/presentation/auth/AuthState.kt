package com.example.presentation.auth

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Authenticated(val userId: String, val email: String?) : AuthState()
    data class Error(val message: String) : AuthState()
}
