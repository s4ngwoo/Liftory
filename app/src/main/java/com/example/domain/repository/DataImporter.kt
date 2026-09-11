package com.example.domain.repository

interface DataImporter {
    suspend fun importDataFromJson(jsonString: String): Result<Int>
    suspend fun importDataFromCsv(csvString: String): Result<Int>
}
