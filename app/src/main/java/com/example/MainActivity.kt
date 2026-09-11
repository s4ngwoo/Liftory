package com.example

import android.app.PictureInPictureParams
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.rememberNavController
import com.example.domain.util.SessionNotesManager
import com.example.infrastructure.service.WorkoutTimerService
import com.example.presentation.AppNavigation
import com.example.presentation.pip.PipWorkoutHudScreen
import com.example.ui.theme.StrengthLogTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var pendingSessionId by mutableStateOf<String?>(null)
    private var isInPipMode by mutableStateOf(false)

    fun enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        addOnPictureInPictureModeChangedListener { info ->
            isInPipMode = info.isInPictureInPictureMode
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        val app = application as StrengthLogApplication
        val container = app.container

        handleDeepLinkIntent(intent)

        // Observe active session: sync system status bar timer & enable PiP auto-enter
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                container.observeActiveWorkoutSessionUseCase().collect { session ->
                    if (session != null) {
                        val title = SessionNotesManager.getSessionTitle(session.notes).ifBlank { "운동 세션" }
                        WorkoutTimerService.start(
                            this@MainActivity,
                            session.id,
                            title,
                            session.startTime
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val pipParams = PictureInPictureParams.Builder()
                                .setAspectRatio(Rational(16, 9))
                                .setAutoEnterEnabled(true)
                                .build()
                            setPictureInPictureParams(pipParams)
                        }
                    } else {
                        WorkoutTimerService.stop(this@MainActivity)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val pipParams = PictureInPictureParams.Builder()
                                .setAutoEnterEnabled(false)
                                .build()
                            setPictureInPictureParams(pipParams)
                        }
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
                    if (isInPipMode) {
                        val activeSession by container.observeActiveWorkoutSessionUseCase().collectAsStateWithLifecycle(initialValue = null)
                        PipWorkoutHudScreen(
                            activeSession = activeSession,
                            restTimerManager = container.restTimerManager
                        )
                    } else {
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
                            appContainer = container,
                            onEnterPip = { enterPip() }
                        )
                    }
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


