package com.example.bunbun

import com.example.bunbun.data.repository.AccountLogoutOperation
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountLogoutOperationTest {
    @Test fun capturesAccountBeforeSessionClearAndClearsOnlyThatAccountsData() = runBlocking {
        val events = mutableListOf<String>()
        var activeAccount: Long? = ACCOUNT_A
        val rows = mutableMapOf(ACCOUNT_A to 4, ACCOUNT_B to 7)
        val operation = AccountLogoutOperation(
            cancelOutbox = { events += "cancel-outbox" },
            activeAccountId = { events += "capture:$activeAccount"; activeAccount },
            unregisterPush = { events += "unregister-push" },
            remoteLogout = { events += "remote-logout" },
            clearSession = { events += "clear-session"; activeAccount = null },
            markPushSignedOut = { events += "push-signed-out" },
            clearAccountData = { accountId -> events += "clear-data:$accountId"; rows.remove(accountId) },
        )

        val clearedAccount = operation.execute()

        assertEquals(ACCOUNT_A, clearedAccount)
        assertNull(activeAccount)
        assertFalse(rows.containsKey(ACCOUNT_A))
        assertEquals(7, rows[ACCOUNT_B])
        assertTrue(events.indexOf("capture:$ACCOUNT_A") < events.indexOf("clear-session"))
        assertTrue(events.indexOf("cancel-outbox") < events.indexOf("clear-data:$ACCOUNT_A"))
    }

    @Test fun backendAndPushFailuresDoNotBlockOfflineLocalLogout() = runBlocking {
        var sessionCleared = false
        var localDataCleared = false
        var pushMarkedPending = false
        AccountLogoutOperation(
            cancelOutbox = { },
            activeAccountId = { ACCOUNT_A },
            unregisterPush = { error("offline push") },
            remoteLogout = { error("offline backend") },
            clearSession = { sessionCleared = true },
            markPushSignedOut = { pushMarkedPending = true },
            clearAccountData = { localDataCleared = true },
        ).execute()

        assertTrue(sessionCleared)
        assertTrue(localDataCleared)
        assertTrue(pushMarkedPending)
    }

    private companion object {
        const val ACCOUNT_A = 11L
        const val ACCOUNT_B = 22L
    }
}
