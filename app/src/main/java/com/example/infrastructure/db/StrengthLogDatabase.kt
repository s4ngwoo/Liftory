package com.example.infrastructure.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.infrastructure.db.dao.ExerciseDao
import com.example.infrastructure.db.dao.ExerciseSetDao
import com.example.infrastructure.db.dao.PendingUploadDao
import com.example.infrastructure.db.dao.WorkoutSessionDao
import com.example.infrastructure.db.entity.ExerciseEntity
import com.example.infrastructure.db.entity.ExerciseSetEntity
import com.example.infrastructure.db.entity.PendingUploadEntity
import com.example.infrastructure.db.entity.WorkoutSessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        WorkoutSessionEntity::class,
        com.example.infrastructure.db.entity.RoutineTemplateEntity::class,
        com.example.infrastructure.db.entity.ExercisePresetEntity::class,
        ExerciseSetEntity::class,
        ExerciseEntity::class,
        PendingUploadEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class StrengthLogDatabase : RoomDatabase() {
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun exerciseSetDao(): ExerciseSetDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun pendingUploadDao(): PendingUploadDao
    abstract fun routineTemplateDao(): com.example.infrastructure.db.dao.RoutineTemplateDao

    companion object {
        private const val DATABASE_NAME = "strength_log.db"

        @Volatile
        private var INSTANCE: StrengthLogDatabase? = null

        fun getInstance(context: Context, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)): StrengthLogDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StrengthLogDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(DatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch {
                        populateDefaultExercises(database.exerciseDao())
                    }
                }
            }
        }

        suspend fun populateDefaultExercises(dao: ExerciseDao) {
            val now = System.currentTimeMillis()
            val defaults = listOf(
                ExerciseEntity("ex_squat", "Squat (바벨 스쿼트)", false, "Legs", now, now),
                ExerciseEntity("ex_bench", "Bench Press (바벨 벤치프레스)", false, "Chest", now, now),
                ExerciseEntity("ex_deadlift", "Deadlift (데드리프트)", false, "Back", now, now),
                ExerciseEntity("ex_ohp", "Overhead Press (밀리터리 프레스)", false, "Shoulders", now, now),
                ExerciseEntity("ex_row", "Barbell Row (바벨 로우)", false, "Back", now, now),
                ExerciseEntity("ex_pullup", "Pull Up (풀업)", false, "Back", now, now)
            )
            dao.insertAll(defaults)
        }
    }
}
