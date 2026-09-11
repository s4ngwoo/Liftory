package com.example.infrastructure.db

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseMigrationTest {

    private fun createInMemoryDb(): SupportSQLiteDatabase {
        val context = RuntimeEnvironment.getApplication()
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null) // in-memory
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {}
                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        return FrameworkSQLiteOpenHelperFactory().create(config).writableDatabase
    }

    @Test
    fun `migration 1 to 2 adds equipmentType and machineBrand preserving existing exercises (DATA-01, DATA-02)`() {
        val db = createInMemoryDb()

        // 1. Setup DDL as of schema version 1
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS exercises (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                isCustom INTEGER NOT NULL,
                muscleGroup TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // Insert version 1 sample exercise data
        db.execSQL(
            """
            INSERT INTO exercises (id, name, isCustom, muscleGroup, createdAt, updatedAt)
            VALUES ('ex_v1_bench', '벤치프레스', 0, 'Chest', 1000, 1000)
            """.trimIndent()
        )

        // 2. Execute Migration 1 -> 2
        StrengthLogDatabase.MIGRATION_1_2.migrate(db)

        // 3. Verify columns and data preservation
        val cursor = db.query("SELECT id, name, equipmentType, machineBrand FROM exercises WHERE id = 'ex_v1_bench'")
        assertTrue(cursor.moveToFirst())
        assertEquals("ex_v1_bench", cursor.getString(0))
        assertEquals("벤치프레스", cursor.getString(1))
        assertEquals("FREE_WEIGHT", cursor.getString(2)) // Default value populated
        assertTrue(cursor.isNull(3)) // machineBrand is null by default
        cursor.close()
    }

    @Test
    fun `migration 2 to 3 adds isCompleted and targetReps preserving existing sets (DATA-03)`() {
        val db = createInMemoryDb()

        // 1. Setup DDL as of schema version 2
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS workout_sessions (
                id TEXT NOT NULL PRIMARY KEY,
                startTime INTEGER NOT NULL,
                endTime INTEGER,
                notes TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS exercise_sets (
                id TEXT NOT NULL PRIMARY KEY,
                sessionId TEXT NOT NULL,
                exerciseId TEXT NOT NULL,
                weight REAL NOT NULL,
                reps INTEGER NOT NULL,
                rpe REAL,
                restSeconds INTEGER,
                orderIndex INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                FOREIGN KEY(sessionId) REFERENCES workout_sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO workout_sessions (id, startTime, endTime, notes, createdAt, updatedAt)
            VALUES ('session_v2_1', 2000, 3000, 'Test Session', 2000, 3000)
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO exercise_sets (id, sessionId, exerciseId, weight, reps, rpe, restSeconds, orderIndex, createdAt, updatedAt)
            VALUES ('set_v2_1', 'session_v2_1', 'ex_bench', 80.0, 10, 8.5, 90, 1, 2100, 2100)
            """.trimIndent()
        )

        // 2. Execute Migration 2 -> 3
        StrengthLogDatabase.MIGRATION_2_3.migrate(db)

        // 3. Verify columns and data preservation
        val cursor = db.query("SELECT id, weight, reps, isCompleted, targetReps FROM exercise_sets WHERE id = 'set_v2_1'")
        assertTrue(cursor.moveToFirst())
        assertEquals("set_v2_1", cursor.getString(0))
        assertEquals(80.0, cursor.getDouble(1), 0.001)
        assertEquals(10, cursor.getInt(2))
        assertEquals(1, cursor.getInt(3)) // Default isCompleted = 1 (true)
        assertTrue(cursor.isNull(4)) // Default targetReps = null
        cursor.close()
    }
}
