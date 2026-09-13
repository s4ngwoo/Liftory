package com.example.domain.sync

import com.example.domain.model.PendingUpload

/**
 * Last-write-wins rules for the Firestore outbox.
 *
 * The production queue is FIFO by [PendingUpload.createdAt] and applies
 * [com.google.firebase.firestore.SetOptions.merge] with no revision check.
 * A failed older UPDATE that retries after a newer UPDATE has already
 * succeeded will otherwise overwrite cloud state with stale fields.
 */
object SyncOutboxPolicy {

    private val updatedAtRegex = Regex("\"updatedAt\"\\s*:\\s*(\\d+)")

    fun supersededPendingIds(pending: List<PendingUpload>): Set<String> {
        return pending
            .mapIndexed { index, item -> item to index }
            .groupBy { it.first.entityType to it.first.entityId }
            .flatMap { (_, items) ->
                val newest = items.maxWith(
                    compareBy<Pair<PendingUpload, Int>> { it.first.createdAt }.thenBy { it.second }
                )
                items.filter { it.first.id != newest.first.id }.map { it.first.id }
            }
            .toSet()
    }

    fun parseUpdatedAtEpochMs(payloadJson: String): Long? {
        return updatedAtRegex.find(payloadJson)?.groupValues?.get(1)?.toLongOrNull()
    }

    fun shouldApplyWrite(incomingUpdatedAt: Long?, remoteUpdatedAt: Long?): Boolean {
        if (incomingUpdatedAt == null || remoteUpdatedAt == null) return true
        return incomingUpdatedAt >= remoteUpdatedAt
    }
}
