package com.example.application.usecase.statistics

import com.example.domain.repository.DataExporter

class ExportWorkoutDataUseCase(
    private val dataExporter: DataExporter
) {
    suspend fun exportAsJson(): Result<String> {
        return dataExporter.exportDataAsJson()
    }

    suspend fun exportAsCsv(): Result<String> {
        return dataExporter.exportDataAsCsv()
    }
}
