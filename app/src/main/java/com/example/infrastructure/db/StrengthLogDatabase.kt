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
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                scope.launch {
                    seedDefaultsIfEmpty(instance)
                }
                instance
            }
        }

        suspend fun seedDefaultsIfEmpty(database: StrengthLogDatabase) {
            val exerciseDao = database.exerciseDao()
            if (exerciseDao.getCount() == 0) {
                populateDefaultExercises(exerciseDao)
            }
            val routineDao = database.routineTemplateDao()
            if (routineDao.getById("routine_push") == null) {
                populateDefaultRoutines(routineDao)
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
                ExerciseEntity("ex_pullup", "Pull Up (풀업)", false, "Back", now, now),
                ExerciseEntity("ex_incline_bench", "Incline Dumbbell Press (인클라인 덤벨 프레스)", false, "Chest", now, now),
                ExerciseEntity("ex_lat_pulldown", "Lat Pulldown (랫 풀다운)", false, "Back", now, now),
                ExerciseEntity("ex_leg_press", "Leg Press (레그 프레스)", false, "Legs", now, now),
                ExerciseEntity("ex_lateral_raise", "Lateral Raise (사이드 레터럴 레이즈)", false, "Shoulders", now, now),
                ExerciseEntity("ex_bicep_curl", "Bicep Curl (덤벨 컬)", false, "Arms", now, now),
                ExerciseEntity("ex_tricep_pushdown", "Tricep Pushdown (트라이셉 푸시다운)", false, "Arms", now, now)
            )
            dao.insertAll(defaults)
        }

        suspend fun populateDefaultRoutines(dao: com.example.infrastructure.db.dao.RoutineTemplateDao) {
            val now = System.currentTimeMillis()
            // Push Day
            dao.insertTemplate(com.example.infrastructure.db.entity.RoutineTemplateEntity("routine_push", "Push Day (가슴/어깨/삼두)", now, now))
            dao.insertPresets(listOf(
                com.example.infrastructure.db.entity.ExercisePresetEntity("routine_push", "ex_bench", 60.0, 10, 0),
                com.example.infrastructure.db.entity.ExercisePresetEntity("routine_push", "ex_ohp", 40.0, 8, 1),
                com.example.infrastructure.db.entity.ExercisePresetEntity("routine_push", "ex_incline_bench", 20.0, 10, 2)
            ))
            // Pull Day
            dao.insertTemplate(com.example.infrastructure.db.entity.RoutineTemplateEntity("routine_pull", "Pull Day (등/이두)", now, now))
            dao.insertPresets(listOf(
                com.example.infrastructure.db.entity.ExercisePresetEntity("routine_pull", "ex_deadlift", 100.0, 5, 0),
                com.example.infrastructure.db.entity.ExercisePresetEntity("routine_pull", "ex_row", 60.0, 8, 1),
                com.example.infrastructure.db.entity.ExercisePresetEntity("routine_pull", "ex_lat_pulldown", 50.0, 10, 2)
            ))
            // Leg Day
            dao.insertTemplate(com.example.infrastructure.db.entity.RoutineTemplateEntity("routine_leg", "Leg Day (하체)", now, now))
            dao.insertPresets(listOf(
                com.example.infrastructure.db.entity.ExercisePresetEntity("routine_leg", "ex_squat", 80.0, 5, 0),
                com.example.infrastructure.db.entity.ExercisePresetEntity("routine_leg", "ex_leg_press", 120.0, 10, 1)
            ))
        }
    }
}
