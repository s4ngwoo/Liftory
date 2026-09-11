package com.example.application.usecase.statistics

import com.example.domain.repository.DataImporter
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeDataImporter : DataImporter {
    var shouldFail: Boolean = false
    var lastImportedJson: String? = null
    var lastImportedCsv: String? = null

    override suspend fun importDataFromJson(jsonString: String): Result<Int> {
        if (shouldFail) return Result.failure(IllegalArgumentException("Invalid JSON schema"))
        lastImportedJson = jsonString
        return Result.success(5) // Simulates 5 imported records
    }

    override suspend fun importDataFromCsv(csvString: String): Result<Int> {
        if (shouldFail) return Result.failure(IllegalArgumentException("Invalid CSV format"))
        lastImportedCsv = csvString
        return Result.success(3) // Simulates 3 imported records
    }
}

class ImportWorkoutDataUseCaseTest {

    private lateinit var fakeDataImporter: FakeDataImporter
    private lateinit var importWorkoutDataUseCase: ImportWorkoutDataUseCase

    @Before
    fun setUp() {
        fakeDataImporter = FakeDataImporter()
        importWorkoutDataUseCase = ImportWorkoutDataUseCase(fakeDataImporter)
    }

    @Test
    fun `importFromJson delegates to importer and returns imported count on success`() = runTest {
        val sampleJson = """{"schemaVersion":1,"sessions":[],"sets":[]}"""
        val result = importWorkoutDataUseCase.importFromJson(sampleJson)

        assertTrue(result.isSuccess)
        assertEquals(5, result.getOrNull())
        assertEquals(sampleJson, fakeDataImporter.lastImportedJson)
    }

    @Test
    fun `importFromJson returns failure when importer encounters error`() = runTest {
        fakeDataImporter.shouldFail = true
        val result = importWorkoutDataUseCase.importFromJson("malformed json")

        assertTrue(result.isFailure)
        assertEquals("Invalid JSON schema", result.exceptionOrNull()?.message)
    }

    @Test
    fun `importFromCsv delegates to importer and returns imported count on success`() = runTest {
        val sampleCsv = "sessionId,sessionStartTime,exerciseId,weight,reps,rpe\ns1,1000,e1,80.0,5,8.0"
        val result = importWorkoutDataUseCase.importFromCsv(sampleCsv)

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrNull())
        assertEquals(sampleCsv, fakeDataImporter.lastImportedCsv)
    }

    @Test
    fun `importFromCsv returns failure when importer encounters error`() = runTest {
        fakeDataImporter.shouldFail = true
        val result = importWorkoutDataUseCase.importFromCsv("corrupted csv")

        assertTrue(result.isFailure)
        assertEquals("Invalid CSV format", result.exceptionOrNull()?.message)
    }
}
