package com.example.domain.cohort

data class CohortDistributionData(
    val cohortCategory: String,
    val sampleCount: Int,
    val p25: Double,
    val p50: Double,
    val p75: Double,
    val isAggregatedQuantileOnly: Boolean = true
)

data class CohortEvaluationResult(
    val isExposed: Boolean,
    val message: String,
    val disclaimerText: String
)

class CohortService {
    private val consentMap = mutableMapOf<String, Boolean>()

    fun setConsent(userId: String, optedIn: Boolean) {
        consentMap[userId] = optedIn
    }

    fun canUploadCohortPayload(userId: String): Boolean {
        return consentMap[userId] == true
    }

    fun isPersonalAnalyticsEnabled(userId: String): Boolean {
        // Always enabled regardless of cohort consent
        return true
    }

    companion object {
        fun buildAnonymizedCohortPayload(
            userId: String,
            exerciseCategory: String,
            calculatedSlope: Double,
            rawSets: List<String>,
            userEmail: String
        ): String {
            // Strips userId, userEmail, rawSets
            return """{"category":"$exerciseCategory","slope":$calculatedSlope}"""
        }

        fun evaluateCohortDistribution(sampleCount: Int, kThreshold: Int = 10): CohortEvaluationResult {
            val disclaimer = "본 코호트 분포는 관측된 통계 요약일 뿐이며, 특정 루틴의 인과적 우수성이나 최적 루틴을 의미하지 않습니다."
            return if (sampleCount < kThreshold) {
                CohortEvaluationResult(
                    isExposed = false,
                    message = "표본 수 부족 (최소 ${kThreshold}명 필요)",
                    disclaimerText = disclaimer
                )
            } else {
                CohortEvaluationResult(
                    isExposed = true,
                    message = "정상 노출",
                    disclaimerText = disclaimer
                )
            }
        }
    }
}
