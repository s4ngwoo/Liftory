package com.example.application.usecase.plan

import com.example.domain.model.EquipmentType
import com.example.domain.model.Exercise
import com.example.domain.model.ExercisePreset
import com.example.domain.model.MeasurementValue
import com.example.domain.model.RoutineTemplate
import com.example.domain.model.plan.PlannedExercise
import com.example.domain.model.plan.PlannedSet
import com.example.domain.model.plan.SessionPlan
import com.example.domain.port.IdGenerator
import com.example.domain.port.WallClock
import com.example.domain.repository.ExerciseRepository
import com.example.domain.repository.RoutineTemplateRepository
import com.example.domain.repository.SessionPlanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeRoutineRepository : RoutineTemplateRepository {
    val routines = mutableMapOf<String, RoutineTemplate>()

    override fun observeAll(): Flow<List<RoutineTemplate>> = flowOf(routines.values.toList())
    override suspend fun getById(id: String): RoutineTemplate? = routines[id]
    override suspend fun create(template: RoutineTemplate): Result<RoutineTemplate> {
        routines[template.id] = template
        return Result.success(template)
    }
    override suspend fun update(template: RoutineTemplate): Result<Unit> {
        routines[template.id] = template
        return Result.success(Unit)
    }
    override suspend fun delete(id: String): Result<Unit> {
        routines.remove(id)
        return Result.success(Unit)
    }
}

class FakePlanRepository : SessionPlanRepository {
    val plans = mutableMapOf<String, SessionPlan>()

    override suspend fun savePlan(plan: SessionPlan): Result<SessionPlan> {
        plans[plan.id] = plan
        return Result.success(plan)
    }
    override suspend fun getPlanById(id: String): SessionPlan? = plans[id]
    override fun observePlan(id: String): Flow<SessionPlan?> = flowOf(plans[id])
    override suspend fun deletePlan(id: String): Result<Unit> {
        plans.remove(id)
        return Result.success(Unit)
    }
}

class FakeSimpleExerciseRepository : ExerciseRepository {
    val exercises = mutableMapOf<String, Exercise>()
    override fun observeAll(): Flow<List<Exercise>> = flowOf(exercises.values.toList())
    override fun search(query: String): Flow<List<Exercise>> = flowOf(emptyList())
    override fun getExercisesByCategory(category: String): Flow<List<Exercise>> = flowOf(emptyList())
    override suspend fun getById(id: String): Result<Exercise> {
        val ex = exercises[id]
        return if (ex != null) Result.success(ex) else Result.failure(NoSuchElementException("Not found"))
    }
    override suspend fun create(exercise: Exercise): Result<Exercise> = Result.success(exercise)
    override suspend fun update(exercise: Exercise): Result<Unit> = Result.success(Unit)
    override suspend fun delete(id: String): Result<Unit> = Result.success(Unit)
}

class SessionPlanUseCasesTest {

    private lateinit var routineRepository: FakeRoutineRepository
    private lateinit var exerciseRepository: FakeSimpleExerciseRepository
    private lateinit var planRepository: FakePlanRepository
    private lateinit var createPlanFromRoutineUseCase: CreatePlanFromRoutineUseCase
    private lateinit var confirmPlanUseCase: ConfirmPlanUseCase

    private val fixedClock = object : WallClock {
        override fun nowMillis(): Long = 1000000L
    }

    private var idCounter = 1
    private val idGenerator = object : IdGenerator {
        override fun generate(): String = "id_${idCounter++}"
    }

    @Before
    fun setUp() {
        routineRepository = FakeRoutineRepository()
        exerciseRepository = FakeSimpleExerciseRepository()
        planRepository = FakePlanRepository()

        createPlanFromRoutineUseCase = CreatePlanFromRoutineUseCase(
            routineRepository = routineRepository,
            exerciseRepository = exerciseRepository,
            planRepository = planRepository,
            idGenerator = idGenerator,
            wallClock = fixedClock
        )

        confirmPlanUseCase = ConfirmPlanUseCase(
            planRepository = planRepository,
            wallClock = fixedClock
        )

        // Seed bench press
        exerciseRepository.exercises["ex_bench"] = Exercise(
            id = "ex_bench",
            name = "벤치프레스",
            muscleGroup = "Chest",
            equipmentType = EquipmentType.FREE_WEIGHT
        )
    }

    @Test
    fun `PLAN-01 creating plan from routine v1 keeps plan unchanged when routine is updated`() = runTest {
        // Given: Routine v1 with Bench Press 80kg x 10
        val routineV1 = RoutineTemplate(
            id = "routine_push",
            name = "Push Day",
            exercises = listOf(
                ExercisePreset(exerciseId = "ex_bench", defaultWeight = 80.0, defaultReps = 10, orderIndex = 0)
            ),
            createdAt = 1000L,
            updatedAt = 1000L
        )
        routineRepository.create(routineV1)

        // When: Plan created and confirmed from routine
        val planResult = createPlanFromRoutineUseCase("routine_push")
        assertTrue(planResult.isSuccess)
        val plan = planResult.getOrThrow()
        confirmPlanUseCase(plan.id)

        // Then: Update routine to 100kg x 5 (v2)
        val routineV2 = routineV1.copy(
            exercises = listOf(
                ExercisePreset(exerciseId = "ex_bench", defaultWeight = 100.0, defaultReps = 5, orderIndex = 0)
            ),
            updatedAt = 2000L
        )
        routineRepository.update(routineV2)

        // Assert: Confirmed plan still holds v1 target (80kg x 10)
        val retrievedPlan = planRepository.getPlanById(plan.id)
        assertNotNull(retrievedPlan)
        val benchExercise = retrievedPlan!!.exercises.first()
        val firstSet = benchExercise.plannedSets.first()
        val target = firstSet.targetMeasurement as MeasurementValue.WeightAndReps
        assertEquals(80.0, target.weightKg, 0.001)
        assertEquals(10, target.reps)
    }

    @Test
    fun `PLAN-02 creating draft plan does not start timer or create completed sets`() = runTest {
        val routine = RoutineTemplate(
            id = "routine_push",
            name = "Push Day",
            exercises = listOf(
                ExercisePreset(exerciseId = "ex_bench", defaultWeight = 80.0, defaultReps = 10, orderIndex = 0)
            ),
            createdAt = 1000L,
            updatedAt = 1000L
        )
        routineRepository.create(routine)

        val planResult = createPlanFromRoutineUseCase("routine_push")
        assertTrue(planResult.isSuccess)
        val plan = planResult.getOrThrow()

        // Draft plan is unconfirmed by default
        assertFalse(plan.isConfirmed)
        assertEquals("Push Day", plan.name)
        assertEquals(1, plan.exercises.size)
    }

    @Test
    fun `PLAN-04 same exercise at multiple positions maintains distinct plannedExercise IDs and ordering`() = runTest {
        // Bench Press as warm-up (position 0) and main workout (position 1)
        val routine = RoutineTemplate(
            id = "routine_double",
            name = "Double Bench Routine",
            exercises = listOf(
                ExercisePreset(exerciseId = "ex_bench", defaultWeight = 40.0, defaultReps = 15, orderIndex = 0),
                ExercisePreset(exerciseId = "ex_bench", defaultWeight = 80.0, defaultReps = 8, orderIndex = 1)
            ),
            createdAt = 1000L,
            updatedAt = 1000L
        )
        routineRepository.create(routine)

        val planResult = createPlanFromRoutineUseCase("routine_double")
        assertTrue(planResult.isSuccess)
        val plan = planResult.getOrThrow()

        assertEquals(2, plan.exercises.size)
        val firstItem = plan.exercises[0]
        val secondItem = plan.exercises[1]

        // Crucial invariant: exerciseId is identical ("ex_bench"), but plan item IDs are unique!
        assertEquals("ex_bench", firstItem.exerciseId)
        assertEquals("ex_bench", secondItem.exerciseId)
        assertTrue("Planned exercise IDs must be distinct", firstItem.id != secondItem.id)
        assertEquals(0, firstItem.orderIndex)
        assertEquals(1, secondItem.orderIndex)
    }

    @Test
    fun `PLAN-06 deleting origin routine allows confirmed plan to remain readable`() = runTest {
        val routine = RoutineTemplate(
            id = "routine_delete_me",
            name = "To Delete",
            exercises = listOf(
                ExercisePreset(exerciseId = "ex_bench", defaultWeight = 60.0, defaultReps = 10, orderIndex = 0)
            ),
            createdAt = 1000L,
            updatedAt = 1000L
        )
        routineRepository.create(routine)

        val plan = createPlanFromRoutineUseCase("routine_delete_me").getOrThrow()
        confirmPlanUseCase(plan.id)

        // Delete origin routine
        routineRepository.delete("routine_delete_me")

        // Plan is still fully readable and self-contained
        val savedPlan = planRepository.getPlanById(plan.id)
        assertNotNull(savedPlan)
        assertEquals("To Delete", savedPlan!!.name)
        assertEquals(1, savedPlan.exercises.size)
    }

    @Test
    fun `PLAN-03 saving and reloading mixed draft plan restores types, orders, and lateral sides`() = runTest {
        val updateDraftPlanUseCase = UpdateDraftPlanUseCase(planRepository, fixedClock)

        val planId = idGenerator.generate()
        val mixedExercises = listOf(
            PlannedExercise(
                id = "pe_stretch",
                exerciseId = "ex_hamstring",
                exerciseName = "햄스트링 스트레칭",
                orderIndex = 0,
                plannedSets = listOf(
                    PlannedSet(
                        id = "ps_stretch_l",
                        orderIndex = 0,
                        targetMeasurement = MeasurementValue.TimedHold(durationSeconds = 30, side = com.example.domain.model.BodySide.LEFT)
                    ),
                    PlannedSet(
                        id = "ps_stretch_r",
                        orderIndex = 1,
                        targetMeasurement = MeasurementValue.TimedHold(durationSeconds = 30, side = com.example.domain.model.BodySide.RIGHT)
                    )
                )
            ),
            PlannedExercise(
                id = "pe_strength",
                exerciseId = "ex_bench",
                exerciseName = "벤치프레스",
                orderIndex = 1,
                plannedSets = listOf(
                    PlannedSet(
                        id = "ps_bench_1",
                        orderIndex = 0,
                        targetMeasurement = MeasurementValue.WeightAndReps(weightKg = 80.0, reps = 10),
                        targetRestSeconds = 90
                    )
                )
            ),
            PlannedExercise(
                id = "pe_cardio",
                exerciseId = "ex_stairmaster",
                exerciseName = "천국의 계단",
                orderIndex = 2,
                plannedSets = listOf(
                    PlannedSet(
                        id = "ps_cardio_1",
                        orderIndex = 0,
                        targetMeasurement = MeasurementValue.TimeAndLevel(durationSeconds = 900, levelOrSpeed = 8.0)
                    )
                )
            )
        )

        val draftPlan = SessionPlan(
            id = planId,
            name = "Mixed Workout Plan",
            exercises = mixedExercises,
            isConfirmed = false,
            createdAt = 1000L,
            updatedAt = 1000L
        )

        updateDraftPlanUseCase(draftPlan)

        // Reload and assert
        val reloadedPlan = planRepository.getPlanById(planId)
        assertNotNull(reloadedPlan)
        assertEquals(3, reloadedPlan!!.exercises.size)

        // Exercise 0: Stretching with left and right holds
        val stretchEx = reloadedPlan.exercises[0]
        assertEquals(2, stretchEx.plannedSets.size)
        val leftHold = stretchEx.plannedSets[0].targetMeasurement as MeasurementValue.TimedHold
        val rightHold = stretchEx.plannedSets[1].targetMeasurement as MeasurementValue.TimedHold
        assertEquals(com.example.domain.model.BodySide.LEFT, leftHold.side)
        assertEquals(com.example.domain.model.BodySide.RIGHT, rightHold.side)

        // Exercise 1: Strength
        val strengthEx = reloadedPlan.exercises[1]
        val strengthTarget = strengthEx.plannedSets[0].targetMeasurement as MeasurementValue.WeightAndReps
        assertEquals(80.0, strengthTarget.weightKg, 0.001)

        // Exercise 2: Cardio
        val cardioEx = reloadedPlan.exercises[2]
        val cardioTarget = cardioEx.plannedSets[0].targetMeasurement as MeasurementValue.TimeAndLevel
        assertEquals(900, cardioTarget.durationSeconds)
        assertEquals(8.0, cardioTarget.levelOrSpeed, 0.001)
    }

    @Test
    fun `PLAN-05 creating plan from past history distinguishes origin and does not auto-mark sets as completed`() = runTest {
        val fakeSetRepo = object : com.example.domain.repository.ExerciseSetRepository {
            val sets = listOf(
                com.example.domain.model.ExerciseSet(
                    id = "past_set_1",
                    sessionId = "past_session_1",
                    exerciseId = "ex_bench",
                    weight = 85.0,
                    reps = 8,
                    orderIndex = 0,
                    isCompleted = true
                )
            )
            override suspend fun create(set: com.example.domain.model.ExerciseSet) = Result.success(set)
            override suspend fun update(set: com.example.domain.model.ExerciseSet) = Result.success(Unit)
            override suspend fun delete(id: String) = Result.success(Unit)
            override fun observeBySession(sessionId: String) = flowOf(sets)
            override suspend fun getBySession(sessionId: String) = sets
            override suspend fun getLastHistoryForExercise(exerciseId: String, currentSessionId: String?) = null
        }

        val createPlanFromHistoryUseCase = CreatePlanFromHistoryUseCase(
            exerciseSetRepository = fakeSetRepo,
            exerciseRepository = exerciseRepository,
            planRepository = planRepository,
            idGenerator = idGenerator,
            wallClock = fixedClock
        )

        val result = createPlanFromHistoryUseCase("past_session_1")
        assertTrue(result.isSuccess)
        val plan = result.getOrThrow()

        // Invariants:
        assertFalse("Plan should be unconfirmed draft", plan.isConfirmed)
        assertEquals(1, plan.exercises.size)
        val plannedSet = plan.exercises.first().plannedSets.first()

        // Target reflects past values (85kg x 8)
        val target = plannedSet.targetMeasurement as MeasurementValue.WeightAndReps
        assertEquals(85.0, target.weightKg, 0.001)
        assertEquals(8, target.reps)

        // But is NOT auto-counted as completed! (PLAN-05)
        assertFalse("Planned set from history must remain uncompleted in plan", plannedSet.isCompleted)
        assertTrue(plan.exercises.first().notes.contains("past_session_1"))
    }

    @Test
    fun `creating plan from missing routine fails closed`() = runTest {
        val result = createPlanFromRoutineUseCase("routine_missing")
        assertTrue(result.isFailure)
        assertTrue(planRepository.plans.isEmpty())
    }

    @Test
    fun `missing exercise in routine falls back to WeightAndReps without aborting the plan`() = runTest {
        val routine = RoutineTemplate(
            id = "routine_orphan",
            name = "Orphan Exercise",
            exercises = listOf(
                ExercisePreset(exerciseId = "ex_unknown", defaultWeight = 40.0, defaultReps = 12, orderIndex = 0)
            ),
            createdAt = 1000L,
            updatedAt = 1000L
        )
        routineRepository.create(routine)

        val plan = createPlanFromRoutineUseCase("routine_orphan").getOrThrow()
        val target = plan.exercises.single().plannedSets.single().targetMeasurement as MeasurementValue.WeightAndReps
        assertEquals("Exercise ex_unknown", plan.exercises.single().exerciseName)
        assertEquals(40.0, target.weightKg, 0.001)
        assertEquals(12, target.reps)
        assertFalse(plan.isConfirmed)
    }

    @Test
    fun `known cardio preset maps minutes to TimeAndLevel seconds`() = runTest {
        exerciseRepository.exercises["ex_treadmill"] = Exercise(
            id = "ex_treadmill",
            name = "트레드밀",
            muscleGroup = "Cardio",
            equipmentType = EquipmentType.CARDIO
        )
        val routine = RoutineTemplate(
            id = "routine_cardio",
            name = "Cardio Day",
            exercises = listOf(
                ExercisePreset(exerciseId = "ex_treadmill", defaultWeight = 6.5, defaultReps = 20, orderIndex = 0)
            ),
            createdAt = 1000L,
            updatedAt = 1000L
        )
        routineRepository.create(routine)

        val plan = createPlanFromRoutineUseCase("routine_cardio").getOrThrow()
        val target = plan.exercises.single().plannedSets.single().targetMeasurement as MeasurementValue.TimeAndLevel
        assertEquals(20 * 60, target.durationSeconds)
        assertEquals(6.5, target.levelOrSpeed, 0.001)
    }

    @Test
    fun `unknown cardio preset preserves raw values as LegacyUnknown`() = runTest {
        exerciseRepository.exercises["custom_spin"] = Exercise(
            id = "custom_spin",
            name = "Custom Spinner",
            muscleGroup = "Cardio",
            equipmentType = EquipmentType.CARDIO
        )
        val routine = RoutineTemplate(
            id = "routine_unknown_cardio",
            name = "Ambiguous Cardio",
            exercises = listOf(
                ExercisePreset(exerciseId = "custom_spin", defaultWeight = 12.0, defaultReps = 30, orderIndex = 0)
            ),
            createdAt = 1000L,
            updatedAt = 1000L
        )
        routineRepository.create(routine)

        val plan = createPlanFromRoutineUseCase("routine_unknown_cardio").getOrThrow()
        val target = plan.exercises.single().plannedSets.single().targetMeasurement as MeasurementValue.LegacyUnknown
        assertEquals(12.0, target.rawValue1, 0.001)
        assertEquals(30.0, target.rawValue2, 0.001)
    }

    @Test
    fun `confirming a missing plan fails without writing a new plan`() = runTest {
        val result = confirmPlanUseCase("plan_missing")
        assertTrue(result.isFailure)
        assertTrue(planRepository.plans.isEmpty())
    }
}
