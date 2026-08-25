package com.example.bunbun.push

import android.content.Intent
import com.example.bunbun.MainActivity

data class PushMessagePayload(
    val chatId: Long,
    val messageId: Long,
    val senderId: Long,
    val senderName: String?,
    val preview: String?,
) {
    companion object {
        fun parse(data: Map<String, String>): PushMessagePayload? =
            (parseDetailed(data) as? PushPayloadParseResult.Valid)?.payload

        fun parseDetailed(data: Map<String, String>): PushPayloadParseResult {
            if (data["type"] != "new_message") {
                return PushPayloadParseResult.Invalid(PushDropReason.INVALID_TYPE)
            }
            val chatId = data["chat_id"]?.toLongOrNull()?.takeIf { it > 0 }
                ?: return PushPayloadParseResult.Invalid(PushDropReason.MISSING_CHAT_ID)
            val messageId = data["message_id"]?.toLongOrNull()?.takeIf { it > 0 }
                ?: return PushPayloadParseResult.Invalid(PushDropReason.MISSING_MESSAGE_ID)
            val senderId = data["sender_id"]?.toLongOrNull()?.takeIf { it > 0 }
                ?: return PushPayloadParseResult.Invalid(PushDropReason.MISSING_SENDER_ID)
            return PushPayloadParseResult.Valid(
                PushMessagePayload(
                    chatId = chatId,
                    messageId = messageId,
                    senderId = senderId,
                    senderName = data["sender_name"]?.trim()?.takeIf { it.isNotEmpty() },
                    preview = data["preview"]?.trim()?.takeIf { it.isNotEmpty() },
                ),
            )
        }
    }
}

enum class PushDropReason {
    INVALID_TYPE,
    MISSING_CHAT_ID,
    MISSING_MESSAGE_ID,
    MISSING_SENDER_ID,
    SIGNED_OUT,
    OWN_MESSAGE,
    SAME_ACTIVE_CHAT,
    SESSION_READ_FAILED,
    NOTIFICATIONS_DISABLED,
    PERMISSION_DENIED,
    CHANNEL_MISSING,
    CHANNEL_DISABLED,
    NOTIFICATION_STATE_FAILED,
    NOTIFY_FAILED,
}

sealed interface PushPayloadParseResult {
    data class Valid(val payload: PushMessagePayload) : PushPayloadParseResult
    data class Invalid(val reason: PushDropReason) : PushPayloadParseResult
}

data class PushSessionSnapshot(val active: Boolean, val accountId: Long?)

internal fun resolvePushSession(
    cachedSessionAvailable: Boolean,
    persistedSessionAvailable: Boolean,
    persistedAccountId: Long?,
): PushSessionSnapshot = PushSessionSnapshot(
    active = cachedSessionAvailable || persistedSessionAvailable,
    accountId = persistedAccountId,
)

data class ForegroundChatState(val appInForeground: Boolean, val activeChatId: Long?)

data class PushNotificationCommand(
    val payload: PushMessagePayload,
    val channelId: String = MESSAGE_NOTIFICATION_CHANNEL_ID,
    val notificationId: Int = notificationIdForMessage(payload.messageId),
)

sealed interface IncomingPushDecision {
    data class Notify(val command: PushNotificationCommand) : IncomingPushDecision
    data class Drop(val reason: PushDropReason) : IncomingPushDecision
}

object IncomingPushPolicy {
    fun decide(
        payload: PushMessagePayload,
        session: PushSessionSnapshot,
        foreground: ForegroundChatState,
    ): IncomingPushDecision {
        if (!session.active) return IncomingPushDecision.Drop(PushDropReason.SIGNED_OUT)
        if (session.accountId != null && payload.senderId == session.accountId) {
            return IncomingPushDecision.Drop(PushDropReason.OWN_MESSAGE)
        }
        if (NotificationSuppressionPolicy.shouldSuppress(
                foreground.appInForeground,
                foreground.activeChatId,
                payload.chatId,
            )
        ) {
            return IncomingPushDecision.Drop(PushDropReason.SAME_ACTIVE_CHAT)
        }
        return IncomingPushDecision.Notify(PushNotificationCommand(payload))
    }
}

internal const val BUNBUN_PUSH_TAG = "BunbunPush"
internal const val MESSAGE_NOTIFICATION_CHANNEL_ID = "bunbun_messages"

internal fun notificationIdForMessage(messageId: Long): Int =
    (messageId xor (messageId ushr 32)).toInt()

data class ChatNavigationTarget(val chatId: Long, val peerName: String) {
    fun putInto(intent: Intent): Intent = intent
        .putExtra(EXTRA_CHAT_ID, chatId)
        .putExtra(EXTRA_PEER_NAME, peerName)

    companion object {
        const val EXTRA_CHAT_ID = "com.example.bunbun.extra.CHAT_ID"
        const val EXTRA_PEER_NAME = "com.example.bunbun.extra.PEER_NAME"

        fun fromPayload(payload: PushMessagePayload) =
            ChatNavigationTarget(payload.chatId, payload.senderName ?: "Bunbun")

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
