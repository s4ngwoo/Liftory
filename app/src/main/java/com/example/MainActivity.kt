package com.example

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.rememberNavController
import com.example.domain.util.SessionNotesManager
import com.example.infrastructure.service.WorkoutTimerService
import com.example.presentation.AppNavigation
import com.example.ui.theme.StrengthLogTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var pendingSessionId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        val app = application as StrengthLogApplication
        val container = app.container

        handleDeepLinkIntent(intent)

        // Observe active session: sync system status bar timer via NotificationScheduler (N05.5/N05.6).
        // Permission or FGS start failure must not affect local session observation.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                container.observeActiveWorkoutSessionUseCase().collect { session ->
                    if (session != null) {
                        val title = SessionNotesManager.getSessionTitle(session.notes).ifBlank { "운동 세션" }
                        container.notificationScheduler.startWorkoutOngoing(
                            sessionId = session.id,
                            title = title,
                            startTimeEpochMs = session.startTime
                        )
                    } else {
                        container.notificationScheduler.stopWorkoutOngoing()
                    }
                }
            }
        }

        setContent {
            StrengthLogTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    LaunchedEffect(pendingSessionId) {
                        val sid = pendingSessionId
                        if (sid != null) {
                            navController.navigate("session/$sid") {
                                launchSingleTop = true
                            }
                            pendingSessionId = null
                        }
                    }

                    AppNavigation(
                        navController = navController,
                        appContainer = container
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        val sessionId = intent?.getStringExtra(WorkoutTimerService.EXTRA_SESSION_ID)
        if (!sessionId.isNullOrBlank()) {
            pendingSessionId = sessionId
        }
    }
}


