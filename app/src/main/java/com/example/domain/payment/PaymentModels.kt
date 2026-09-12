package com.example.domain.payment

enum class PaymentStatus {
    PENDING,
    APPROVED,
    CANCELLED,
    PARTIALLY_REFUNDED,
    REFUNDED
}

data class PaymentOrder(
    val id: String,
    val userId: String,
    val facilityId: String,
    val planId: String,
    val amountWon: Long,
    val refundedAmountWon: Long = 0L,
    val status: PaymentStatus = PaymentStatus.PENDING
)

data class LedgerEntry(
    val id: String,
    val orderId: String,
    val grossAmount: Long,
    val pgFee: Long,
    val platformFee: Long,
    val netSettlementAmount: Long
)

data class OrderCreationResult(
    val isSuccess: Boolean,
    val order: PaymentOrder? = null,
    val errorMessage: String? = null
)

data class PaymentActionResult(
    val isSuccess: Boolean,
    val errorMessage: String? = null
)

object SettlementCalculator {
    fun computeLedgerEntry(
        orderId: String,
        grossAmount: Long,
        pgFeeRate: Double,
        platformFeeRate: Double
    ): LedgerEntry {
        val pgFee = (grossAmount * pgFeeRate).toLong()
        val platformFee = (grossAmount * platformFeeRate).toLong()
        val net = grossAmount - pgFee - platformFee
        return LedgerEntry(
            id = "ledger_${orderId}",
            orderId = orderId,
            grossAmount = grossAmount,
            pgFee = pgFee,
            platformFee = platformFee,
            netSettlementAmount = net
        )
    }
}

class PaymentProcessingService {
    private val orders = mutableMapOf<String, PaymentOrder>()
    private val processedWebhooks = mutableSetOf<String>()
    private val ledgers = mutableListOf<LedgerEntry>()

    fun createOrder(
        orderId: String,
        userId: String,
        facilityId: String,
        planId: String,
        catalogAmountWon: Long,
        clientRequestedAmountWon: Long
    ): OrderCreationResult {
        if (catalogAmountWon != clientRequestedAmountWon) {
            return OrderCreationResult(false, null, "Amount mismatch: client price tampered")
        }
        val existing = orders[orderId]
        if (existing != null) {
            return OrderCreationResult(true, existing, null)
        }
        val newOrder = PaymentOrder(
            id = orderId,
            userId = userId,
            facilityId = facilityId,
            planId = planId,
            amountWon = catalogAmountWon
        )
        orders[orderId] = newOrder
        return OrderCreationResult(true, newOrder, null)
    }

    fun handleApprovalWebhook(eventId: String, orderId: String): PaymentActionResult {
        if (processedWebhooks.contains(eventId)) {
            return PaymentActionResult(true) // Idempotent
        }
        val order = orders[orderId] ?: return PaymentActionResult(false, "Order not found")
        if (order.status == PaymentStatus.CANCELLED) {
            return PaymentActionResult(false, "Order was already cancelled")
        }

        processedWebhooks.add(eventId)
        orders[orderId] = order.copy(status = PaymentStatus.APPROVED)

        // Record ledger entry exactly once
        if (ledgers.none { it.orderId == orderId }) {
            val ledger = SettlementCalculator.computeLedgerEntry(
                orderId = orderId,
                grossAmount = order.amountWon,
                pgFeeRate = 0.033,
                platformFeeRate = 0.05
            )
            ledgers.add(ledger)
        }
        return PaymentActionResult(true)
    }

    fun cancelOrder(orderId: String): PaymentActionResult {
        val order = orders[orderId] ?: return PaymentActionResult(false, "Order not found")
        orders[orderId] = order.copy(status = PaymentStatus.CANCELLED)
        return PaymentActionResult(true)
    }

    fun refund(orderId: String, refundAmount: Long): PaymentActionResult {
        val order = orders[orderId] ?: return PaymentActionResult(false, "Order not found")
        val remaining = order.amountWon - order.refundedAmountWon
        if (refundAmount > remaining) {
            return PaymentActionResult(false, "Refund amount exceeds remaining refundable balance")
        }
        val newRefunded = order.refundedAmountWon + refundAmount
        val newStatus = if (newRefunded == order.amountWon) PaymentStatus.REFUNDED else PaymentStatus.PARTIALLY_REFUNDED
        orders[orderId] = order.copy(refundedAmountWon = newRefunded, status = newStatus)
        return PaymentActionResult(true)
    }

    fun getOrder(orderId: String): PaymentOrder? = orders[orderId]
    fun getAllOrders(): List<PaymentOrder> = orders.values.toList()
    fun getLedgersForOrder(orderId: String): List<LedgerEntry> = ledgers.filter { it.orderId == orderId }
}
