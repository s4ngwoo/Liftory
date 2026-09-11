package com.example.presentation.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSetEditorSheet(
    onDismissRequest: () -> Unit,
    onSaveSet: (weight: Double, reps: Int, rpe: Double?) -> Unit,
    exerciseName: String = "Exercise",
    setNumber: Int = 1,
    lastSetSummary: String? = null
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        ExerciseSetEditorContent(
            onSaveSet = onSaveSet,
            exerciseName = exerciseName,
            setNumber = setNumber,
            lastSetSummary = lastSetSummary,
            onClose = onDismissRequest
        )
    }
}

@Composable
fun ExerciseSetEditorContent(
    onSaveSet: (weight: Double, reps: Int, rpe: Double?) -> Unit,
    exerciseName: String = "Exercise",
    setNumber: Int = 1,
    lastSetSummary: String? = null,
    onClose: (() -> Unit)? = null
) {
    var weightInput by remember { mutableStateOf("") }
    var repsInput by remember { mutableStateOf("") }
    var rpeInput by remember { mutableStateOf("") }

    val weightFocusRequester = remember { FocusRequester() }
    val repsFocusRequester = remember { FocusRequester() }
    val rpeFocusRequester = remember { FocusRequester() }

    val performSave = {
        val weight = weightInput.toDoubleOrNull() ?: 0.0
        val reps = repsInput.toIntOrNull() ?: 0
        val rpe = rpeInput.toDoubleOrNull()
        onSaveSet(weight, reps, rpe)
    }

    LaunchedEffect(Unit) {
        delay(150)
        try {
            weightFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 20.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = exerciseName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${setNumber}세트 기록 (Set $setNumber)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (onClose != null) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Previous Set Reference Banner (if available)
        if (lastSetSummary != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "이전 세트 참고: ",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = lastSetSummary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input Fields: Bottom-to-Top Filling Order
        // Visual Stack:
        // [3. RPE (선택)] - Next jumps here from Reps
        // [2. Reps (회)] - Next jumps here from Weight
        // [1. Weight (kg)] - Lowest field, nearest to keyboard, auto-focused on launch!

        // 3. RPE (Optional) - Topmost of the three inputs
        OutlinedTextField(
            value = rpeInput,
            onValueChange = { rpeInput = it },
            label = { Text("RPE (운동 자각도, 선택: 6~10)") },
            placeholder = { Text("예: 8.5 (생략 가능)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { performSave() }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(rpeFocusRequester)
                .testTag("input_rpe")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Reps - Middle
        OutlinedTextField(
            value = repsInput,
            onValueChange = { repsInput = it },
            label = { Text("반복 횟수 (Reps)") },
            placeholder = { Text("예: 10") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { rpeFocusRequester.requestFocus() }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(repsFocusRequester)
                .testTag("input_reps")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 1. Weight - Bottom-most input (closest to keyboard, auto-focused)
        OutlinedTextField(
            value = weightInput,
            onValueChange = { weightInput = it },
            label = { Text("무게 (Weight kg)") },
            placeholder = { Text("예: 60.0") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { repsFocusRequester.requestFocus() }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(weightFocusRequester)
                .testTag("input_weight")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Save Set Action Button (right above the keypad)
        Button(
            onClick = { performSave() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_save_set"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("세트 저장 (Save Set)", fontWeight = FontWeight.Bold)
        }
    }
}
