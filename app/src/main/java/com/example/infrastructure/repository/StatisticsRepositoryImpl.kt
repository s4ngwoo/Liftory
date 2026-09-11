package com.example.infrastructure.repository

import com.example.domain.model.PersonalRecord
import com.example.domain.model.WorkoutVolume
import com.example.domain.repository.StatisticsRepository
import com.example.infrastructure.db.dao.ExerciseSetDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StatisticsRepositoryImpl(
    private val exerciseSetDao: ExerciseSetDao,
    private val ioDispatcher: CoroutineDispatcher
) : StatisticsRepository {

    override fun observeVolumeByPeriod(startDate: Long, endDate: Long): Flow<List<WorkoutVolume>> {
        return exerciseSetDao.observeVolumeByPeriod(startDate, endDate).map { tuples ->
            tuples.map { tuple ->
                WorkoutVolume(
                    dateMillis = tuple.startTime,
                    totalVolume = tuple.totalVolume,
                    cardioDurationMinutes = tuple.cardioMinutes
                )
            }
        }
    }

    override fun observePersonalRecords(): Flow<List<PersonalRecord>> {
        return exerciseSetDao.observePersonalRecords().map { tuples ->
            tuples.map { tuple ->
                PersonalRecord(
                    exerciseId = tuple.exerciseId,
                    exerciseName = tuple.exerciseName,
                    isCardio = tuple.equipmentType.equals("CARDIO", ignoreCase = true),
                    maxWeight = tuple.maxWeight,
                    maxCardioLevel = tuple.maxCardioLevel,
                    maxCardioMinutes = tuple.maxCardioMinutes,
                    achievedAt = tuple.achievedAt
                )
            }
        }
    }
}
