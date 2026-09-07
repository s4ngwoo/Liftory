package com.example.application.usecase.statistics

import com.example.domain.model.PersonalRecord
import com.example.domain.model.WorkoutVolume
import com.example.domain.repository.StatisticsRepository
import kotlinx.coroutines.flow.Flow

class CalculateWorkoutVolumeUseCase(
    private val statisticsRepository: StatisticsRepository
) {
    operator fun invoke(startDate: Long, endDate: Long): Flow<List<WorkoutVolume>> {
        return statisticsRepository.observeVolumeByPeriod(startDate, endDate)
    }
}

class CalculatePersonalRecordsUseCase(
    private val statisticsRepository: StatisticsRepository
) {
    operator fun invoke(): Flow<List<PersonalRecord>> {
        return statisticsRepository.observePersonalRecords()
    }
}
