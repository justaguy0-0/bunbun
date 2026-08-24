package com.example.bunbun.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "cached_current_users",
    primaryKeys = ["accountId"],
)
data class CachedCurrentUserEntity(
    val accountId: Long,
    val username: String,
    val displayName: String,
    val createdAt: String,
    val updatedAtMillis: Long,
)

@Entity(
    tableName = "cached_chats",
    primaryKeys = ["accountId", "chatId"],
    indices = [
        Index(value = ["accountId", "lastMessageCreatedAtMillis"]),
        Index(value = ["accountId", "updatedAtMillis"]),
    ],
)
data class CachedChatEntity(
    val accountId: Long,
    val chatId: Long,
    val type: String,
    val peerUserId: Long,
    val peerUsername: String,
    val peerDisplayName: String,
    val peerCreatedAt: String,
    val peerLastSeenAtMillis: Long?,
    val lastMessageServerId: Long?,
    val lastMessageClientId: String?,
    val lastMessageText: String?,
    val lastMessageSenderId: Long?,
    val lastMessageCreatedAtMillis: Long?,
    val lastMessageSendState: String?,
    val unreadCount: Int,
    val peerLastReadMessageId: Long?,
    val myLastReadMessageId: Long?,
    val updatedAtMillis: Long,
)

@Entity(
    tableName = "cached_messages",
    indices = [
        Index(value = ["accountId", "chatId", "createdAtLocalMillis"]),
        Index(value = ["accountId", "serverId"], unique = true),
        Index(value = ["accountId", "clientMessageId"], unique = true),
        Index(value = ["accountId", "sendState", "createdAtLocalMillis"]),
    ],
)
data class CachedMessageEntity(
    @androidx.room.PrimaryKey val localId: String,
    val accountId: Long,
    val serverId: Long?,
    val clientMessageId: String?,
    val chatId: Long,
    val senderId: Long,
    val text: String,
    val createdAtLocalMillis: Long,
    val createdAtServerMillis: Long?,
    val isMine: Boolean,
    val sendState: String,
    val readByPeer: Boolean,
    val retryCount: Int,
    val lastAttemptAtMillis: Long?,
    val failureReason: String?,
)
