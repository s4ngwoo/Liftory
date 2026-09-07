package com.example.presentation.session

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.domain.model.Exercise
import com.example.presentation.exercise.ExerciseListScreen
import com.example.presentation.exercise.ExerciseViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSelectionSheet(
    viewModel: ExerciseViewModel,
    onExerciseSelected: (Exercise) -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        ExerciseListScreen(
            viewModel = viewModel,
            onExerciseSelected = {
                onExerciseSelected(it)
                onDismissRequest()
            }
        )
    }
}
