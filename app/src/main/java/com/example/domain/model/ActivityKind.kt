package com.example.domain.model

/**
 * Activity category representing the fundamental nature of an exercise.
 */
enum class ActivityKind {
    STRENGTH,   // 웨이트 트레이닝 (중량/반복 중심)
    CARDIO,     // 심폐 지구력 유산소 (시간, 속도, 거리, 레벨 중심)
    STRETCHING, // 스트레칭 및 가동성 (시간, 좌/우 중심)
    UNKNOWN     // 미확인/레거시 활동
}
