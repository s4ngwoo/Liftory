package com.example.domain.repository

import com.example.domain.model.PersonalRecord
import com.example.domain.model.WorkoutVolume
import kotlinx.coroutines.flow.Flow

interface StatisticsRepository {
    fun observeVolumeByPeriod(startDate: Long, endDate: Long): Flow<List<WorkoutVolume>>
    fun observePersonalRecords(): Flow<List<PersonalRecord>>
}
