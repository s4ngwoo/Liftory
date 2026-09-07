package com.example.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    val authState = authRepository.authState

    fun signInWithGoogle() {
        // Mock sign in for testing
        // In real app, launch Credential Manager flow in Activity
    }

    fun handleGoogleSignInSuccess(idToken: String) {
        viewModelScope.launch {
            authRepository.signInWithGoogle(idToken)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
    
    class Factory(
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(authRepository) as T
        }
    }
}
