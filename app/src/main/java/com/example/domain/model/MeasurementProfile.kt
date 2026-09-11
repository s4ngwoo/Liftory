package com.example.domain.model

/**
 * Defines how an exercise set is parameterized and measured.
 */
enum class MeasurementProfile {
    WEIGHT_AND_REPS,       // 중량(kg) + 반복 횟수 (표준 근력)
    BODYWEIGHT_PLUS_REPS,  // 맨몸/체중 + 추가/보조 중량 + 횟수 (풀업, 딥스)
    TIME_AND_LEVEL,        // 지속 시간(초) + 레벨/속도 (천국의 계단, 실내 사이클)
    TIME_AND_DISTANCE,     // 지속 시간(초) + 거리(m) + 경사도 (러닝머신, 야외 러닝)
    TIMED_HOLD,            // 좌/우 또는 단일 자세 유지 시간(초) (정적 스트레칭, 플랭크)
    LEGACY_UNKNOWN         // 레거시 미확인 단위
}

/**
 * Body side for unilateral exercises and stretching.
 */
enum class BodySide {
    LEFT,
    RIGHT,
    BOTH,
    NONE
}
