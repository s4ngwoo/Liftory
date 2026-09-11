package com.example.presentation.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToHome: () -> Unit
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()

    LaunchedEffect(authState) {
        if (authState is com.example.presentation.auth.AuthState.Authenticated) {
            onNavigateToHome()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Liftory", style = MaterialTheme.typography.headlineLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            
            when (authState) {
                is com.example.presentation.auth.AuthState.Loading -> {
                    CircularProgressIndicator()
                }
                is com.example.presentation.auth.AuthState.Error -> {
                    Text(
                        text = (authState as com.example.presentation.auth.AuthState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Button(onClick = { viewModel.signInWithGoogle() }) {
                        Text("다시 시도 (Retry)")
                    }
                    OutlinedButton(
                        onClick = onNavigateToHome,
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        Text("오프라인으로 계속하기 (Continue Offline)")
                    }
                }
                else -> {
                    Button(
                        onClick = { viewModel.signInWithGoogle() },
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        Text("Google 계정으로 로그인")
                    }
                    
                    OutlinedButton(
                        onClick = onNavigateToHome,
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        Text("오프라인으로 계속하기")
                    }
                }
            }
        }
    }
}
