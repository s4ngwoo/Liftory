package com.example.domain.cohort

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * N16 Opt-in Cohort Distribution Tests (COHORT-01~COHORT-07).
 */
class CohortDistributionTest {

    @Test
    fun `COHORT-01 opt-out halts new cohort uploads while preserving personal offline analytics`() {
        val service = CohortService()
        service.setConsent(userId = "user_1", optedIn = false)

        val canUpload = service.canUploadCohortPayload("user_1")
        assertFalse("Opted-out user must not upload any cohort payload", canUpload)

        // Personal analytics remains functional independently
        val personalDataAvailable = service.isPersonalAnalyticsEnabled("user_1")
        assertTrue("Personal analytics must remain functional regardless of cohort opt-out", personalDataAvailable)
    }

    @Test
    fun `COHORT-02 cohort upload payload strips raw set logs and personal identifiers`() {
        val payload = CohortService.buildAnonymizedCohortPayload(
            userId = "user_secret_123",
            exerciseCategory = "Bench Press",
            calculatedSlope = 1.2,
            rawSets = listOf("Set 1: 100kg x 10 with personal note: gym was crowded"),
            userEmail = "secret@example.com"
        )

        assertFalse("Payload must not contain user ID", payload.contains("user_secret_123"))
        assertFalse("Payload must not contain user email", payload.contains("secret@example.com"))
        assertFalse("Payload must not contain raw set notes", payload.contains("gym was crowded"))
        assertTrue("Payload retains only aggregated slope and category", payload.contains("Bench Press") && payload.contains("1.2"))
    }

    @Test
    fun `COHORT-03 query with sample count below threshold k rejects distribution exposure`() {
        val kThreshold = 10

        // k - 1: rejected
        val belowResult = CohortService.evaluateCohortDistribution(sampleCount = 9, kThreshold = kThreshold)
        assertFalse("Below threshold k-1 must not expose distribution", belowResult.isExposed)
        assertEquals("표본 수 부족 (최소 10명 필요)", belowResult.message)

        // k: allowed
        val exactResult = CohortService.evaluateCohortDistribution(sampleCount = 10, kThreshold = kThreshold)
        assertTrue(exactResult.isExposed)

        // k + 1: allowed
        val aboveResult = CohortService.evaluateCohortDistribution(sampleCount = 11, kThreshold = kThreshold)
        assertTrue(aboveResult.isExposed)
    }

    @Test
    fun `COHORT-05 UI distribution model returns percentiles without faking individual scatter points`() {
        val distribution = CohortDistributionData(
            cohortCategory = "Squat Heavy",
            sampleCount = 45,
            p25 = 110.0,
            p50 = 130.0,
            p75 = 150.0
        )

        assertEquals(110.0, distribution.p25, 0.001)
        assertEquals(130.0, distribution.p50, 0.001)
        assertEquals(150.0, distribution.p75, 0.001)
        // No fake individual data points list exists
        assertTrue(distribution.isAggregatedQuantileOnly)
    }

    @Test
    fun `COHORT-07 cohort presentation disclaimer rejects causal superiority and optimal routine claims`() {
        val result = CohortService.evaluateCohortDistribution(sampleCount = 50, kThreshold = 10)
        assertTrue(result.isExposed)

        val disclaimer = result.disclaimerText
        assertNotNull(disclaimer)
        assertTrue("Disclaimer must clarify observational distribution without claiming causal superiority",
            disclaimer.contains("인과적 우수성이나 최적 루틴을 의미하지 않습니다"))
    }
}
