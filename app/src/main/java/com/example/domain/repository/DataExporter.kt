package com.example.domain.repository

interface DataExporter {
    suspend fun exportDataAsJson(): Result<String>
    suspend fun exportDataAsCsv(): Result<String>
}
