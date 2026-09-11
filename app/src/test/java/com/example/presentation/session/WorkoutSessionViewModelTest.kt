package com.example.presentation.session

import app.cash.turbine.test
import com.example.application.usecase.session.CreateWorkoutSessionUseCase
import com.example.application.usecase.session.DeleteWorkoutSessionUseCase
import com.example.application.usecase.session.ObserveWorkoutSessionsUseCase
import com.example.application.usecase.session.UpdateWorkoutSessionUseCase
import com.example.application.usecase.set.AddExerciseSetUseCase
import com.example.application.usecase.set.ObserveExerciseSetsUseCase
import com.example.domain.model.ExerciseSet
import com.example.domain.model.WorkoutSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutSessionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: WorkoutSessionViewModel

    private lateinit var fakeSessionRepository: FakeSessionRepository
    private lateinit var observeSessionsUseCase: ObserveWorkoutSessionsUseCase
    private lateinit var observeExerciseSetsUseCase: ObserveExerciseSetsUseCase
    private lateinit var createSessionUseCase: CreateWorkoutSessionUseCase
    private lateinit var addExerciseSetUseCase: AddExerciseSetUseCase
    private lateinit var updateWorkoutSessionUseCase: UpdateWorkoutSessionUseCase
    private lateinit var deleteWorkoutSessionUseCase: DeleteWorkoutSessionUseCase
    private lateinit var getWorkoutSessionUseCase: com.example.application.usecase.session.GetWorkoutSessionUseCase

    private val testSessionsFlow = MutableStateFlow<List<WorkoutSession>>(emptyList())
    private val testSetsFlow = MutableStateFlow<List<ExerciseSet>>(emptyList())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        fakeSessionRepository = FakeSessionRepository()

        observeSessionsUseCase = object : ObserveWorkoutSessionsUseCase(fakeSessionRepository) {
            override operator fun invoke(): Flow<List<WorkoutSession>> = testSessionsFlow
        }

        observeExerciseSetsUseCase = object : ObserveExerciseSetsUseCase(FakeSetRepository()) {
            override operator fun invoke(sessionId: String): Flow<List<ExerciseSet>> = testSetsFlow
        }

        createSessionUseCase = CreateWorkoutSessionUseCase(fakeSessionRepository)
        addExerciseSetUseCase = AddExerciseSetUseCase(FakeSetRepository(), fakeSessionRepository, FakeTransactionProvider())
        updateWorkoutSessionUseCase = UpdateWorkoutSessionUseCase(fakeSessionRepository)
        deleteWorkoutSessionUseCase = DeleteWorkoutSessionUseCase(fakeSessionRepository)
        getWorkoutSessionUseCase = com.example.application.usecase.session.GetWorkoutSessionUseCase(fakeSessionRepository)

        viewModel = WorkoutSessionViewModel(
            observeSessionsUseCase,
            observeExerciseSetsUseCase,
            createSessionUseCase,
            addExerciseSetUseCase,
            updateWorkoutSessionUseCase,
            deleteWorkoutSessionUseCase,
            getWorkoutSessionUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when session is selected, currentSessionSets emits correct sets via flatMapLatest`() = runTest {
        val sessionId = "session_1"
        val mockSets = listOf(
            ExerciseSet(id = "set1", sessionId = sessionId, exerciseId = "ex1", weight = 100.0, reps = 5, rpe = null, restSeconds = null, orderIndex = 0)
        )

        testSetsFlow.value = mockSets

        viewModel.currentSessionSets.test {
            assertEquals(emptyList<ExerciseSet>(), awaitItem())

            viewModel.selectSession(sessionId)

            val newSets = awaitItem()
            assertEquals(1, newSets.size)
            assertEquals("set1", newSets[0].id)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when updateSessionNotes is called, repository update is triggered`() = runTest {
        val session = WorkoutSession(id = "sess1", startTime = 1000L, notes = "Old Note")
        fakeSessionRepository.sessions.add(session)
        testSessionsFlow.value = listOf(session)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updateSessionNotes("sess1", "New Note")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("New Note", fakeSessionRepository.updatedSession?.notes)
    }

    @Test
    fun `when deleteSession is called for selected session, selectedSessionId is cleared`() = runTest {
        viewModel.selectSession("sess1")
        assertEquals("sess1", viewModel.selectedSessionId.value)

        var callbackCalled = false
        viewModel.deleteSession("sess1") {
            callbackCalled = true
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.selectedSessionId.value)
        assertEquals(true, callbackCalled)
    }

    @Test
    fun `loadHistoryForExercise should populate exerciseHistoryMap`() = runTest {
        val exerciseId = "bench_press"
        val expectedRecord = com.example.domain.model.ExerciseHistoryRecord(
            exerciseId = exerciseId,
            sessionId = "sess_old",
            sessionDate = 1700000000000L,
            sets = listOf(
                com.example.domain.model.ExerciseSetSummary(setNumber = 1, weight = 100.0, reps = 5, rpe = 8.0)
            )
        )

        val fakeSetRepo = object : FakeSetRepository() {
            override suspend fun getLastHistoryForExercise(exerciseId: String, currentSessionId: String?): com.example.domain.model.ExerciseHistoryRecord? {
                return if (exerciseId == "bench_press") expectedRecord else null
            }
        }
        val historyUseCase = com.example.application.usecase.set.GetLastExerciseHistoryUseCase(fakeSetRepo)

        val vm = WorkoutSessionViewModel(
            observeSessionsUseCase,
            observeExerciseSetsUseCase,
            createSessionUseCase,
            addExerciseSetUseCase,
            updateWorkoutSessionUseCase,
            deleteWorkoutSessionUseCase,
            getWorkoutSessionUseCase,
            getLastExerciseHistoryUseCase = historyUseCase
        )

        vm.loadHistoryForExercise(exerciseId)
        testDispatcher.scheduler.advanceUntilIdle()

        val history = vm.exerciseHistoryMap.value[exerciseId]
        org.junit.Assert.assertNotNull(history)
        assertEquals("sess_old", history?.sessionId)
        assertEquals(100.0, history?.sets?.first()?.weight ?: 0.0, 0.01)
    }

    @Test
    fun `createNewSession when active session exists triggers onActiveConflict callback`() = runTest {
        val existingSession = WorkoutSession(id = "active_1", startTime = 1000L, endTime = null, notes = "Ongoing Session")
        fakeSessionRepository.sessions.add(existingSession)
        testSessionsFlow.value = listOf(existingSession)
        testDispatcher.scheduler.advanceUntilIdle()

        var conflictSession: WorkoutSession? = null
        var createdSessionId: String? = null

        viewModel.createNewSession(
            notes = "New Session",
            finishExistingActive = false,
            onCreated = { createdSessionId = it },
            onActiveConflict = { conflictSession = it }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        org.junit.Assert.assertNull(createdSessionId)
        org.junit.Assert.assertNotNull(conflictSession)
        assertEquals("active_1", conflictSession?.id)
    }

    @Test
    fun `createNewSession with finishExistingActive true ends previous session and creates new`() = runTest {
        val existingSession = WorkoutSession(id = "active_1", startTime = 1000L, endTime = null, notes = "Ongoing Session")
        fakeSessionRepository.sessions.add(existingSession)
        testSessionsFlow.value = listOf(existingSession)
        testDispatcher.scheduler.advanceUntilIdle()

        var createdSessionId: String? = null
        viewModel.createNewSession(
            notes = "New Session",
            finishExistingActive = true,
            onCreated = { createdSessionId = it }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        org.junit.Assert.assertNotNull(createdSessionId)
        org.junit.Assert.assertNotNull(fakeSessionRepository.updatedSession?.endTime)
        assertEquals("active_1", fakeSessionRepository.updatedSession?.id)
    }
}

