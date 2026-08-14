package com.example.bunbun.data.repository

import com.example.bunbun.data.api.BunbunApi
import com.example.bunbun.data.api.NetworkModule
import com.example.bunbun.data.local.CachedChat
import com.example.bunbun.data.local.CachedMessage
import com.example.bunbun.data.local.LocalDataStore
import com.example.bunbun.data.local.SessionManager
import com.example.bunbun.data.model.*
import com.example.bunbun.outbox.OutboxDrainResult
import com.example.bunbun.outbox.OutboxFailureDisposition
import com.example.bunbun.outbox.OutboxScheduler
import com.example.bunbun.outbox.classifyOutboxHttpFailure
import com.example.bunbun.push.PushTokenSynchronizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import java.io.IOException

class ApiException(val code: String, message: String, val status: Int) : IOException(message)

class BunbunRepository(
    private val api: BunbunApi,
    private val sessions: SessionManager,
    private val local: LocalDataStore,
    private val outboxScheduler: OutboxScheduler,
    private val pushTokens: PushTokenSynchronizer? = null,
) {
    private val accountMutex = Mutex()

    suspend fun restoreSession(): UserDto? {
        if (sessions.load() == null) return null
        val cached = sessions.activeUser()?.let { sessionUser ->
            local.cachedCurrentUser(sessionUser.id) ?: sessionUser
        }
        if (cached != null) {
            outboxScheduler.enqueue()
            return cached
        }
        return refreshSession()
    }

    suspend fun refreshSession(): UserDto? {
        if (sessions.currentOrLoad() == null) return null
        return try {
            val remote = unwrapApiResponse(api.me()).user
            accountMutex.withLock { activate(remote, token = null) }
            remote.also {
                outboxScheduler.enqueue()
                runCatching { pushTokens?.afterAuthentication() }
            }
        } catch (error: ApiException) {
            if (error.status == 401) {
                accountMutex.withLock { sessions.clear() }
                pushTokens?.markSignedOut()
                null
            } else throw error
        }
    }

    suspend fun login(username: String, password: String): UserDto {
        val data = unwrapApiResponse(api.login(LoginRequest(username.trim().lowercase(), password)))
        accountMutex.withLock { activate(data.user, data.accessToken) }
        outboxScheduler.enqueue()
        runCatching { pushTokens?.afterAuthentication() }
        return data.user
    }

    suspend fun register(username: String, displayName: String, password: String): UserDto {
        val data = unwrapApiResponse(
            api.register(RegisterRequest(username.trim().lowercase(), displayName.trim(), password)),
        )
        accountMutex.withLock { activate(data.user, data.accessToken) }
        outboxScheduler.enqueue()
        runCatching { pushTokens?.afterAuthentication() }
        return data.user
    }

    suspend fun logout() = accountMutex.withLock {
        try {
            runCatching { pushTokens?.unregisterBeforeLogout() }
            if (sessions.peekToken() != null) unwrapApiResponse(api.logout())
        } finally {
            sessions.clear()
            pushTokens?.markSignedOut()
        }
    }

    suspend fun activeAccountId(): Long = sessions.activeUserId()
        ?: error("An authenticated account is required")

    fun observeChats(accountId: Long): Flow<List<CachedChat>> = local.observeChats(accountId)

    fun observeMessages(accountId: Long, chatId: Long): Flow<List<CachedMessage>> =
        local.observeMessages(accountId, chatId)

    suspend fun syncChats(accountId: Long) {
        requireActiveAccount(accountId)
        local.mergeChats(accountId, unwrapApiResponse(api.chats()).chats)
    }

    suspend fun syncMessages(accountId: Long, chatId: Long) {
        requireActiveAccount(accountId)
        val afterId = local.maxServerId(accountId, chatId).takeIf { it > 0L }
        val data = unwrapApiResponse(api.messages(chatId, afterId))
        local.mergeMessages(accountId, accountId, chatId, data)
    }

    suspend fun searchUsers(query: String): List<UserDto> =
        unwrapApiResponse(api.searchUsers(query.trim())).users

    suspend fun createDirect(userId: Long): ChatDto {
        val accountId = activeAccountId()
        val chat = unwrapApiResponse(api.createDirect(CreateDirectRequest(userId))).chat
        local.mergeChats(accountId, listOf(chat))
        return chat
    }

    suspend fun queueMessage(accountId: Long, chatId: Long, text: String): CachedMessage {
        requireActiveAccount(accountId)
        val message = local.queueOutgoing(accountId, chatId, accountId, text)
        outboxScheduler.enqueue()
        return message
    }

    suspend fun retryMessage(accountId: Long, localId: String): Boolean {
        requireActiveAccount(accountId)
        val retried = local.retryFailed(localId, accountId)
        if (retried) outboxScheduler.enqueue()
        return retried
    }

    suspend fun markRead(accountId: Long, chatId: Long, messageId: Long) {
        requireActiveAccount(accountId)
        local.clearUnread(accountId, chatId)
        if (local.myLastRead(accountId, chatId) >= messageId) return
        val confirmed = unwrapApiResponse(api.markRead(MarkReadRequest(chatId, messageId))).lastReadMessageId
        local.markLocallyRead(accountId, chatId, confirmed)
    }

    suspend fun drainOutbox(): OutboxDrainResult = accountMutex.withLock {
        val accountId = sessions.activeUserId() ?: return OutboxDrainResult.AUTH_REQUIRED
        if (sessions.currentOrLoad() == null) return OutboxDrainResult.AUTH_REQUIRED
        local.recoverInterruptedSends(accountId)
        for (pending in local.pendingMessages(accountId)) {
            if (sessions.activeUserId() != accountId) return OutboxDrainResult.AUTH_REQUIRED
            local.markSending(pending.localId)
            val acknowledged = try {
                unwrapApiResponse(
                    api.sendMessage(
                        SendMessageRequest(
                            chatId = pending.chatId,
                            text = pending.text,
                            clientMessageId = requireNotNull(pending.clientMessageId),
                        ),
                    ),
                ).message
            } catch (error: ApiException) {
                when (classifyOutboxHttpFailure(error.status)) {
                    OutboxFailureDisposition.AUTH_REQUIRED -> {
                        local.markPending(pending.localId, error.code)
                        return OutboxDrainResult.AUTH_REQUIRED
                    }
                    OutboxFailureDisposition.RETRY -> {
                        local.markPending(pending.localId, error.code)
                        return OutboxDrainResult.RETRY
                    }
                    OutboxFailureDisposition.FAILED -> {
                        local.markFailed(pending.localId, error.code)
                        continue
                    }
                }
            } catch (error: IOException) {
                local.markPending(pending.localId, error.javaClass.simpleName)
                return OutboxDrainResult.RETRY
            } catch (error: Throwable) {
                local.markPending(pending.localId, error.javaClass.simpleName)
                return OutboxDrainResult.RETRY
            }
            local.acknowledge(accountId, accountId, acknowledged)
        }
        OutboxDrainResult.COMPLETED
    }

    private suspend fun activate(user: UserDto, token: String?) {
        token?.let { sessions.save(it) }
        sessions.setActiveUser(user)
        local.cacheCurrentUser(user)
    }

    private suspend fun requireActiveAccount(accountId: Long) {
        check(sessions.activeUserId() == accountId) { "The active account changed" }
    }
}

fun <T> unwrapApiResponse(response: Response<ApiEnvelope<T>>): T {
    val body = response.body()
    if (response.isSuccessful && body?.ok == true && body.data != null) return body.data
    val parsedError = response.errorBody()?.string()?.let { raw ->
        runCatching { NetworkModule.json.decodeFromString<ApiEnvelope<JsonElement>>(raw).error }.getOrNull()
    } ?: body?.error
    throw ApiException(
        parsedError?.code ?: "HTTP_${response.code()}",
        parsedError?.message ?: "Request failed (${response.code()})",
        response.code(),
    )
}
