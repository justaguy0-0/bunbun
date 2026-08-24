package com.example.bunbun.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BunbunDao {
    @Query("SELECT * FROM cached_chats WHERE accountId = :accountId ORDER BY COALESCE(lastMessageCreatedAtMillis, updatedAtMillis) DESC, chatId DESC")
    fun observeChats(accountId: Long): Flow<List<CachedChatEntity>>

    @Query("SELECT * FROM cached_messages WHERE accountId = :accountId AND chatId = :chatId ORDER BY createdAtLocalMillis ASC, localId ASC")
    fun observeMessages(accountId: Long, chatId: Long): Flow<List<CachedMessageEntity>>

    @Query("SELECT COUNT(*) FROM cached_chats WHERE accountId = :accountId")
    suspend fun chatCount(accountId: Long): Int

    @Query("SELECT * FROM cached_current_users WHERE accountId = :accountId LIMIT 1")
    suspend fun currentUser(accountId: Long): CachedCurrentUserEntity?

    @Upsert
    suspend fun upsertCurrentUser(user: CachedCurrentUserEntity)

    @Query("SELECT * FROM cached_chats WHERE accountId = :accountId AND chatId = :chatId LIMIT 1")
    suspend fun chat(accountId: Long, chatId: Long): CachedChatEntity?

    @Query("SELECT * FROM cached_chats WHERE accountId = :accountId AND chatId = :chatId LIMIT 1")
    fun observeChat(accountId: Long, chatId: Long): Flow<CachedChatEntity?>

    @Upsert
    suspend fun upsertChat(chat: CachedChatEntity)

    @Query("SELECT COALESCE(MAX(serverId), 0) FROM cached_messages WHERE accountId = :accountId AND chatId = :chatId")
    suspend fun maxServerId(accountId: Long, chatId: Long): Long

    @Query("SELECT * FROM cached_messages WHERE accountId = :accountId AND serverId = :serverId LIMIT 1")
    suspend fun messageByServerId(accountId: Long, serverId: Long): CachedMessageEntity?

    @Query("SELECT * FROM cached_messages WHERE accountId = :accountId AND clientMessageId = :clientMessageId LIMIT 1")
    suspend fun messageByClientId(accountId: Long, clientMessageId: String): CachedMessageEntity?

    @Query("SELECT * FROM cached_messages WHERE localId = :localId LIMIT 1")
    suspend fun message(localId: String): CachedMessageEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMessage(message: CachedMessageEntity)

    @Update
    suspend fun updateMessage(message: CachedMessageEntity)

    @Query("DELETE FROM cached_messages WHERE localId = :localId")
    suspend fun deleteMessage(localId: String)

    @Query("SELECT * FROM cached_messages WHERE accountId = :accountId AND sendState IN ('PENDING', 'SENDING') ORDER BY createdAtLocalMillis ASC, localId ASC")
    suspend fun pendingMessages(accountId: Long): List<CachedMessageEntity>

    @Query("UPDATE cached_messages SET sendState = 'SENDING', retryCount = retryCount + 1, lastAttemptAtMillis = :attemptAt, failureReason = NULL WHERE localId = :localId")
    suspend fun markSending(localId: String, attemptAt: Long)

    @Query("UPDATE cached_messages SET sendState = 'PENDING', failureReason = :reason WHERE localId = :localId")
    suspend fun markPending(localId: String, reason: String? = null)

    @Query("UPDATE cached_messages SET sendState = 'FAILED', failureReason = :reason WHERE localId = :localId")
    suspend fun markFailed(localId: String, reason: String)

    @Query("UPDATE cached_messages SET sendState = 'PENDING' WHERE accountId = :accountId AND sendState = 'SENDING'")
    suspend fun recoverInterruptedSends(accountId: Long)

    @Query("UPDATE cached_messages SET sendState = 'READ', readByPeer = 1 WHERE accountId = :accountId AND chatId = :chatId AND isMine = 1 AND serverId IS NOT NULL AND serverId <= :cursor AND sendState IN ('SENT', 'READ')")
    suspend fun applyPeerReadCursor(accountId: Long, chatId: Long, cursor: Long)

    @Query("UPDATE cached_chats SET peerLastReadMessageId = MAX(COALESCE(peerLastReadMessageId, 0), :cursor), updatedAtMillis = :updatedAt WHERE accountId = :accountId AND chatId = :chatId")
    suspend fun updatePeerReadCursor(accountId: Long, chatId: Long, cursor: Long, updatedAt: Long)

    @Query("UPDATE cached_chats SET myLastReadMessageId = MAX(COALESCE(myLastReadMessageId, 0), :cursor), unreadCount = 0, updatedAtMillis = :updatedAt WHERE accountId = :accountId AND chatId = :chatId")
    suspend fun updateMyReadCursor(accountId: Long, chatId: Long, cursor: Long, updatedAt: Long)

    @Query("UPDATE cached_chats SET unreadCount = 0, updatedAtMillis = :updatedAt WHERE accountId = :accountId AND chatId = :chatId")
    suspend fun clearUnread(accountId: Long, chatId: Long, updatedAt: Long)
}
