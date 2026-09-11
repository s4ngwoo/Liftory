package com.example.domain.port

/**
 * Domain port for scheduling background data synchronization.
 * Pure interface independent of Android WorkManager or Context.
 */
interface SyncScheduler {
    fun scheduleImmediateSync()
}
