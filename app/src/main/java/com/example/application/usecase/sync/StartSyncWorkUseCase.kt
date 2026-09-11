package com.example.application.usecase.sync

import com.example.domain.port.SyncScheduler

/**
 * Pure application UseCase for starting sync work.
 * Delegates scheduling to the SyncScheduler domain port.
 */
class StartSyncWorkUseCase(
    private val syncScheduler: SyncScheduler
) {
    operator fun invoke() {
        syncScheduler.scheduleImmediateSync()
    }
}
