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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSetEditorSheet(
    onDismissRequest: () -> Unit,
    onSaveSet: (weight: Double, reps: Int, rpe: Double?) -> Unit,
    exerciseName: String = "Exercise",
    setNumber: Int = 1,
    isCardio: Boolean = false,
    lastSetSummary: String? = null,
    lastWeight: Double? = null,
    lastReps: Int? = null,
    lastRpe: Double? = null,
    lastSessionDate: Long? = null,
    lastSessionWeight: Double? = null,
    lastSessionReps: Int? = null,
    lastSessionRpe: Double? = null
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
            isCardio = isCardio,
            lastSetSummary = lastSetSummary,
            lastWeight = lastWeight,
            lastReps = lastReps,
            lastRpe = lastRpe,
            lastSessionDate = lastSessionDate,
            lastSessionWeight = lastSessionWeight,
            lastSessionReps = lastSessionReps,
            lastSessionRpe = lastSessionRpe,
            onClose = onDismissRequest
        )
    }
}

@Composable
fun ExerciseSetEditorContent(
    onSaveSet: (weight: Double, reps: Int, rpe: Double?) -> Unit,
    exerciseName: String = "Exercise",
    setNumber: Int = 1,
    isCardio: Boolean = false,
    lastSetSummary: String? = null,
    lastWeight: Double? = null,
    lastReps: Int? = null,
    lastRpe: Double? = null,
    lastSessionDate: Long? = null,
    lastSessionWeight: Double? = null,
    lastSessionReps: Int? = null,
    lastSessionRpe: Double? = null,
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

    val ghostWeight = lastWeight?.let { if (it % 1.0 == 0.0) "${it.toInt()}" else "$it" }
        ?: lastSessionWeight?.let { if (it % 1.0 == 0.0) "${it.toInt()}" else "$it" } ?: (if (isCardio) "6.0" else "0.0")
    val ghostReps = lastReps?.toString()
        ?: lastSessionReps?.toString() ?: (if (isCardio) "20" else "0")
    val ghostRpe = lastRpe?.let { if (it % 1.0 == 0.0) "${it.toInt()}" else "$it" }
        ?: lastSessionRpe?.let { if (it % 1.0 == 0.0) "${it.toInt()}" else "$it" } ?: "7.0"

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

        // Last Workout Session Reference Banner (Historical reference)
        if (lastSessionWeight != null && lastSessionReps != null) {
            Spacer(modifier = Modifier.height(8.dp))
            val dateLabel = if (lastSessionDate != null) {
                SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(Date(lastSessionDate))
            } else "이전 세션"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "📅 지난 운동 ($dateLabel $setNumber" + "세트):",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "${lastSessionWeight}kg × ${lastSessionReps}회" + if (lastSessionRpe != null) " (RPE $lastSessionRpe)" else "",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }

                AssistChip(
                    onClick = {
                        weightInput = if (lastSessionWeight % 1.0 == 0.0) "${lastSessionWeight.toInt()}" else "$lastSessionWeight"
                        repsInput = "$lastSessionReps"
                        rpeInput = lastSessionRpe?.let { if (it % 1.0 == 0.0) "${it.toInt()}" else "$it" } ?: ""
                    },
                    label = { Text("지난 세션 복사", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        labelColor = MaterialTheme.colorScheme.onTertiary
                    ),
                    modifier = Modifier.height(30.dp)
                )
            }
        }

        // Previous Set in Current Session Reference Banner (with 1-touch copy button)
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
                        text = "오늘 직전 세트: ",
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
                        label = { Text("직전 세트 복사", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
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

        // Quick delta buttons (weight delta with explicit kg unit, reps delta with explicit 회 unit)
        if (isCardio) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("+5분", "+10분", "+15분", "+30분").forEach { delta ->
                    val min = delta.removeSuffix("분").toInt()
                    AssistChip(
                        onClick = {
                            val current = repsInput.toIntOrNull() ?: 0
                            repsInput = "${current + min}"
                        },
                        label = { Text(delta, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("+1.0 kg", "+2.5 kg", "+5.0 kg", "+10.0 kg").forEach { delta ->
                        val add = delta.removeSuffix(" kg").toDouble()
                        AssistChip(
                            onClick = {
                                val current = weightInput.toDoubleOrNull() ?: 0.0
                                weightInput = "%.1f".format(current + add).trimEnd('0').trimEnd('.')
                            },
                            label = { Text(delta, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("+1 회", "+2 회", "+5 회").forEach { delta ->
                        val add = delta.removeSuffix(" 회").toInt()
                        AssistChip(
                            onClick = {
                                val current = repsInput.toIntOrNull() ?: 0
                                repsInput = "${current + add}"
                            },
                            label = { Text(delta, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Primary 2-Column Inputs:
        // Weight/Speed (50%) | Reps/Time (50%)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Weight or Speed/Level
            OutlinedTextField(
                value = weightInput,
                onValueChange = { weightInput = it },
                label = { Text(if (isCardio) "속도/레벨" else "무게 (kg)") },
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
                    .weight(1f)
                    .focusRequester(weightFocusRequester)
                    .testTag("input_weight")
            )

            // 2. Reps or Minutes
            OutlinedTextField(
                value = repsInput,
                onValueChange = { repsInput = it },
                label = { Text(if (isCardio) "시간 (분)" else "횟수 (회)") },
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
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Optional RPE (운동 강도) with quick preset chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = rpeInput,
                onValueChange = { rpeInput = it },
                label = { Text("운동 강도 (RPE · 선택)") },
                placeholder = { Text(if (ghostRpe.isNotBlank()) "직전: $ghostRpe" else "6.0 ~ 10.0") },
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

            listOf(7.0, 8.0, 9.0, 10.0).forEach { rpePreset ->
                val str = if (rpePreset % 1.0 == 0.0) "${rpePreset.toInt()}" else "$rpePreset"
                FilterChip(
                    selected = rpeInput == str,
                    onClick = {
                        rpeInput = if (rpeInput == str) "" else str
                    },
                    label = { Text("${rpePreset.toInt()}", fontWeight = FontWeight.Bold) },
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Live estimated 1RM feedback badge (strength only)
        if (!isCardio && liveOneRm > 0.0) {
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
