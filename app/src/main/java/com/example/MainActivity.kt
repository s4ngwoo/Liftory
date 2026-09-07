package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.presentation.scaffold.StrengthLogScaffoldScreen
import com.example.presentation.scaffold.StrengthLogScaffoldViewModel
import com.example.ui.theme.StrengthLogTheme

class MainActivity : ComponentActivity() {

  private val viewModel: StrengthLogScaffoldViewModel by viewModels {
    val app = application as StrengthLogApplication
    val container = app.container
    StrengthLogScaffoldViewModel.Factory(
      observeSessionsUseCase = container.observeWorkoutSessionsUseCase,
      observeExercisesUseCase = container.observeExercisesUseCase,
      createSessionUseCase = container.createWorkoutSessionUseCase,
      syncQueueRepository = container.syncQueueRepository,
      startSyncWorkUseCase = container.startSyncWorkUseCase,
      createExerciseUseCase = container.createExerciseUseCase,
      addExerciseSetUseCase = container.addExerciseSetUseCase
    )
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      StrengthLogTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          StrengthLogScaffoldScreen(viewModel = viewModel)
        }
      }
    }
  }
}

