package com.example.presentation.session

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
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
import com.example.application.usecase.statistics.CalculateOneRepMaxUseCase
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSetEditorSheet(
    onDismissRequest: () -> Unit,
    onSaveSet: (weight: Double, reps: Int, rpe: Double?) -> Unit,
    exerciseName: String = "Exercise",
    setNumber: Int = 1,
    lastSetSummary: String? = null,
    lastWeight: Double? = null,
    lastReps: Int? = null,
    lastRpe: Double? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        ExerciseSetEditorContent(
            onSaveSet = onSaveSet,
            exerciseName = exerciseName,
            setNumber = setNumber,
            lastSetSummary = lastSetSummary,
            lastWeight = lastWeight,
            lastReps = lastReps,
            lastRpe = lastRpe,
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
    lastWeight: Double? = null,
    lastReps: Int? = null,
    lastRpe: Double? = null,
    onClose: (() -> Unit)? = null
) {
    var weightInput by remember { mutableStateOf("") }
    var repsInput by remember { mutableStateOf("") }
    var rpeInput by remember { mutableStateOf("") }

    val calculateOneRepMaxUseCase = remember { CalculateOneRepMaxUseCase() }

    val weightFocusRequester = remember { FocusRequester() }
    val repsFocusRequester = remember { FocusRequester() }
    val rpeFocusRequester = remember { FocusRequester() }

    val performSave = {
        val weight = weightInput.toDoubleOrNull() ?: 0.0
        val reps = repsInput.toIntOrNull() ?: 0
        val rpe = rpeInput.toDoubleOrNull()
        onSaveSet(weight, reps, rpe)
    }

    val ghostWeight = lastWeight?.let { if (it % 1.0 == 0.0) "${it.toInt()}" else "$it" } ?: "0.0"
    val ghostReps = lastReps?.toString() ?: "0"
    val ghostRpe = lastRpe?.let { if (it % 1.0 == 0.0) "${it.toInt()}" else "$it" } ?: "8.0"

    // Calculate live 1RM estimate
    val liveWeight = weightInput.toDoubleOrNull() ?: 0.0
    val liveReps = repsInput.toIntOrNull() ?: 0
    val liveRpe = rpeInput.toDoubleOrNull()
    val liveOneRm = remember(weightInput, repsInput, rpeInput) {
        calculateOneRepMaxUseCase(liveWeight, liveReps, liveRpe)
    }

    LaunchedEffect(Unit) {
        delay(200)
        try {
            weightFocusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
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

        // Previous Set Reference Banner (with 1-touch copy button)
        if (lastSetSummary != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "이전 세트: ",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = lastSetSummary,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (lastWeight != null && lastReps != null) {
                    AssistChip(
                        onClick = {
                            weightInput = if (lastWeight % 1.0 == 0.0) "${lastWeight.toInt()}" else "$lastWeight"
                            repsInput = "$lastReps"
                            rpeInput = lastRpe?.let { if (it % 1.0 == 0.0) "${it.toInt()}" else "$it" } ?: ""
                        },
                        label = { Text("이전 세트 복사", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.height(30.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Quick weight delta buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("+2.5", "+5.0", "+10.0").forEach { delta ->
                AssistChip(
                    onClick = {
                        val current = weightInput.toDoubleOrNull() ?: 0.0
                        val add = delta.toDouble()
                        weightInput = "%.1f".format(current + add).trimEnd('0').trimEnd('.')
                    },
                    label = { Text(delta, style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Compact Horizontal Row for 3 Inputs:
        // Weight (40%) | Reps (30%) | RPE (30%)
        // Height is only ~64dp, ensuring 100% full visibility above the keyboard!
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Weight (kg) - Auto focused with ghost placeholder!
            OutlinedTextField(
                value = weightInput,
                onValueChange = { weightInput = it },
                label = { Text("무게(kg)") },
                placeholder = { Text(ghostWeight) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { repsFocusRequester.requestFocus() }
                ),
                modifier = Modifier
                    .weight(1.3f)
                    .focusRequester(weightFocusRequester)
                    .testTag("input_weight")
            )

            // 2. Reps with ghost placeholder!
            OutlinedTextField(
                value = repsInput,
                onValueChange = { repsInput = it },
                label = { Text("횟수") },
                placeholder = { Text(ghostReps) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { rpeFocusRequester.requestFocus() }
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(repsFocusRequester)
                    .testTag("input_reps")
            )

            // 3. RPE (Optional) with ghost placeholder!
            OutlinedTextField(
                value = rpeInput,
                onValueChange = { rpeInput = it },
                label = { Text("RPE") },
                placeholder = { Text(ghostRpe) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { performSave() }
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(rpeFocusRequester)
                    .testTag("input_rpe")
            )
        }

        // Live estimated 1RM feedback badge
        if (liveOneRm > 0.0) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🔥 예상 1RM",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (liveRpe != null) "(RPE $liveRpe 반영)" else "(Epley 공식)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "$liveOneRm kg",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Save Set Action Button (positioned directly beneath the inputs)
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
