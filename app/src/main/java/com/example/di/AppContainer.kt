package com.example.di

import android.content.Context
import com.example.application.usecase.exercise.CreateExerciseUseCase
import com.example.application.usecase.exercise.DeleteExerciseUseCase
import com.example.application.usecase.exercise.ObserveExercisesUseCase
import com.example.application.usecase.exercise.SearchExercisesUseCase
import com.example.application.usecase.exercise.UpdateExerciseUseCase
import com.example.application.usecase.routine.ApplyRoutineTemplateUseCase
import com.example.application.usecase.routine.CreateRoutineTemplateUseCase
import com.example.application.usecase.routine.ObserveRoutineTemplatesUseCase
import com.example.application.usecase.session.CreateWorkoutSessionUseCase
import com.example.application.usecase.session.DeleteWorkoutSessionUseCase
import com.example.application.usecase.session.GetWorkoutSessionUseCase
import com.example.application.usecase.session.ObserveWorkoutSessionsUseCase
import com.example.application.usecase.session.UpdateWorkoutSessionUseCase
import com.example.application.usecase.set.AddExerciseSetUseCase
import com.example.application.usecase.set.DeleteExerciseSetUseCase
import com.example.application.usecase.set.ObserveExerciseSetsUseCase
import com.example.application.usecase.set.UpdateExerciseSetUseCase
import com.example.domain.repository.ExerciseRepository
import com.example.domain.repository.ExerciseSetRepository
import com.example.domain.repository.RoutineTemplateRepository
import com.example.domain.repository.SyncQueueRepository
import com.example.domain.repository.WorkoutSessionRepository
import com.example.infrastructure.db.StrengthLogDatabase
import com.example.infrastructure.repository.ExerciseRepositoryImpl
import com.example.infrastructure.repository.ExerciseSetRepositoryImpl
import com.example.infrastructure.repository.RoutineTemplateRepositoryImpl
import com.example.infrastructure.repository.SyncQueueRepositoryImpl
import com.example.infrastructure.repository.WorkoutSessionRepositoryImpl
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import com.example.domain.repository.TransactionProvider
import com.example.infrastructure.repository.RoomTransactionProvider
import com.example.presentation.timer.RestTimerManager

interface AppContainer {
    val database: StrengthLogDatabase
    val workoutSessionRepository: WorkoutSessionRepository
    val exerciseSetRepository: ExerciseSetRepository
    val exerciseRepository: ExerciseRepository
    val routineTemplateRepository: RoutineTemplateRepository
    val syncQueueRepository: SyncQueueRepository
    val authRepository: com.example.domain.repository.AuthRepository
    val remoteSyncDataSource: com.example.domain.repository.RemoteSyncDataSource
    val syncScheduler: com.example.domain.port.SyncScheduler
    val startSyncWorkUseCase: com.example.application.usecase.sync.StartSyncWorkUseCase
    val transactionProvider: TransactionProvider

    val createWorkoutSessionUseCase: CreateWorkoutSessionUseCase
    val getWorkoutSessionUseCase: GetWorkoutSessionUseCase
    val updateWorkoutSessionUseCase: UpdateWorkoutSessionUseCase
    val deleteWorkoutSessionUseCase: DeleteWorkoutSessionUseCase
    val observeWorkoutSessionsUseCase: ObserveWorkoutSessionsUseCase
    val observeActiveWorkoutSessionUseCase: com.example.application.usecase.session.ObserveActiveWorkoutSessionUseCase

    val addExerciseSetUseCase: AddExerciseSetUseCase
    val updateExerciseSetUseCase: UpdateExerciseSetUseCase
    val deleteExerciseSetUseCase: DeleteExerciseSetUseCase
    val observeExerciseSetsUseCase: ObserveExerciseSetsUseCase

    val observeExercisesUseCase: ObserveExercisesUseCase
    val createExerciseUseCase: CreateExerciseUseCase
    val updateExerciseUseCase: UpdateExerciseUseCase
    val deleteExerciseUseCase: DeleteExerciseUseCase
    val searchExercisesUseCase: SearchExercisesUseCase
    
    val observeRoutineTemplatesUseCase: ObserveRoutineTemplatesUseCase
    val createRoutineTemplateUseCase: CreateRoutineTemplateUseCase
    val applyRoutineTemplateUseCase: ApplyRoutineTemplateUseCase
    val deleteRoutineTemplateUseCase: com.example.application.usecase.routine.DeleteRoutineTemplateUseCase
    val updateRoutineTemplateUseCase: com.example.application.usecase.routine.UpdateRoutineTemplateUseCase
    
    val statisticsRepository: com.example.domain.repository.StatisticsRepository
    val dataExporter: com.example.domain.repository.DataExporter
    val dataImporter: com.example.domain.repository.DataImporter
    val calculateWorkoutVolumeUseCase: com.example.application.usecase.statistics.CalculateWorkoutVolumeUseCase
    val calculatePersonalRecordsUseCase: com.example.application.usecase.statistics.CalculatePersonalRecordsUseCase
    val exportWorkoutDataUseCase: com.example.application.usecase.statistics.ExportWorkoutDataUseCase
    val importWorkoutDataUseCase: com.example.application.usecase.statistics.ImportWorkoutDataUseCase
    val calculateOneRepMaxUseCase: com.example.application.usecase.statistics.CalculateOneRepMaxUseCase
    val getLastExerciseHistoryUseCase: com.example.application.usecase.set.GetLastExerciseHistoryUseCase

    val notificationScheduler: com.example.domain.port.NotificationScheduler
    val wallClock: com.example.domain.port.WallClock
    val monotonicClock: com.example.domain.port.MonotonicClock
    val restTimerManager: RestTimerManager
}

class DefaultAppContainer(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AppContainer {

    override val database: StrengthLogDatabase by lazy {
        StrengthLogDatabase.getInstance(context)
    }

    override val workoutSessionRepository: WorkoutSessionRepository by lazy {
        WorkoutSessionRepositoryImpl(
            db = database,
            currentUserId = { authRepository.getCurrentUserId() },
            ioDispatcher = ioDispatcher
        )
    }

    override val exerciseSetRepository: ExerciseSetRepository by lazy {
        ExerciseSetRepositoryImpl(
            db = database,
            currentUserId = { authRepository.getCurrentUserId() },
            ioDispatcher = ioDispatcher
        )
    }

    override val exerciseRepository: ExerciseRepository by lazy {
        ExerciseRepositoryImpl(
            db = database,
            currentUserId = { authRepository.getCurrentUserId() },
            ioDispatcher = ioDispatcher
        )
    }

    override val routineTemplateRepository: RoutineTemplateRepository by lazy {
        RoutineTemplateRepositoryImpl(
            database = database,
            currentUserId = { authRepository.getCurrentUserId() },
            ioDispatcher = ioDispatcher
        )
    }

    override val syncQueueRepository: SyncQueueRepository by lazy {
        SyncQueueRepositoryImpl(
            pendingUploadDao = database.pendingUploadDao(),
            ioDispatcher = ioDispatcher
        )
    }

    override val authRepository: com.example.domain.repository.AuthRepository by lazy {
        com.example.infrastructure.repository.FirebaseAuthRepositoryImpl()
    }

    override val remoteSyncDataSource: com.example.domain.repository.RemoteSyncDataSource by lazy {
        com.example.infrastructure.repository.FirestoreSyncDataSource(authRepository = authRepository)
    }

    override val syncScheduler: com.example.domain.port.SyncScheduler by lazy {
        com.example.infrastructure.sync.WorkManagerSyncScheduler(context)
    }

    override val startSyncWorkUseCase: com.example.application.usecase.sync.StartSyncWorkUseCase by lazy {
        com.example.application.usecase.sync.StartSyncWorkUseCase(syncScheduler)
    }
    
    override val transactionProvider: TransactionProvider by lazy {
        RoomTransactionProvider(database)
    }

    override val createWorkoutSessionUseCase: CreateWorkoutSessionUseCase by lazy {
        CreateWorkoutSessionUseCase(workoutSessionRepository)
    }
    override val getWorkoutSessionUseCase: GetWorkoutSessionUseCase by lazy {
        GetWorkoutSessionUseCase(workoutSessionRepository)
    }
    override val updateWorkoutSessionUseCase: UpdateWorkoutSessionUseCase by lazy {
        UpdateWorkoutSessionUseCase(workoutSessionRepository)
    }
    override val deleteWorkoutSessionUseCase: DeleteWorkoutSessionUseCase by lazy {
        DeleteWorkoutSessionUseCase(workoutSessionRepository)
    }
    override val observeWorkoutSessionsUseCase: ObserveWorkoutSessionsUseCase by lazy {
        ObserveWorkoutSessionsUseCase(workoutSessionRepository)
    }
    override val observeActiveWorkoutSessionUseCase: com.example.application.usecase.session.ObserveActiveWorkoutSessionUseCase by lazy {
        com.example.application.usecase.session.ObserveActiveWorkoutSessionUseCase(workoutSessionRepository)
    }

    override val addExerciseSetUseCase: AddExerciseSetUseCase by lazy {
        AddExerciseSetUseCase(exerciseSetRepository, workoutSessionRepository, transactionProvider)
    }
    override val updateExerciseSetUseCase: UpdateExerciseSetUseCase by lazy {
        UpdateExerciseSetUseCase(exerciseSetRepository, workoutSessionRepository, transactionProvider)
    }
    override val deleteExerciseSetUseCase: DeleteExerciseSetUseCase by lazy {
        DeleteExerciseSetUseCase(exerciseSetRepository, workoutSessionRepository, transactionProvider)
    }
    override val observeExerciseSetsUseCase: ObserveExerciseSetsUseCase by lazy {
        ObserveExerciseSetsUseCase(exerciseSetRepository)
    }

    override val observeExercisesUseCase: ObserveExercisesUseCase by lazy {
        ObserveExercisesUseCase(exerciseRepository)
    }
    override val createExerciseUseCase: CreateExerciseUseCase by lazy {
        CreateExerciseUseCase(exerciseRepository)
    }
    override val updateExerciseUseCase: UpdateExerciseUseCase by lazy {
        UpdateExerciseUseCase(exerciseRepository)
    }
    override val deleteExerciseUseCase: DeleteExerciseUseCase by lazy {
        DeleteExerciseUseCase(exerciseRepository)
    }
    override val searchExercisesUseCase: SearchExercisesUseCase by lazy {
        SearchExercisesUseCase(exerciseRepository)
    }

    override val observeRoutineTemplatesUseCase: ObserveRoutineTemplatesUseCase by lazy {
        ObserveRoutineTemplatesUseCase(routineTemplateRepository)
    }
    override val createRoutineTemplateUseCase: CreateRoutineTemplateUseCase by lazy {
        CreateRoutineTemplateUseCase(routineTemplateRepository)
    }
    override val applyRoutineTemplateUseCase: ApplyRoutineTemplateUseCase by lazy {
        ApplyRoutineTemplateUseCase(routineTemplateRepository, exerciseSetRepository, workoutSessionRepository, transactionProvider)
    }
    override val deleteRoutineTemplateUseCase: com.example.application.usecase.routine.DeleteRoutineTemplateUseCase by lazy {
        com.example.application.usecase.routine.DeleteRoutineTemplateUseCase(routineTemplateRepository)
    }
    override val updateRoutineTemplateUseCase: com.example.application.usecase.routine.UpdateRoutineTemplateUseCase by lazy {
        com.example.application.usecase.routine.UpdateRoutineTemplateUseCase(routineTemplateRepository)
    }

    override val statisticsRepository: com.example.domain.repository.StatisticsRepository by lazy {
        com.example.infrastructure.repository.StatisticsRepositoryImpl(database.exerciseSetDao(), ioDispatcher)
    }
    override val dataExporter: com.example.domain.repository.DataExporter by lazy {
        com.example.infrastructure.export.DataExporterImpl(database.workoutSessionDao(), database.exerciseSetDao(), ioDispatcher)
    }
    override val dataImporter: com.example.domain.repository.DataImporter by lazy {
        com.example.infrastructure.export.DataImporterImpl(database.workoutSessionDao(), database.exerciseSetDao(), ioDispatcher)
    }
    override val calculateWorkoutVolumeUseCase: com.example.application.usecase.statistics.CalculateWorkoutVolumeUseCase by lazy {
        com.example.application.usecase.statistics.CalculateWorkoutVolumeUseCase(statisticsRepository)
    }
    override val calculatePersonalRecordsUseCase: com.example.application.usecase.statistics.CalculatePersonalRecordsUseCase by lazy {
        com.example.application.usecase.statistics.CalculatePersonalRecordsUseCase(statisticsRepository)
    }
    override val exportWorkoutDataUseCase: com.example.application.usecase.statistics.ExportWorkoutDataUseCase by lazy {
        com.example.application.usecase.statistics.ExportWorkoutDataUseCase(dataExporter)
    }
    override val importWorkoutDataUseCase: com.example.application.usecase.statistics.ImportWorkoutDataUseCase by lazy {
        com.example.application.usecase.statistics.ImportWorkoutDataUseCase(dataImporter)
    }
    override val calculateOneRepMaxUseCase: com.example.application.usecase.statistics.CalculateOneRepMaxUseCase by lazy {
        com.example.application.usecase.statistics.CalculateOneRepMaxUseCase()
    }
    override val getLastExerciseHistoryUseCase: com.example.application.usecase.set.GetLastExerciseHistoryUseCase by lazy {
        com.example.application.usecase.set.GetLastExerciseHistoryUseCase(exerciseSetRepository)
    }

    override val wallClock: com.example.domain.port.WallClock by lazy {
        com.example.domain.port.WallClock.System
    }

    override val monotonicClock: com.example.domain.port.MonotonicClock by lazy {
        com.example.infrastructure.clock.SystemMonotonicClock()
    }

    override val notificationScheduler: com.example.domain.port.NotificationScheduler by lazy {
        com.example.infrastructure.notification.AndroidNotificationScheduler(context)
    }

    override val restTimerManager: RestTimerManager by lazy {
        RestTimerManager(
            wallClock = wallClock,
            monotonicClock = monotonicClock,
            notificationScheduler = notificationScheduler
        )
    }
}
