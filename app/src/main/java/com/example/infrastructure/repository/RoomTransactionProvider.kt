package com.example.infrastructure.repository

import androidx.room.withTransaction
import com.example.domain.repository.TransactionProvider
import com.example.infrastructure.db.StrengthLogDatabase

class RoomTransactionProvider(
    private val database: StrengthLogDatabase
) : TransactionProvider {
    override suspend fun <T> runAsTransaction(block: suspend () -> T): T {
        return database.withTransaction {
            block()
        }
    }
}
