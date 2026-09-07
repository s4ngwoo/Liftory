package com.example.presentation.session

import app.cash.turbine.test
import com.example.application.usecase.session.CreateWorkoutSessionUseCase
import com.example.application.usecase.session.ObserveWorkoutSessionsUseCase
import com.example.application.usecase.set.AddExerciseSetUseCase
import com.example.application.usecase.set.ObserveExerciseSetsUseCase
import com.example.domain.model.ExerciseSet
import com.example.domain.model.WorkoutSession
import com.example.domain.repository.ExerciseSetRepository
import com.example.domain.repository.WorkoutSessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutSessionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: WorkoutSessionViewModel

    private lateinit var observeSessionsUseCase: ObserveWorkoutSessionsUseCase
    private lateinit var observeExerciseSetsUseCase: ObserveExerciseSetsUseCase
    private lateinit var createSessionUseCase: CreateWorkoutSessionUseCase
    private lateinit var addExerciseSetUseCase: AddExerciseSetUseCase

    private val testSetsFlow = kotlinx.coroutines.flow.MutableStateFlow<List<ExerciseSet>>(emptyList())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        observeSessionsUseCase = object : ObserveWorkoutSessionsUseCase(FakeSessionRepository()) {
            override operator fun invoke(): Flow<List<WorkoutSession>> = flowOf(emptyList())
        }
        
        observeExerciseSetsUseCase = object : ObserveExerciseSetsUseCase(FakeSetRepository()) {
            override operator fun invoke(sessionId: String): Flow<List<ExerciseSet>> = testSetsFlow
        }
        
        createSessionUseCase = CreateWorkoutSessionUseCase(FakeSessionRepository())
        addExerciseSetUseCase = AddExerciseSetUseCase(FakeSetRepository(), FakeSessionRepository(), FakeTransactionProvider())

        viewModel = WorkoutSessionViewModel(
            observeSessionsUseCase,
            observeExerciseSetsUseCase,
            createSessionUseCase,
            addExerciseSetUseCase
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
}
