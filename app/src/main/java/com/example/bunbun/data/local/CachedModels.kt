package com.example.bunbun.data.local

enum class MessageSendState {
    PENDING,
    SENDING,
    SENT,
    READ,
    FAILED,
}

data class CachedChat(
    val id: Long,
    val type: String,
    val peerUserId: Long,
    val peerUsername: String,
    val peerDisplayName: String,
    val lastMessage: CachedChatPreview?,
    val unreadCount: Int,
)

data class CachedChatPreview(
    val serverId: Long?,
    val clientMessageId: String?,
    val text: String,
    val senderId: Long,
    val createdAtMillis: Long,
    val sendState: MessageSendState?,
)

data class CachedMessage(
    val localId: String,
    val serverId: Long?,
    val clientMessageId: String?,
    val chatId: Long,
    val senderId: Long,
    val text: String,
    val createdAtMillis: Long,
    val isMine: Boolean,
    val sendState: MessageSendState,
    val readByPeer: Boolean,
    val failureReason: String?,
) {
    val stableKey: String get() = "message:$localId"
}
