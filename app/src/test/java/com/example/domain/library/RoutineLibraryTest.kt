package com.example.domain.library

import com.example.domain.model.MeasurementProfile
import com.example.domain.model.MeasurementValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * N12 Published Routine Library Tests (LIB-01~LIB-06).
 */
class RoutineLibraryTest {

    @Test
    fun `LIB-01 import published routine creates personal copy with provenance and resets target weights`() {
        val published = PublishedRoutine(
            id = "pub_routine_1",
            authorId = "author_coach_kim",
            title = "3대 운동 기초 v1",
            version = 1,
            isPublic = true,
            exercises = listOf(
                PublishedExercise(
                    exerciseId = "ex_squat",
                    exerciseName = "스쿼트",
                    profile = MeasurementProfile.WEIGHT_AND_REPS,
                    targetReps = 10,
                    suggestedWeightKg = 140.0 // Coach's heavy weight!
                )
            )
        )

        val service = RoutineLibraryService()
        val commandId = "import_cmd_1"
        val imported = service.importToMyRoutines(
            userId = "user_me",
            commandId = commandId,
            published = published,
            resetPersonalWeights = true
        )

        assertNotNull(imported)
        assertEquals("user_me", imported.ownerUserId)
        assertEquals("pub_routine_1", imported.provenanceRoutineId)
        assertEquals(1, imported.provenanceVersion)
        // User's weight is reset or set to 0.0 rather than forcing coach's 140kg
        assertEquals(0.0, imported.exercises[0].personalTargetWeightKg, 0.001)
    }

    @Test
    fun `LIB-02 author updating or deleting original routine does not mutate imported copy`() {
        val service = RoutineLibraryService()
        val original = PublishedRoutine(
            id = "pub_v1",
            authorId = "author_1",
            title = "Original Workout",
            version = 1,
            isPublic = true,
            exercises = listOf(
                PublishedExercise("ex_1", "Bench", MeasurementProfile.WEIGHT_AND_REPS, 10, 80.0)
            )
        )
        val myCopy = service.importToMyRoutines("user_2", "cmd_2", original)

        // Author updates to v2 and then deletes
        service.updateRoutine("author_1", original.copy(version = 2, title = "Changed v2 Title"))
        service.deleteRoutine("author_1", "pub_v1")

        // My imported copy remains intact
        assertEquals("Original Workout", myCopy.title)
        assertEquals(1, myCopy.provenanceVersion)
    }

    @Test
    fun `LIB-03 duplicate import command returns existing copy without creating duplicates`() {
        val service = RoutineLibraryService()
        val routine = PublishedRoutine(
            id = "pub_3",
            authorId = "author_3",
            title = "Duplicate Test",
            version = 1,
            isPublic = true,
            exercises = emptyList()
        )

        val firstImport = service.importToMyRoutines("user_1", "cmd_idempotent", routine)
        val secondImport = service.importToMyRoutines("user_1", "cmd_idempotent", routine)

        assertEquals("Same command ID must return the same copy", firstImport.id, secondImport.id)
        assertEquals(1, service.getUserImportedRoutines("user_1").size)
    }

    @Test
    fun `LIB-04 non-author cannot view or modify private routine`() {
        val service = RoutineLibraryService()
        val privateRoutine = PublishedRoutine(
            id = "private_rt",
            authorId = "author_secret",
            title = "Secret Plan",
            version = 1,
            isPublic = false,
            exercises = emptyList()
        )
        service.publishOrSaveDraft("author_secret", privateRoutine)

        // Other user attempts to query or edit
        val viewResult = service.getPublishedRoutine("other_user", "private_rt")
        assertNull("Private routine must not be visible to other users", viewResult)

        val editResult = service.updateRoutine("other_user", privateRoutine.copy(title = "Hacked"))
        assertFalse("Other user cannot modify private routine", editResult.isSuccess)
    }

    @Test
    fun `LIB-05 routine containing unsupported measurement profile halts import with warning`() {
        val unsupportedRoutine = PublishedRoutine(
            id = "pub_unsupported",
            authorId = "author_future",
            title = "Future Workout",
            version = 1,
            isPublic = true,
            exercises = listOf(
                PublishedExercise("ex_alien", "Alien Motion", MeasurementProfile.LEGACY_UNKNOWN, 10, 0.0)
            )
        )

        val service = RoutineLibraryService()
        val check = service.validateCompatibility(unsupportedRoutine)
        assertFalse("Routine with unsupported profile must fail compatibility check", check.isCompatible)
        assertTrue(check.incompatibleReasons.any { it.contains("LEGACY_UNKNOWN") })
    }

    @Test
    fun `LIB-06 cached offline routine copy allows workout execution during network disconnection`() {
        val localCache = LocalRoutineCache()
        val cachedRoutine = ImportedRoutineCopy(
            id = "local_copy_1",
            ownerUserId = "user_me",
            title = "Offline Cached Plan",
            provenanceRoutineId = "pub_remote_1",
            provenanceVersion = 1,
            exercises = listOf(
                ImportedExercise("ex_pushup", "푸시업", MeasurementProfile.BODYWEIGHT_PLUS_REPS, 15, 0.0)
            )
        )
        localCache.save(cachedRoutine)

        // Offline retrieval
        val retrieved = localCache.getById("local_copy_1")
        assertNotNull(retrieved)
        assertEquals("Offline Cached Plan", retrieved!!.title)
    }
}
