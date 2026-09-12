package com.example.domain.operations

enum class OperatorRole {
    OWNER,
    MANAGER,
    STAFF
}

data class GymOperator(
    val operatorId: String,
    val role: OperatorRole,
    val facilityIds: Set<String>,
    val isActive: Boolean = true
)

data class CheckInRecord(
    val id: String,
    val facilityId: String,
    val userId: String,
    val timestampEpochMs: Long,
    val checkOutTimestampEpochMs: Long? = null
)

data class CheckInProcessResult(
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

data class OccupancyReport(
    val confirmedVisitsCount: Int,
    val estimatedCurrentOccupancy: Int,
    val missingCheckoutCount: Int
)

data class EquipmentFrequencyReport(
    val observedAppFrequency: Int,
    val estimatedMarketSharePercent: Double?,
    val metricCaveat: String
)

data class OperatorDashboardSummary(
    val facilityId: String,
    val totalVisitsToday: Int,
    val personalRpeLogsCount: Int = 0,
    val personalNotesPayload: String? = null
)

class FacilityOperationsService {
    private val operators = mutableMapOf<String, GymOperator>()
    private val processedTokens = mutableSetOf<String>()

    fun registerOperator(operator: GymOperator) {
        operators[operator.operatorId] = operator
    }

    fun revokeOperator(operatorId: String) {
        val existing = operators[operatorId] ?: return
        operators[operatorId] = existing.copy(isActive = false)
    }

    fun verifyFacilityAccess(operator: GymOperator, facilityId: String): Boolean {
        if (!operator.isActive) return false
        return operator.facilityIds.contains(facilityId)
    }

    fun verifyFacilityAccessById(operatorId: String, facilityId: String): Boolean {
        val op = operators[operatorId] ?: return false
        return verifyFacilityAccess(op, facilityId)
    }

    fun processCheckIn(
        facilityId: String,
        userId: String,
        token: String,
        isTokenExpired: Boolean,
        timestampEpochMs: Long
    ): CheckInProcessResult {
        if (isTokenExpired) {
            return CheckInProcessResult(false, "Expired check-in token")
        }
        if (processedTokens.contains(token)) {
            return CheckInProcessResult(false, "Duplicate token reuse")
        }
        processedTokens.add(token)
        return CheckInProcessResult(true)
    }

    companion object {
        fun resolveOccupancyAndVisits(checkIns: List<CheckInRecord>): OccupancyReport {
            val totalVisits = checkIns.size
            val missingCheckout = checkIns.count { it.checkOutTimestampEpochMs == null }
            return OccupancyReport(
                confirmedVisitsCount = totalVisits,
                estimatedCurrentOccupancy = missingCheckout,
                missingCheckoutCount = missingCheckout
            )
        }

        fun aggregateEquipmentFrequency(
            appLoggedSessionsCount: Int,
            totalMembers: Int?
        ): EquipmentFrequencyReport {
            val share = if (totalMembers != null && totalMembers > 0) {
                (appLoggedSessionsCount.toDouble() / totalMembers) * 100.0
            } else null
            return EquipmentFrequencyReport(
                observedAppFrequency = appLoggedSessionsCount,
                estimatedMarketSharePercent = share,
                metricCaveat = "앱 기록 기준 기구 이용 빈도로, 미기록 회원의 실제 점유율을 단정하지 않습니다."
            )
        }

        fun getDashboardSummary(operator: GymOperator, facilityId: String): OperatorDashboardSummary {
            return OperatorDashboardSummary(
                facilityId = facilityId,
                totalVisitsToday = 0,
                personalRpeLogsCount = 0,
                personalNotesPayload = null
            )
        }
    }
}
