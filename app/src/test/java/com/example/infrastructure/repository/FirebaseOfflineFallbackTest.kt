package com.example.infrastructure.repository

import com.example.domain.model.EntityType
import com.example.domain.model.PendingUpload
import com.example.domain.model.SyncOperation
import com.example.domain.repository.AuthRepository
import com.example.domain.model.AuthState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FirebaseOfflineFallbackTest {

    @Test
    fun `FirebaseAuthRepositoryImpl does not crash when Firebase is uninitialized`() = runTest {
        // Given: Firebase is uninitialized (auth passed as null)
        val repository = FirebaseAuthRepositoryImpl(auth = null)

        // Then: AuthState should default to Unauthenticated without throwing
        val state = repository.authState.first()
        assertEquals(AuthState.Unauthenticated, state)
        assertNull(repository.getCurrentUserId())

        // And: signInWithGoogle should return failure gracefully
        val signInResult = repository.signInWithGoogle("dummy_token")
        assertTrue(signInResult.isFailure)
        assertTrue(repository.authState.value is AuthState.Error)

        // And: signOut should succeed and keep unauthenticated
        val signOutResult = repository.signOut()
        assertTrue(signOutResult.isSuccess)
        assertEquals(AuthState.Unauthenticated, repository.authState.value)
    }

    @Test
    fun `FirestoreSyncDataSource does not crash when Firebase is uninitialized`() = runTest {
        // Given: Firebase is uninitialized (firestore passed as null)
        val mockAuthRepo = object : AuthRepository {
            override val authState = kotlinx.coroutines.flow.MutableStateFlow<AuthState>(AuthState.Unauthenticated)
            override suspend fun signInWithGoogle(idToken: String) = Result.success(Unit)
            override suspend fun signOut() = Result.success(Unit)
            override fun getCurrentUserId(): String? = "user_123"
        }
        val dataSource = FirestoreSyncDataSource(firestore = null, authRepository = mockAuthRepo)

        val pending = PendingUpload(
            id = "pending_1",
            entityType = EntityType.SESSION,
            entityId = "session_1",
            operation = SyncOperation.CREATE,
            payloadJson = "{}"
        )

        // When: syncing without Firebase initialized
        val result = dataSource.sync(pending)

        // Then: it should return failure gracefully without throwing IllegalStateException
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue(exception is IllegalStateException)
        assertTrue(exception?.message?.contains("Firebase is not initialized") == true)
    }

    @Test
    fun `FirestoreSyncListener does not crash when Firebase is uninitialized`() {
        val mockAuthRepo = object : AuthRepository {
            override val authState = kotlinx.coroutines.flow.MutableStateFlow<AuthState>(AuthState.Unauthenticated)
            override suspend fun signInWithGoogle(idToken: String) = Result.success(Unit)
            override suspend fun signOut() = Result.success(Unit)
            override fun getCurrentUserId(): String? = "user_123"
        }
        val listener = FirestoreSyncListener(firestore = null, authRepository = mockAuthRepo)

        // Calling startListening and stopListening should be safe no-ops
        listener.startListening()
        listener.stopListening()
    }
}
