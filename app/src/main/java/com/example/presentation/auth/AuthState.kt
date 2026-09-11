package com.example.presentation.auth

import com.example.domain.model.AuthState as DomainAuthState

/**
 * Presentation layer UI state for Authentication.
 * Decoupled from the pure domain model.
 */
sealed class AuthState {
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Authenticated(val userId: String, val email: String?) : AuthState()
    data class Error(val message: String) : AuthState()
}

fun DomainAuthState.toUiState(): AuthState = when (this) {
    is DomainAuthState.Unauthenticated -> AuthState.Unauthenticated
    is DomainAuthState.Loading -> AuthState.Loading
    is DomainAuthState.Authenticated -> AuthState.Authenticated(userId, email)
    is DomainAuthState.Error -> AuthState.Error(message)
}
