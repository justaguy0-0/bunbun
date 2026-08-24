package com.example.bunbun

import com.example.bunbun.presence.PresenceSynchronizer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PresenceSynchronizerTest {
    @Test fun repeatedStartKeepsOneLoop() = runTest {
        var touches = 0
        val synchronizer = PresenceSynchronizer(this, { touches++ }, intervalMillis = 60_000L)

        synchronizer.start()
        synchronizer.start()
        runCurrent()
        assertEquals(1, touches)
        assertTrue(synchronizer.isRunning)

        advanceTimeBy(60_000L)
        runCurrent()
        assertEquals(2, touches)
        synchronizer.stop()
    }

    @Test fun stopCancelsForegroundLoop() = runTest {
        var touches = 0
        val synchronizer = PresenceSynchronizer(this, { touches++ }, intervalMillis = 60_000L)

        synchronizer.start()
        runCurrent()
        synchronizer.stop()
        assertFalse(synchronizer.isRunning)

        advanceTimeBy(180_000L)
        runCurrent()
        assertEquals(1, touches)
    }

    @Test fun authenticationWakeUpIsSerializedAndImmediate() = runTest {
        var touches = 0
        val synchronizer = PresenceSynchronizer(this, { touches++ }, intervalMillis = 60_000L)

        synchronizer.start()
        runCurrent()
        synchronizer.onAuthenticated()
        runCurrent()

        assertEquals(2, touches)
        synchronizer.stop()
    }

    @Test fun authenticationRestartsLoopAfterLogoutStop() = runTest {
        var touches = 0
        val synchronizer = PresenceSynchronizer(this, { touches++ }, intervalMillis = 60_000L)

        synchronizer.start()
        runCurrent()
        synchronizer.stop()
        synchronizer.onAuthenticated()
        runCurrent()

        assertTrue(synchronizer.isRunning)
        assertEquals(2, touches)
        synchronizer.stop()
    }
}
