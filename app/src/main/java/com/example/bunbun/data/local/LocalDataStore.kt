package com.example.bunbun.data.local

import androidx.room.withTransaction
import com.example.bunbun.data.model.ChatDto
import com.example.bunbun.data.model.MessageDto
import com.example.bunbun.data.model.MessagesData
import com.example.bunbun.data.model.UserDto
import com.example.bunbun.data.time.parseServerTimestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class LocalDataStore(
    private val database: BunbunDatabase,
    private val now: () -> Long = System::currentTimeMillis,
    private val newClientId: () -> String = { UUID.randomUUID().toString() },
) {
    private val dao = database.dao()

    suspend fun cacheCurrentUser(user: UserDto) {
        dao.upsertCurrentUser(
            CachedCurrentUserEntity(user.id, user.username, user.displayName, user.createdAt, now()),
        )
    }

    suspend fun cachedCurrentUser(accountId: Long): UserDto? = dao.currentUser(accountId)?.let {
        UserDto(it.accountId, it.username, it.displayName, it.createdAt)
    }

    suspend fun hasChats(accountId: Long): Boolean = dao.chatCount(accountId) > 0

    fun observeChats(accountId: Long): Flow<List<CachedChat>> = dao.observeChats(accountId).map { rows ->
        rows.map(CachedChatEntity::asModel)
    }

    fun observeChat(accountId: Long, chatId: Long): Flow<CachedChat?> =
        dao.observeChat(accountId, chatId).map { it?.asModel() }

    fun observeMessages(accountId: Long, chatId: Long): Flow<List<CachedMessage>> =
        dao.observeMessages(accountId, chatId).map { rows -> rows.map(CachedMessageEntity::asModel) }

    suspend fun maxServerId(accountId: Long, chatId: Long): Long = dao.maxServerId(accountId, chatId)

    suspend fun mergeChats(accountId: Long, chats: List<ChatDto>) = database.withTransaction {
        chats.forEach { remote -> mergeChatLocked(accountId, remote) }
    }

    suspend fun mergeMessages(accountId: Long, currentUserId: Long, chatId: Long, data: MessagesData) =
        database.withTransaction {
            data.peerLastReadMessageId?.let { cursor ->
                dao.updatePeerReadCursor(accountId, chatId, cursor, now())
                dao.applyPeerReadCursor(accountId, chatId, cursor)
            }
            data.messages.forEach { remote -> mergeRemoteMessageLocked(accountId, currentUserId, remote) }
        }

    suspend fun queueOutgoing(accountId: Long, chatId: Long, senderId: Long, text: String): CachedMessage =
        database.withTransaction {
            val createdAt = now()
            val clientId = newClientId()
            val entity = CachedMessageEntity(
                localId = "client:$accountId:$clientId",
                accountId = accountId,
                serverId = null,
                clientMessageId = clientId,
                chatId = chatId,
                senderId = senderId,
                text = text,
                createdAtLocalMillis = createdAt,
                createdAtServerMillis = null,
                isMine = true,
                sendState = MessageSendState.PENDING.name,
                readByPeer = false,
                retryCount = 0,
                lastAttemptAtMillis = null,
                failureReason = null,
            )
            dao.insertMessage(entity)
            dao.chat(accountId, chatId)?.let { chat ->
                dao.upsertChat(
                    chat.copy(
                        lastMessageServerId = null,
                        lastMessageClientId = clientId,
                        lastMessageText = text,
                        lastMessageSenderId = senderId,
                        lastMessageCreatedAtMillis = createdAt,
                        lastMessageSendState = MessageSendState.PENDING.name,
                        updatedAtMillis = createdAt,
                    ),
                )
            }
            entity.asModel()
        }

    suspend fun acknowledge(accountId: Long, currentUserId: Long, message: MessageDto) =
        database.withTransaction { mergeRemoteMessageLocked(accountId, currentUserId, message) }

    suspend fun markLocallyRead(accountId: Long, chatId: Long, serverId: Long) {
        dao.updateMyReadCursor(accountId, chatId, serverId, now())
    }

    suspend fun clearUnread(accountId: Long, chatId: Long) = dao.clearUnread(accountId, chatId, now())

    suspend fun myLastRead(accountId: Long, chatId: Long): Long =
        dao.chat(accountId, chatId)?.myLastReadMessageId ?: 0L

    suspend fun pendingMessages(accountId: Long): List<CachedMessageEntity> = dao.pendingMessages(accountId)

    suspend fun pendingMessageCount(accountId: Long): Int = dao.pendingMessageCount(accountId)

    suspend fun clearAccountData(accountId: Long) = database.withTransaction {
        dao.deleteAccountMessages(accountId)
        dao.deleteAccountChats(accountId)
        dao.deleteAccountCurrentUser(accountId)
    }

    suspend fun recoverInterruptedSends(accountId: Long) = dao.recoverInterruptedSends(accountId)

    suspend fun markSending(localId: String) = dao.markSending(localId, now())

    suspend fun markPending(localId: String, reason: String? = null) = dao.markPending(localId, reason)

    suspend fun markFailed(localId: String, reason: String) = dao.markFailed(localId, reason)

    suspend fun retryFailed(localId: String, accountId: Long): Boolean {
        val message = dao.message(localId) ?: return false
        if (message.accountId != accountId || message.sendState != MessageSendState.FAILED.name) return false
        dao.markPending(localId)
        return true
    }

    private suspend fun mergeChatLocked(accountId: Long, remote: ChatDto) {
        val existing = dao.chat(accountId, remote.id)
        val remoteTime = remote.lastMessage?.let { parseServerTimestamp(it.createdAt) }
        val keepLocalPreview = existing?.lastMessageSendState in setOf(
            MessageSendState.PENDING.name,
            MessageSendState.SENDING.name,
            MessageSendState.FAILED.name,
        ) && (remoteTime == null || (existing?.lastMessageCreatedAtMillis ?: Long.MIN_VALUE) >= remoteTime)
        val peerCursor = maxOf(existing?.peerLastReadMessageId ?: 0L, remote.peerLastReadMessageId ?: 0L)
            .takeIf { it > 0L }
        val myCursor = maxOf(existing?.myLastReadMessageId ?: 0L, remote.myLastReadMessageId ?: 0L)
            .takeIf { it > 0L }
        val last = remote.lastMessage
        val remoteState = last?.takeIf { it.senderId == accountId }?.let {
            if (it.id <= (peerCursor ?: 0L)) MessageSendState.READ else MessageSendState.SENT
        }
        val updated = CachedChatEntity(
            accountId = accountId,
            chatId = remote.id,
            type = remote.type,
            peerUserId = remote.peer.id,
            peerUsername = remote.peer.username,
            peerDisplayName = remote.peer.displayName,
            peerCreatedAt = remote.peer.createdAt,
            peerLastSeenAtMillis = remote.peer.lastSeenAt?.let(::parseServerTimestamp),
            lastMessageServerId = if (keepLocalPreview) existing?.lastMessageServerId else last?.id,
            lastMessageClientId = if (keepLocalPreview) existing?.lastMessageClientId else last?.clientMessageId,
            lastMessageText = if (keepLocalPreview) existing?.lastMessageText else last?.text,
            lastMessageSenderId = if (keepLocalPreview) existing?.lastMessageSenderId else last?.senderId,
            lastMessageCreatedAtMillis = if (keepLocalPreview) existing?.lastMessageCreatedAtMillis else remoteTime,
            lastMessageSendState = if (keepLocalPreview) existing?.lastMessageSendState else remoteState?.name,
            unreadCount = remote.unreadCount.coerceAtLeast(0),
            peerLastReadMessageId = peerCursor,
            myLastReadMessageId = myCursor,
            updatedAtMillis = now(),
        )
        dao.upsertChat(updated)
        peerCursor?.let { dao.applyPeerReadCursor(accountId, remote.id, it) }
    }

    private suspend fun mergeRemoteMessageLocked(accountId: Long, currentUserId: Long, remote: MessageDto) {
        val byClient = remote.clientMessageId?.let { dao.messageByClientId(accountId, it) }
        val byServer = dao.messageByServerId(accountId, remote.id)
        if (byClient != null && byServer != null && byClient.localId != byServer.localId) {
            dao.deleteMessage(byServer.localId)
        }
        val existing = byClient ?: byServer
        val serverTime = parseServerTimestamp(remote.createdAt)
        val peerCursor = dao.chat(accountId, remote.chatId)?.peerLastReadMessageId ?: 0L
        val isMine = remote.senderId == currentUserId
        val state = if (isMine && remote.id <= peerCursor) MessageSendState.READ else MessageSendState.SENT
        val merged = CachedMessageEntity(
            localId = existing?.localId ?: "server:$accountId:${remote.id}",
            accountId = accountId,
            serverId = remote.id,
            clientMessageId = remote.clientMessageId ?: existing?.clientMessageId,
            chatId = remote.chatId,
            senderId = remote.senderId,
            text = remote.text,
            createdAtLocalMillis = serverTime ?: existing?.createdAtLocalMillis ?: now(),
            createdAtServerMillis = serverTime,
            isMine = isMine,
            sendState = state.name,
            readByPeer = state == MessageSendState.READ,
            retryCount = existing?.retryCount ?: 0,
            lastAttemptAtMillis = existing?.lastAttemptAtMillis,
            failureReason = null,
        )
        if (existing == null) dao.insertMessage(merged) else dao.updateMessage(merged)
        dao.chat(accountId, remote.chatId)?.let { chat ->
            val currentTime = chat.lastMessageCreatedAtMillis ?: Long.MIN_VALUE
            if (merged.createdAtLocalMillis >= currentTime || chat.lastMessageClientId == merged.clientMessageId) {
                dao.upsertChat(
                    chat.copy(
                        lastMessageServerId = merged.serverId,
                        lastMessageClientId = merged.clientMessageId,
                        lastMessageText = merged.text,
                        lastMessageSenderId = merged.senderId,
                        lastMessageCreatedAtMillis = merged.createdAtLocalMillis,
                        lastMessageSendState = if (merged.isMine) merged.sendState else null,
                        updatedAtMillis = now(),
                    ),
                )
            }
        }
    }
}

private fun CachedChatEntity.asModel() = CachedChat(
    id = chatId,
    type = type,
    peerUserId = peerUserId,
    peerUsername = peerUsername,
    peerDisplayName = peerDisplayName,
    peerLastSeenAtMillis = peerLastSeenAtMillis,
    lastMessage = lastMessageText?.let {
        CachedChatPreview(
            serverId = lastMessageServerId,
            clientMessageId = lastMessageClientId,
            text = it,
            senderId = lastMessageSenderId ?: 0L,
            createdAtMillis = lastMessageCreatedAtMillis ?: updatedAtMillis,
            sendState = lastMessageSendState?.let { value -> runCatching { MessageSendState.valueOf(value) }.getOrNull() },
        )
    },
    unreadCount = unreadCount.coerceAtLeast(0),
    myLastReadMessageId = myLastReadMessageId,
)

private fun CachedMessageEntity.asModel() = CachedMessage(
    localId = localId,
    serverId = serverId,
    clientMessageId = clientMessageId,
    chatId = chatId,
    senderId = senderId,
    text = text,
    createdAtMillis = createdAtServerMillis ?: createdAtLocalMillis,
    isMine = isMine,
    sendState = runCatching { MessageSendState.valueOf(sendState) }.getOrDefault(MessageSendState.PENDING),
    readByPeer = readByPeer,
    failureReason = failureReason,
)
