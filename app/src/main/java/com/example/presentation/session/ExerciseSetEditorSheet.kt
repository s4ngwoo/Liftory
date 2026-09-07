package com.example.presentation.session

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSetEditorSheet(
    onDismissRequest: () -> Unit,
    onSaveSet: (weight: Double, reps: Int, rpe: Double?) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        ExerciseSetEditorContent(onSaveSet = onSaveSet)
    }
}

@Composable
fun ExerciseSetEditorContent(
    onSaveSet: (weight: Double, reps: Int, rpe: Double?) -> Unit
) {
    var weightInput by remember { mutableStateOf("") }
    var repsInput by remember { mutableStateOf("") }
    var rpeInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Record Set",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = weightInput,
            onValueChange = { weightInput = it },
            label = { Text("Weight (kg)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_weight")
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = repsInput,
            onValueChange = { repsInput = it },
            label = { Text("Reps") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_reps")
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = rpeInput,
            onValueChange = { rpeInput = it },
            label = { Text("RPE (Optional)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("input_rpe")
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val weight = weightInput.toDoubleOrNull() ?: 0.0
                val reps = repsInput.toIntOrNull() ?: 0
                val rpe = rpeInput.toDoubleOrNull()
                onSaveSet(weight, reps, rpe)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("btn_save_set"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Save Set", fontWeight = FontWeight.Bold)
        }
    }
}
