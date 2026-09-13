package com.example.domain.payment

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * N15 Payment, Membership, Refund, and Settlement Tests (PAY-01~PAY-08).
 */
class PaymentAndSettlementTest {

    @Test
    fun `PAY-01 client price or facility tampering is strictly rejected`() {
        val service = PaymentProcessingService()
        val result = service.createOrder(
            orderId = "ord_1",
            userId = "user_1",
            facilityId = "fac_A",
            planId = "pass_100k",
            catalogAmountWon = 100_000L,
            clientRequestedAmountWon = 50_000L // Tampered!
        )

        assertFalse("Price tampering must be rejected", result.isSuccess)
        assertEquals("Amount mismatch: client price tampered", result.errorMessage)
    }

    @Test
    fun `PAY-02 duplicate payment command returns existing order without creating new charge`() {
        val service = PaymentProcessingService()
        val res1 = service.createOrder("ord_dup", "u1", "fac_A", "plan_1", 100_000L, 100_000L)
        val res2 = service.createOrder("ord_dup", "u1", "fac_A", "plan_1", 100_000L, 100_000L)

        assertTrue(res1.isSuccess)
        assertTrue(res2.isSuccess)
        assertEquals(res1.order?.id, res2.order?.id)
        assertEquals(1, service.getAllOrders().size)
    }

    @Test
    fun `PAY-03 duplicate webhook events record ledger exactly once`() {
        val service = PaymentProcessingService()
        service.createOrder("ord_webhook", "u1", "fac_A", "plan_1", 100_000L, 100_000L)

        // Webhook arrives twice
        val hook1 = service.handleApprovalWebhook("hook_event_1", "ord_webhook")
        val hook2 = service.handleApprovalWebhook("hook_event_1", "ord_webhook")

        assertTrue(hook1.isSuccess)
        assertTrue(hook2.isSuccess)
        assertEquals("Exactly 1 ledger entry must exist for order", 1, service.getLedgersForOrder("ord_webhook").size)
        assertEquals(PaymentStatus.APPROVED, service.getOrder("ord_webhook")?.status)
    }

    @Test
    fun `PAY-04 delayed approval webhook cannot resurrect cancelled order`() {
        val service = PaymentProcessingService()
        service.createOrder("ord_cancelled", "u1", "fac_A", "plan_1", 100_000L, 100_000L)
        service.cancelOrder("ord_cancelled")

        val hookResult = service.handleApprovalWebhook("hook_delayed", "ord_cancelled")
        assertFalse("Cancelled order cannot be revived by delayed approval", hookResult.isSuccess)
        assertEquals(PaymentStatus.CANCELLED, service.getOrder("ord_cancelled")?.status)
    }

    @Test
    fun `PAY-06 partial refund prevents total refund exceeding original payment amount`() {
        val service = PaymentProcessingService()
        service.createOrder("ord_refund", "u1", "fac_A", "plan_1", 100_000L, 100_000L)
        service.handleApprovalWebhook("hook_ref", "ord_refund")

        // First partial refund: 40,000 KRW
        val refund1 = service.refund("ord_refund", 40_000L)
        assertTrue(refund1.isSuccess)
        assertEquals(PaymentStatus.PARTIALLY_REFUNDED, service.getOrder("ord_refund")?.status)

        // Second excessive refund: 70,000 KRW (total 110,000 > 100,000)
        val refund2 = service.refund("ord_refund", 70_000L)
        assertFalse("Refund exceeding original payment must be rejected", refund2.isSuccess)
        assertEquals("Refund amount exceeds remaining refundable balance", refund2.errorMessage)

        // Valid remaining refund: 60,000 KRW
        val refund3 = service.refund("ord_refund", 60_000L)
        assertTrue(refund3.isSuccess)
        assertEquals(PaymentStatus.REFUNDED, service.getOrder("ord_refund")?.status)
    }

    @Test
    fun `unknown order webhook refund and cancel fail closed`() {
        val service = PaymentProcessingService()

        val hook = service.handleApprovalWebhook("hook_missing", "ord_missing")
        assertFalse(hook.isSuccess)
        assertEquals("Order not found", hook.errorMessage)

        val refund = service.refund("ord_missing", 1_000L)
        assertFalse(refund.isSuccess)
        assertEquals("Order not found", refund.errorMessage)

        val cancel = service.cancelOrder("ord_missing")
        assertFalse(cancel.isSuccess)
        assertEquals("Order not found", cancel.errorMessage)
        assertTrue(service.getAllOrders().isEmpty())
    }

    @Test
    fun `PAY-07 ledger fee split strictly balances with zero fractional mismatch`() {
        val gross = 100_000L
        val entry = SettlementCalculator.computeLedgerEntry(
            orderId = "ord_split",
            grossAmount = gross,
            pgFeeRate = 0.033, // 3.3% = 3,300 KRW
            platformFeeRate = 0.05 // 5.0% = 5,000 KRW
        )

        assertEquals(3_300L, entry.pgFee)
        assertEquals(5_000L, entry.platformFee)
        assertEquals(91_700L, entry.netSettlementAmount)

        // Strict balance check: Gross = pgFee + platformFee + net
        val sum = entry.pgFee + entry.platformFee + entry.netSettlementAmount
        assertEquals("Ledger fee sum must equal gross amount exactly", gross, sum)
    }
}
