package com.example.domain.exception

import com.example.domain.model.WorkoutSession

/**
 * Thrown or returned when attempting to start a new workout session while another
 * session is already active (endTime == null).
 */
class ActiveSessionAlreadyExistsException(
    val activeSession: WorkoutSession
) : IllegalStateException("이미 진행 중인 운동 세션이 있습니다: ${activeSession.id} (${activeSession.notes})")
