package com.example.domain.repository

interface TransactionProvider {
    suspend fun <T> runAsTransaction(block: suspend () -> T): T
}
