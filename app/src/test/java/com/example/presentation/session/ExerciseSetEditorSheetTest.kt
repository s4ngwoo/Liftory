package com.example.presentation.session

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExerciseSetEditorSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `when fields are filled and save is clicked, onSaveSet is called with correct values`() {
        var savedWeight = 0.0
        var savedReps = 0
        var savedRpe: Double? = null
        var isCalled = false

        composeTestRule.setContent {
            ExerciseSetEditorContent(
                onSaveSet = { weight, reps, rpe ->
                    savedWeight = weight
                    savedReps = reps
                    savedRpe = rpe
                    isCalled = true
                }
            )
        }

        // Fill inputs
        composeTestRule.onNodeWithTag("input_weight").performTextInput("100.5")
        composeTestRule.onNodeWithTag("input_reps").performTextInput("10")
        composeTestRule.onNodeWithTag("input_rpe").performTextInput("8")

        // Click save
        composeTestRule.onNodeWithTag("btn_save_set").performClick()

        // Assert
        assertTrue("onSaveSet should have been called", isCalled)
        assertEquals(100.5, savedWeight, 0.0)
        assertEquals(10, savedReps)
        assertEquals(8.0, savedRpe ?: 0.0, 0.0)
    }
}
