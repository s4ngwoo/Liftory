package com.example.architecture

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * ARCH-01 ~ ARCH-04: Clean Architecture Dependency Boundary Tests.
 * Ensures strict enforcement of the Dependency Inversion Rule:
 * - Domain MUST NOT import Presentation, Infrastructure, Android, Room, or Firebase.
 * - Application MUST NOT import Presentation, Infrastructure, Android Context, WorkManager, or Compose.
 */
class ArchitectureRulesTest {

    private val projectRoot = File(System.getProperty("user.dir") ?: ".")
    private val domainSrcDir = File(projectRoot, "src/main/java/com/example/domain")
    private val applicationSrcDir = File(projectRoot, "src/main/java/com/example/application")

    /**
     * ARCH-01: Domain layer must be pure Kotlin.
     * Must NOT import presentation, infrastructure, android, androidx, or firebase.
     */
    @Test
    fun `ARCH-01 Domain layer must NOT depend on Presentation, Android, or Frameworks`() {
        val violations = mutableListOf<String>()
        val forbiddenDomainImports = listOf(
            "com.example.presentation",
            "com.example.infrastructure",
            "android.",
            "androidx.compose",
            "androidx.room",
            "androidx.work",
            "com.google.firebase"
        )

        domainSrcDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val lines = file.readLines()
                lines.forEachIndexed { index, line ->
                    if (line.trim().startsWith("import ")) {
                        for (forbidden in forbiddenDomainImports) {
                            if (line.contains(forbidden)) {
                                violations.add("${file.relativeTo(projectRoot).path}:${index + 1} imports '$forbidden'")
                            }
                        }
                    }
                }
            }

        assertTrue(
            "ARCH-01 VIOLATION: Domain layer has forbidden external dependencies:\n" + violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    /**
     * ARCH-02: Application layer must NOT depend on Presentation, Infrastructure concrete classes, Android, or WorkManager.
     */
    @Test
    fun `ARCH-02 Application layer must NOT depend on Presentation, Infrastructure, Android Context, or WorkManager`() {
        val violations = mutableListOf<String>()
        val forbiddenAppImports = listOf(
            "com.example.presentation",
            "com.example.infrastructure",
            "android.content.Context",
            "androidx.work",
            "androidx.compose"
        )

        applicationSrcDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val lines = file.readLines()
                lines.forEachIndexed { index, line ->
                    if (line.trim().startsWith("import ")) {
                        for (forbidden in forbiddenAppImports) {
                            if (line.contains(forbidden)) {
                                violations.add("${file.relativeTo(projectRoot).path}:${index + 1} imports '$forbidden'")
                            }
                        }
                    }
                }
            }

        assertTrue(
            "ARCH-02 VIOLATION: Application layer has forbidden external dependencies:\n" + violations.joinToString("\n"),
            violations.isEmpty()
        )
    }
}
