package com.example.application.usecase.routine

import com.example.domain.model.ExercisePreset
import com.example.domain.model.RoutineTemplate
import com.example.domain.repository.RoutineTemplateRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ObserveRoutineTemplatesUseCase(
    private val repository: RoutineTemplateRepository
) {
    operator fun invoke(): Flow<List<RoutineTemplate>> {
        return repository.observeAll()
    }
}

class CreateRoutineTemplateUseCase(
    private val repository: RoutineTemplateRepository
) {
    suspend operator fun invoke(name: String, presets: List<ExercisePreset>): Result<RoutineTemplate> {
        val template = RoutineTemplate(
            id = UUID.randomUUID().toString(),
            name = name,
            exercises = presets
        )
        return repository.create(template)
    }
}
