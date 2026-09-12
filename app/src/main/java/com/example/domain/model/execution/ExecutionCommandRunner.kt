package com.example.domain.model.execution

/**
 * Idempotent command gate with optimistic revision checks (EXEC-04, EXEC-05, N04.4).
 *
 * - Same [commandId] is applied at most once.
 * - [expectedRevision] must match the current revision or the call fails with a conflict.
 * - Successful unique commands advance revision by 1.
 */
class ExecutionCommandRunner(
    initialRevision: Long = 1L,
    initiallyExecutedCommandIds: Set<String> = emptySet()
) {
    private var revision: Long = initialRevision
    private val executedCommandIds = linkedSetOf<String>().apply { addAll(initiallyExecutedCommandIds) }

    val currentRevision: Long get() = revision
    val executedIds: Set<String> get() = executedCommandIds.toSet()

    fun runCommand(
        commandId: String,
        expectedRevision: Long,
        action: () -> Boolean
    ): Result<Unit> {
        // Idempotency wins over revision: same commandId never re-runs (EXEC-04).
        if (commandId in executedCommandIds) {
            return Result.success(Unit)
        }
        if (expectedRevision != revision) {
            return Result.failure(
                IllegalStateException("Revision conflict: expected $expectedRevision, actual $revision")
            )
        }
        val applied = action()
        if (!applied) {
            return Result.failure(IllegalStateException("Command rejected by state machine: $commandId"))
        }
        executedCommandIds.add(commandId)
        revision += 1
        return Result.success(Unit)
    }

    fun hasExecuted(commandId: String): Boolean = commandId in executedCommandIds
}
