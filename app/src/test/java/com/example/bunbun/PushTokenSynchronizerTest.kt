package com.example.bunbun

import com.example.bunbun.push.PushRegistrationRemote
import com.example.bunbun.push.PushSessionSource
import com.example.bunbun.push.PushRegistrationTrigger
import com.example.bunbun.push.PushTokenStorage
import com.example.bunbun.push.PushTokenSynchronizer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PushTokenSynchronizerTest {
    @Test
    fun newTokenIsRegisteredWhenSessionExists() = runBlocking {
        val storage = FakeStorage()
        val remote = FakeRemote()
        val synchronizer = PushTokenSynchronizer(storage, PushSessionSource { "session" }, remote, PushRegistrationTrigger { })

        synchronizer.onRegistered("token-1")

        assertEquals(listOf("token-1"), remote.registered)
        assertTrue(storage.synchronized)
    }

    @Test
    fun tokenReceivedBeforeLoginRemainsPendingAndSyncsAfterLogin() = runBlocking {
        var session: String? = null
        val storage = FakeStorage()
        val remote = FakeRemote()
        val synchronizer = PushTokenSynchronizer(storage, PushSessionSource { session }, remote, PushRegistrationTrigger { })

        synchronizer.onRegistered("pending-token")
        assertTrue(remote.registered.isEmpty())
        assertFalse(storage.synchronized)

        session = "session"
        synchronizer.afterAuthentication()
        assertEquals(listOf("pending-token"), remote.registered)
        assertTrue(storage.synchronized)
    }

    @Test
    fun logoutUnregistersOldAccountAndNextLoginRegistersForNewAccount() = runBlocking {
        var session: String? = "account-a"
        val storage = FakeStorage().apply {
            value = "shared-device-token"
            synchronized = true
        }
        val remote = FakeRemote()
        val synchronizer = PushTokenSynchronizer(storage, PushSessionSource { session }, remote, PushRegistrationTrigger { })

        synchronizer.unregisterBeforeLogout()
        synchronizer.markSignedOut()
        session = "account-b"
        synchronizer.afterAuthentication()

        assertEquals(listOf("shared-device-token"), remote.unregistered)
        assertEquals(listOf("shared-device-token"), remote.registered)
        assertTrue(storage.synchronized)
    }

    private class FakeStorage : PushTokenStorage {
        var value: String? = null
        var synchronized = false
        override fun token() = value
        override fun savePending(token: String) { value = token; synchronized = false }
        override fun markSynchronized() { synchronized = true }
        override fun markPending() { synchronized = false }
        override fun isSynchronized() = synchronized
    }

    private class FakeRemote : PushRegistrationRemote {
        val registered = mutableListOf<String>()
        val unregistered = mutableListOf<String>()
        override suspend fun register(token: String) { registered += token }
        override suspend fun unregister(token: String) { unregistered += token }
    }
}
