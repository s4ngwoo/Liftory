package com.example.domain.sync

import com.example.domain.model.EntityType
import com.example.domain.model.PendingUpload
import com.example.domain.model.SyncOperation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * N11 Account, Sync, Conflict and Sharing Boundary Tests (SYNC-01~SYNC-08).
 */
class SyncConflictAndBoundaryTest {

    @Test
    fun `SYNC-01 offline write enqueues atomic outbox mutation without network access`() {
        val outbox = InMemorySyncOutbox()
        val upload = PendingUpload(
            id = "cmd_1",
            entityType = EntityType.SESSION,
            entityId = "sess_1",
            operation = SyncOperation.CREATE,
            payloadJson = """{"sessionId":"sess_1","revision":1}""",
            createdAt = 1000L
        )

        outbox.enqueue("user_A", upload)
        val pending = outbox.getPending("user_A")

        assertEquals(1, pending.size)
        assertEquals("sess_1", pending[0].entityId)
    }

    @Test
    fun `SYNC-02 retry after lost ack consumes queue safely without duplicate creation`() {
        val remoteStorage = mutableMapOf<String, String>()
        val engine = SyncEngineStub(remoteStorage)

        val upload = PendingUpload(
            id = "cmd_dup_1",
            entityType = EntityType.SESSION,
            entityId = "sess_dup",
            operation = SyncOperation.CREATE,
            payloadJson = """{"name":"Session 1"}"""
        )

        // 1st attempt: server records entity
        engine.processUpload("user_A", upload)
        assertEquals(1, remoteStorage.size)

        // 2nd retry with same idempotency command ID
        engine.processUpload("user_A", upload)
        assertEquals("Idempotent retry must not duplicate remote record", 1, remoteStorage.size)
    }

    @Test
    fun `SYNC-03 older pull response cannot overwrite higher local revision`() {
        val localSession = SyncableEntity(
            id = "sess_1",
            revision = 5L,
            payload = "Local Updated Weight 90kg",
            updatedAtEpochMs = 2000L
        )
        val staleRemotePull = SyncableEntity(
            id = "sess_1",
            revision = 3L,
            payload = "Stale Remote Weight 80kg",
            updatedAtEpochMs = 1500L
        )

        val resolved = SyncConflictResolver.resolve(local = localSession, remote = staleRemotePull)

        assertEquals("Local revision 5 must be preserved against stale revision 3", 5L, resolved.revision)
        assertEquals("Local Updated Weight 90kg", resolved.payload)
    }

    @Test
    fun `SYNC-04 deleted tombstone prevents older update from resurrecting record`() {
        val tombstone = SyncableEntity(
            id = "sess_del",
            revision = 4L,
            payload = "",
            isDeleted = true,
            updatedAtEpochMs = 3000L
        )
        val staleUpdate = SyncableEntity(
            id = "sess_del",
            revision = 2L,
            payload = "Old Session Data",
            isDeleted = false,
            updatedAtEpochMs = 1000L
        )

        val resolved = SyncConflictResolver.resolve(local = tombstone, remote = staleUpdate)

        assertTrue("Deleted tombstone must never resurrect data", resolved.isDeleted)
        assertEquals(4L, resolved.revision)
    }

    @Test
    fun `SYNC-05 concurrent active session modification preserves state without silent termination`() {
        val localActive = SyncableEntity(
            id = "sess_concurrent",
            revision = 2L,
            payload = """{"state":"ACTIVE","device":"Phone"}""",
            isConflict = false
        )
        val remoteActive = SyncableEntity(
            id = "sess_concurrent",
            revision = 2L,
            payload = """{"state":"ACTIVE","device":"Watch"}""",
            isConflict = false
        )

        val resolution = SyncConflictResolver.resolveConcurrentActiveSessions(local = localActive, remote = remoteActive)

        assertFalse("Silent session termination is forbidden", resolution.payload.contains("TERMINATED"))
        assertTrue("Conflict flag must be marked to allow user review", resolution.isConflict)
    }

    @Test
    fun `SYNC-06 account switch isolates outbox queues preventing data leakage between users`() {
        val outbox = InMemorySyncOutbox()
        val uploadA = PendingUpload(
            id = "cmd_A",
            entityType = EntityType.SESSION,
            entityId = "sess_A",
            operation = SyncOperation.CREATE,
            payloadJson = "secret data A"
        )
        outbox.enqueue(userId = "user_A", upload = uploadA)

        // Switch to user B
        val pendingB = outbox.getPending(userId = "user_B")
        assertTrue("User B must never see or upload user A's pending queue", pendingB.isEmpty())
    }

    @Test
    fun `SYNC-07 condition or cohort non-consent transmits zero optional fields`() {
        val payloadWithConsent = SharingPayloadFilter.filterPersonalData(
            rawHealthPayload = """{"rpe":8,"sleep":7,"heartRate":145,"weightKg":80.0}""",
            allowHealthSharing = true,
            allowCohortSharing = false
        )
        assertTrue(payloadWithConsent.contains("sleep"))
        assertFalse(payloadWithConsent.contains("cohortId"))

        val payloadWithoutConsent = SharingPayloadFilter.filterPersonalData(
            rawHealthPayload = """{"rpe":8,"sleep":7,"heartRate":145,"weightKg":80.0}""",
            allowHealthSharing = false,
            allowCohortSharing = false
        )
        assertFalse(payloadWithoutConsent.contains("sleep"))
        assertFalse(payloadWithoutConsent.contains("heartRate"))
        assertTrue(payloadWithoutConsent.contains("weightKg")) // core workout preserved
    }

    @Test
    fun `SYNC-08 unsupported future schema version preserves existing local data with error`() {
        val currentSupportedSchema = 2
        val payloadFromFuture = """{"schemaVersion":999,"unknownData":"xyz"}"""

        val result = SyncSchemaGate.validateAndParse(payloadFromFuture, currentSupportedSchema)

        assertFalse(result.isSuccess)
        assertEquals("Unsupported schema version: 999", result.errorMessage)
    }

    @Test
    fun `newer remote update resurrects a stale local tombstone`() {
        val staleTombstone = SyncableEntity(
            id = "sess_revived",
            revision = 2L,
            payload = "",
            isDeleted = true,
            updatedAtEpochMs = 1000L
        )
        val newerRemote = SyncableEntity(
            id = "sess_revived",
            revision = 5L,
            payload = "resurrected",
            isDeleted = false,
            updatedAtEpochMs = 4000L
        )

        val resolved = SyncConflictResolver.resolve(local = staleTombstone, remote = newerRemote)

        assertFalse(resolved.isDeleted)
        assertEquals(5L, resolved.revision)
        assertEquals("resurrected", resolved.payload)
    }

    @Test
    fun `higher revision remote tombstone wins over live local`() {
        val local = SyncableEntity(
            id = "sess_live",
            revision = 3L,
            payload = "still here",
            isDeleted = false
        )
        val remoteTombstone = SyncableEntity(
            id = "sess_live",
            revision = 4L,
            payload = "",
            isDeleted = true
        )

        val resolved = SyncConflictResolver.resolve(local = local, remote = remoteTombstone)

        assertTrue(resolved.isDeleted)
        assertEquals(4L, resolved.revision)
    }

    @Test
    fun `equal revision prefers later timestamp then local on a tie`() {
        val local = SyncableEntity(
            id = "sess_eq",
            revision = 2L,
            payload = "local",
            updatedAtEpochMs = 2000L
        )
        val olderRemote = SyncableEntity(
            id = "sess_eq",
            revision = 2L,
            payload = "remote-old",
            updatedAtEpochMs = 1000L
        )
        val newerRemote = SyncableEntity(
            id = "sess_eq",
            revision = 2L,
            payload = "remote-new",
            updatedAtEpochMs = 3000L
        )
        val tiedRemote = SyncableEntity(
            id = "sess_eq",
            revision = 2L,
            payload = "remote-tie",
            updatedAtEpochMs = 2000L
        )

        assertEquals("local", SyncConflictResolver.resolve(local, olderRemote).payload)
        assertEquals("remote-new", SyncConflictResolver.resolve(local, newerRemote).payload)
        assertEquals("local", SyncConflictResolver.resolve(local, tiedRemote).payload)
    }

    @Test
    fun `removeCompleted drops only that command and isolates other users`() {
        val outbox = InMemorySyncOutbox()
        outbox.enqueue(
            "user_A",
            PendingUpload(
                id = "cmd_1",
                entityType = EntityType.SESSION,
                entityId = "sess_A",
                operation = SyncOperation.CREATE,
                payloadJson = "A"
            )
        )
        outbox.enqueue(
            "user_B",
            PendingUpload(
                id = "cmd_1",
                entityType = EntityType.SESSION,
                entityId = "sess_B",
                operation = SyncOperation.CREATE,
                payloadJson = "B"
            )
        )

        outbox.removeCompleted("user_A", "cmd_1")

        assertTrue(outbox.getPending("user_A").isEmpty())
        assertEquals(1, outbox.getPending("user_B").size)
        assertEquals("sess_B", outbox.getPending("user_B")[0].entityId)
    }

    @Test
    fun `schema gate defaults missing version to 1 and accepts the current max`() {
        assertTrue(SyncSchemaGate.validateAndParse("""{"foo":1}""", 2).isSuccess)
        assertTrue(SyncSchemaGate.validateAndParse("""{"schemaVersion":2}""", 2).isSuccess)
        assertFalse(SyncSchemaGate.validateAndParse("""{"schemaVersion":3}""", 2).isSuccess)
    }

    @Test
    fun `sharing filter strips stress fatigue and cohortId and cleans trailing commas`() {
        val stripped = SharingPayloadFilter.filterPersonalData(
            rawHealthPayload = """{"rpe":8,"stress":3,"fatigue":4,"cohortId":"c1","weightKg":80}""",
            allowHealthSharing = false,
            allowCohortSharing = false
        )

        assertFalse(stripped.contains("stress"))
        assertFalse(stripped.contains("fatigue"))
        assertFalse(stripped.contains("cohortId"))
        assertTrue(stripped.contains("weightKg"))
        assertFalse(stripped.contains(",}"))

        val kept = SharingPayloadFilter.filterPersonalData(
            rawHealthPayload = """{"rpe":8,"stress":3,"fatigue":4,"weightKg":80}""",
            allowHealthSharing = true,
            allowCohortSharing = false
        )
        assertTrue(kept.contains("stress"))
        assertTrue(kept.contains("fatigue"))
    }
}
