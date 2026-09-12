package com.example.domain.gym

enum class EquipmentStatus {
    AVAILABLE,
    OUT_OF_ORDER,
    UNCONFIRMED,
    DECOMMISSIONED
}

data class EquipmentModel(
    val id: String,
    val manufacturer: String,
    val name: String
)

data class GymEquipment(
    val id: String,
    val gymId: String,
    val modelId: String,
    val serialNumber: String? = null,
    val quantity: Int?,
    val status: EquipmentStatus = EquipmentStatus.AVAILABLE
)

data class GymFulfillmentReport(
    val availableModels: List<String>,
    val unavailableModels: List<String>,
    val unknownModels: List<String>
)

data class GymHistoryLink(
    val sessionId: String,
    val gymId: String,
    val gymName: String,
    val equipmentModelId: String,
    val performedAtEpochMs: Long
)

data class EquipmentSubstitutionPlanChange(
    val sessionId: String,
    val originalModelId: String,
    val substituteModelId: String,
    val reason: String
)

/**
 * Service and pure domain logic for Gym Equipment catalogs (N13).
 */
class GymCatalogService {
    private val gyms = mutableMapOf<String, String>()

    fun deleteGym(gymId: String) {
        gyms.remove(gymId)
    }

    companion object {
        fun countUniqueEquipmentAssets(items: List<GymEquipment>): Int {
            // Deduplicate by serial number if present, otherwise by ID
            val unique = items.distinctBy { it.serialNumber ?: it.id }
            return unique.sumOf { it.quantity ?: 0 }
        }

        fun evaluateRoutineFulfillment(
            requiredModelIds: List<String>,
            gymEquipments: List<GymEquipment>
        ): GymFulfillmentReport {
            val byModel = gymEquipments.groupBy { it.modelId }
            val available = mutableListOf<String>()
            val unavailable = mutableListOf<String>()
            val unknown = mutableListOf<String>()

            for (modelId in requiredModelIds) {
                val matching = byModel[modelId]
                if (matching == null) {
                    unknown.add(modelId)
                    continue
                }
                val hasAvailable = matching.any { (it.quantity ?: 0) > 0 && it.status == EquipmentStatus.AVAILABLE }
                val hasOutOfOrderOrZero = matching.any { it.quantity == 0 || it.status == EquipmentStatus.OUT_OF_ORDER }
                val hasUnknown = matching.any { it.quantity == null || it.status == EquipmentStatus.UNCONFIRMED }

                when {
                    hasAvailable -> available.add(modelId)
                    hasOutOfOrderOrZero -> unavailable.add(modelId)
                    hasUnknown -> unknown.add(modelId)
                    else -> unknown.add(modelId)
                }
            }

            return GymFulfillmentReport(
                availableModels = available,
                unavailableModels = unavailable,
                unknownModels = unknown
            )
        }

        fun createEquipmentSubstitutionChange(
            sessionId: String,
            originalModelId: String,
            substituteModelId: String,
            reason: String
        ): EquipmentSubstitutionPlanChange {
            return EquipmentSubstitutionPlanChange(
                sessionId = sessionId,
                originalModelId = originalModelId,
                substituteModelId = substituteModelId,
                reason = reason
            )
        }
    }
}
