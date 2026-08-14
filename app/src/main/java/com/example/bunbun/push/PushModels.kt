package com.example.bunbun.push

import android.content.Intent
import com.example.bunbun.MainActivity

data class PushMessagePayload(
    val chatId: Long,
    val messageId: Long,
    val senderId: Long,
    val senderName: String,
    val preview: String,
) {
    companion object {
        fun parse(data: Map<String, String>): PushMessagePayload? {
            if (data["type"] != "new_message") return null
            val chatId = data["chat_id"]?.toLongOrNull()?.takeIf { it > 0 } ?: return null
            val messageId = data["message_id"]?.toLongOrNull()?.takeIf { it > 0 } ?: return null
            val senderId = data["sender_id"]?.toLongOrNull()?.takeIf { it > 0 } ?: return null
            val senderName = data["sender_name"]?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val preview = data["preview"]?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            return PushMessagePayload(chatId, messageId, senderId, senderName, preview)
        }
    }
}

data class ChatNavigationTarget(val chatId: Long, val peerName: String) {
    fun putInto(intent: Intent): Intent = intent
        .putExtra(EXTRA_CHAT_ID, chatId)
        .putExtra(EXTRA_PEER_NAME, peerName)

    companion object {
        const val EXTRA_CHAT_ID = "com.example.bunbun.extra.CHAT_ID"
        const val EXTRA_PEER_NAME = "com.example.bunbun.extra.PEER_NAME"

        fun fromPayload(payload: PushMessagePayload) = ChatNavigationTarget(payload.chatId, payload.senderName)

        fun fromIntent(intent: Intent?): ChatNavigationTarget? {
            if (intent?.component?.className != null && intent.component?.className != MainActivity::class.java.name) return null
            val chatId = intent?.getLongExtra(EXTRA_CHAT_ID, -1L)?.takeIf { it > 0 } ?: return null
            val peerName = intent.getStringExtra(EXTRA_PEER_NAME)?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            return ChatNavigationTarget(chatId, peerName)
        }
    }
}

object NotificationSuppressionPolicy {
    fun shouldSuppress(appInForeground: Boolean, activeChatId: Long?, incomingChatId: Long): Boolean =
        appInForeground && activeChatId == incomingChatId
}
