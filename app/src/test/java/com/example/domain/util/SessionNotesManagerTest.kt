package com.example.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionNotesManagerTest {

    @Test
    fun `title and exercise comments are saved and retrieved correctly`() {
        val initialNotes = "가슴 & 삼두 루틴"
        
        // Add comment for bench press
        val notesWithBench = SessionNotesManager.setExerciseComment(
            initialNotes,
            "ex_bench",
            "80kg 5세트 완료. 다음엔 82.5kg 도전!"
        )
        
        // Add comment for incline dumbbell press
        val notesWithBoth = SessionNotesManager.setExerciseComment(
            notesWithBench,
            "ex_incline",
            "각도 30도 유지. 펌핑감 최고"
        )
        
        assertEquals("가슴 & 삼두 루틴", SessionNotesManager.getSessionTitle(notesWithBoth))
        assertEquals("80kg 5세트 완료. 다음엔 82.5kg 도전!", SessionNotesManager.getExerciseComment(notesWithBoth, "ex_bench"))
        assertEquals("각도 30도 유지. 펌핑감 최고", SessionNotesManager.getExerciseComment(notesWithBoth, "ex_incline"))
        assertEquals("", SessionNotesManager.getExerciseComment(notesWithBoth, "ex_other"))

        // Update session title without losing comments
        val updatedTitle = SessionNotesManager.setSessionTitle(notesWithBoth, "가슴 & 어깨 루틴")
        assertEquals("가슴 & 어깨 루틴", SessionNotesManager.getSessionTitle(updatedTitle))
        assertEquals("80kg 5세트 완료. 다음엔 82.5kg 도전!", SessionNotesManager.getExerciseComment(updatedTitle, "ex_bench"))
    }
}
