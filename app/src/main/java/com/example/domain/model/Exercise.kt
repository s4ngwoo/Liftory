package com.example.domain.model

enum class EquipmentType {
    FREE_WEIGHT, // 프리웨이트 (바벨, 덤벨, 맨몸 등)
    MACHINE      // 머신운동 (핀로드, 플레이트 로드 등)
}

/**
 * Represents a strength training exercise (e.g., Squat, Bench Press, Lat Pulldown).
 */
data class Exercise(
    val id: String,
    val name: String,
    val isCustom: Boolean = false,
    val muscleGroup: String = "All",
    val equipmentType: EquipmentType = EquipmentType.FREE_WEIGHT,
    val machineBrand: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
