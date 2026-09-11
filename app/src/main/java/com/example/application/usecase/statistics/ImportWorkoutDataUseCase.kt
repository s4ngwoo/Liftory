package com.example.application.usecase.statistics

import com.example.domain.repository.DataImporter

class ImportWorkoutDataUseCase(
    private val dataImporter: DataImporter
) {
    suspend fun importFromJson(jsonString: String): Result<Int> {
        return dataImporter.importDataFromJson(jsonString)
    }

    suspend fun importFromCsv(csvString: String): Result<Int> {
        return dataImporter.importDataFromCsv(csvString)
    }
}
