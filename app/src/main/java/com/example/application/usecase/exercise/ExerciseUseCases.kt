package com.example.application.usecase.exercise

import com.example.domain.model.Exercise
import com.example.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ObserveExercisesUseCase(
    private val exerciseRepository: ExerciseRepository
) {
    operator fun invoke(): Flow<List<Exercise>> {
        return exerciseRepository.observeAll()
    }
}

class CreateExerciseUseCase(
    private val exerciseRepository: ExerciseRepository
) {
    suspend operator fun invoke(
        name: String,
        muscleGroup: String = "All",
        equipmentType: com.example.domain.model.EquipmentType = com.example.domain.model.EquipmentType.FREE_WEIGHT,
        machineBrand: String? = null
    ): Result<Exercise> {
        val exercise = Exercise(
            id = UUID.randomUUID().toString(),
            name = name,
            isCustom = true,
            muscleGroup = muscleGroup,
            equipmentType = equipmentType,
            machineBrand = machineBrand
        )
        return exerciseRepository.create(exercise)
    }
}

class SearchExercisesUseCase(
    private val exerciseRepository: ExerciseRepository
) {
    operator fun invoke(query: String): Flow<List<Exercise>> {
        return exerciseRepository.search(query)
    }
}
