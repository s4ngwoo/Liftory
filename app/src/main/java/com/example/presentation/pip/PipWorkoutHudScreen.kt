package com.example.presentation.pip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.WorkoutSession
import com.example.domain.util.SessionNotesManager
import com.example.presentation.timer.RestTimerManager
import kotlinx.coroutines.delay

@Composable
fun PipWorkoutHudScreen(
    activeSession: WorkoutSession?,
    restTimerManager: RestTimerManager,
    modifier: Modifier = Modifier
) {
    val restTimerState by restTimerManager.timerState.collectAsStateWithLifecycle()

    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(activeSession?.id) {
        while (true) {
            delay(1000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val elapsedSeconds = if (activeSession != null) {
        ((nowMillis - activeSession.startTime).coerceAtLeast(0L) / 1000L).toInt()
    } else 0

    val elapsedHours = elapsedSeconds / 3600
    val elapsedMinutes = (elapsedSeconds % 3600) / 60
    val elapsedSecs = elapsedSeconds % 60
    val elapsedFormatted = if (elapsedHours > 0) {
        "%02d:%02d:%02d".format(elapsedHours, elapsedMinutes, elapsedSecs)
    } else {
        "%02d:%02d".format(elapsedMinutes, elapsedSecs)
    }

    val sessionTitle = remember(activeSession?.notes) {
        val raw = activeSession?.notes ?: ""
        val parsed = SessionNotesManager.getSessionTitle(raw)
        if (parsed.isNotBlank()) parsed else "운동 중"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Liftory · $sessionTitle",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = elapsedFormatted,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            if (restTimerState.isRunning) {
                Spacer(modifier = Modifier.height(4.dp))
                val rMin = restTimerState.remainingSeconds / 60
                val rSec = restTimerState.remainingSeconds % 60
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HourglassBottom,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "휴식 %02d:%02d".format(rMin, rSec),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFBBF24)
                    )
                }
            }
        }
    }
}
