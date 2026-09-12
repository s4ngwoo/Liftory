package com.example.domain.operations

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * N14 Facility Operations, Check-in, and Tenant Isolation Tests (OPS-01~OPS-07).
 */
class FacilityOperationsTest {

    @Test
    fun `OPS-01 tenant isolation blocks facility A manager from reading or writing facility B data`() {
        val operatorA = GymOperator(operatorId = "op_1", role = OperatorRole.MANAGER, facilityIds = setOf("fac_A"), isActive = true)
        val service = FacilityOperationsService()

        val canAccessA = service.verifyFacilityAccess(operatorA, "fac_A")
        val canAccessB = service.verifyFacilityAccess(operatorA, "fac_B")

        assertTrue("Manager of facility A can access facility A", canAccessA)
        assertFalse("Tenant isolation: Manager of facility A must be rejected for facility B", canAccessB)
    }

    @Test
    fun `OPS-02 revoked operator token is immediately rejected on next operation`() {
        val operator = GymOperator(operatorId = "op_2", role = OperatorRole.STAFF, facilityIds = setOf("fac_A"), isActive = true)
        val service = FacilityOperationsService()
        service.registerOperator(operator)

        // Revoke
        service.revokeOperator("op_2")

        val canAccess = service.verifyFacilityAccessById("op_2", "fac_A")
        assertFalse("Revoked operator must be blocked from accessing facility", canAccess)
    }

    @Test
    fun `OPS-03 check-in rejects expired QR tokens and duplicate check-in commands`() {
        val service = FacilityOperationsService()

        // Expired token rejected
        val expiredResult = service.processCheckIn(
            facilityId = "fac_A",
            userId = "user_1",
            token = "qr_old",
            isTokenExpired = true,
            timestampEpochMs = 1000L
        )
        assertFalse(expiredResult.isSuccess)
        assertEquals("Expired check-in token", expiredResult.errorMessage)

        // Valid check-in succeeded
        val validResult = service.processCheckIn(
            facilityId = "fac_A",
            userId = "user_1",
            token = "qr_fresh",
            isTokenExpired = false,
            timestampEpochMs = 1000L
        )
        assertTrue(validResult.isSuccess)

        // Immediate duplicate token reuse rejected
        val duplicateResult = service.processCheckIn(
            facilityId = "fac_A",
            userId = "user_1",
            token = "qr_fresh",
            isTokenExpired = false,
            timestampEpochMs = 1005L
        )
        assertFalse("Duplicate token reuse must be rejected", duplicateResult.isSuccess)
    }

    @Test
    fun `OPS-04 missing checkout counts confirmed visit while classifying current occupancy as estimated`() {
        val checkIns = listOf(
            CheckInRecord("c1", "fac_A", "u1", 1000L, checkOutTimestampEpochMs = 2000L), // complete
            CheckInRecord("c2", "fac_A", "u2", 1000L, checkOutTimestampEpochMs = null)    // missing checkout!
        )

        val report = FacilityOperationsService.resolveOccupancyAndVisits(checkIns)

        assertEquals("Total visits must reflect both entries", 2, report.confirmedVisitsCount)
        assertEquals("Currently checked in or missing checkout counted as estimated", 1, report.estimatedCurrentOccupancy)
        assertEquals(1, report.missingCheckoutCount)
    }

    @Test
    fun `OPS-06 app equipment logs are reported as observed frequency without asserting total gym market share`() {
        val report = FacilityOperationsService.aggregateEquipmentFrequency(
            appLoggedSessionsCount = 10,
            totalMembers = null
        )

        assertEquals(10, report.observedAppFrequency)
        assertNull("Cannot claim total gym market share when total members or unlogged entries are unknown", report.estimatedMarketSharePercent)
        assertTrue(report.metricCaveat.contains("앱 기록 기준"))
    }

    @Test
    fun `OPS-07 operator dashboard summary strictly strips personal RPE and private notes`() {
        val summary = FacilityOperationsService.getDashboardSummary(
            operator = GymOperator("op_1", OperatorRole.OWNER, setOf("fac_A"), true),
            facilityId = "fac_A"
        )

        assertNotNull(summary)
        // Dashboard has total visits, active equipment count, but no personal logs
        assertEquals(0, summary.personalRpeLogsCount)
        assertNull(summary.personalNotesPayload)
    }
}
