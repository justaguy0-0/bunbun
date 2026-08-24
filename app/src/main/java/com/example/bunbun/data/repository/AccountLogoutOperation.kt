package com.example.bunbun.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class AccountLogoutOperation(
    private val cancelOutbox: () -> Unit,
    private val activeAccountId: suspend () -> Long?,
    private val unregisterPush: suspend () -> Unit,
    private val remoteLogout: suspend () -> Unit,
    private val clearSession: suspend () -> Unit,
    private val markPushSignedOut: () -> Unit,
    private val clearAccountData: suspend (Long) -> Unit,
    private val remoteTimeoutMillis: Long = 3_000L,
) {
    suspend fun execute(): Long? {
        cancelOutbox()
        val accountId = activeAccountId()

        bestEffort(unregisterPush)
        bestEffort(remoteLogout)

        var localFailure: Throwable? = null
        withContext(NonCancellable) {
            try {
                clearSession()
            } catch (error: Throwable) {
                localFailure = error
            }
            runCatching(markPushSignedOut)
            if (accountId != null) {
                try {
                    clearAccountData(accountId)
                } catch (error: Throwable) {
                    if (localFailure == null) localFailure = error
                }
            }
        }
        localFailure?.let { throw it }
        return accountId
    }

    private suspend fun bestEffort(block: suspend () -> Unit) {
        try {
            withTimeout(remoteTimeoutMillis) { block() }
        } catch (_: TimeoutCancellationException) {
            // Remote logout is deliberately best-effort so offline logout remains usable.
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            // Authentication and account-scoped local data are still cleared below.
        }
    }
}
