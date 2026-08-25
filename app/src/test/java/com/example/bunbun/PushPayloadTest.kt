package com.example.bunbun

import com.example.bunbun.push.ChatNavigationTarget
import com.example.bunbun.push.ForegroundChatState
import com.example.bunbun.push.IncomingPushDecision
import com.example.bunbun.push.IncomingPushPolicy
import com.example.bunbun.push.MESSAGE_NOTIFICATION_CHANNEL_ID
import com.example.bunbun.push.NotificationSuppressionPolicy
import com.example.bunbun.push.PushDropReason
import com.example.bunbun.push.PushMessagePayload
import com.example.bunbun.push.PushPayloadParseResult
import com.example.bunbun.push.PushSessionSnapshot
import com.example.bunbun.push.notificationIdForMessage
import com.example.bunbun.push.resolvePushSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PushPayloadTest {
    private val valid = mapOf(
        "type" to "new_message",
        "chat_id" to "42",
        "message_id" to "81",
        "sender_id" to "7",
        "sender_name" to "Илья",
        "preview" to "Привет",
    )

    @Test fun parsesValidPayloadAndNavigationChatId() {
        val payload = PushMessagePayload.parse(valid)!!
        assertEquals(42L, payload.chatId)
        assertEquals(42L, ChatNavigationTarget.fromPayload(payload).chatId)
    }

    @Test fun acceptsNewMessageAndRejectsUnknownType() {
        assertTrue(PushMessagePayload.parseDetailed(valid) is PushPayloadParseResult.Valid)
        assertEquals(
            PushDropReason.INVALID_TYPE,
            (PushMessagePayload.parseDetailed(valid + ("type" to "unknown")) as PushPayloadParseResult.Invalid).reason,
        )
    }

    @Test fun malformedRequiredChatIdIsRejected() {
        assertNull(PushMessagePayload.parse(valid - "chat_id"))
        assertNull(PushMessagePayload.parse(valid + ("chat_id" to "not-a-number")))
    }

    @Test fun optionalDisplayFieldsDoNotDestroyPush() {
        val payload = PushMessagePayload.parse(valid - "sender_name" - "preview")!!
        assertNull(payload.senderName)
        assertNull(payload.preview)
        assertEquals("Bunbun", ChatNavigationTarget.fromPayload(payload).peerName)
    }

    @Test fun suppressesOnlyForegroundActiveChat() {
        assertTrue(NotificationSuppressionPolicy.shouldSuppress(true, 42L, 42L))
        assertFalse(NotificationSuppressionPolicy.shouldSuppress(false, 42L, 42L))
        assertFalse(NotificationSuppressionPolicy.shouldSuppress(true, 7L, 42L))
        assertFalse(NotificationSuppressionPolicy.shouldSuppress(true, null, 42L))
    }

    @Test fun signedOutDropsButPersistedSignedInSessionNotLoadedInMemoryNotifies() {
        val payload = PushMessagePayload.parse(valid)!!
        assertEquals(
            PushDropReason.SIGNED_OUT,
            (IncomingPushPolicy.decide(payload, PushSessionSnapshot(false, null), chatsScreen()) as IncomingPushDecision.Drop).reason,
        )

        val restored = resolvePushSession(
            cachedSessionAvailable = false,
            persistedSessionAvailable = true,
            persistedAccountId = null,
        )
        assertTrue(restored.active)
        assertTrue(IncomingPushPolicy.decide(payload, restored, chatsScreen()) is IncomingPushDecision.Notify)
    }

    @Test fun ownSenderDropsAndOtherSenderNotifies() {
        val payload = PushMessagePayload.parse(valid)!!
        assertEquals(
            PushDropReason.OWN_MESSAGE,
            (IncomingPushPolicy.decide(payload, PushSessionSnapshot(true, 7L), chatsScreen()) as IncomingPushDecision.Drop).reason,
        )
        assertTrue(
            IncomingPushPolicy.decide(payload, PushSessionSnapshot(true, 8L), chatsScreen()) is IncomingPushDecision.Notify,
        )
    }

    @Test fun sameChatSuppressesOnlyInForegroundWhileChatsScreenAndBackgroundNotify() {
        val payload = PushMessagePayload.parse(valid)!!
        assertEquals(
            PushDropReason.SAME_ACTIVE_CHAT,
            (IncomingPushPolicy.decide(
                payload,
                PushSessionSnapshot(true, 99L),
                ForegroundChatState(true, payload.chatId),
            ) as IncomingPushDecision.Drop).reason,
        )
        assertTrue(IncomingPushPolicy.decide(payload, PushSessionSnapshot(true, 99L), chatsScreen()) is IncomingPushDecision.Notify)
        assertTrue(
            IncomingPushPolicy.decide(
                payload,
                PushSessionSnapshot(true, 99L),
                ForegroundChatState(false, payload.chatId),
            ) is IncomingPushDecision.Notify,
        )
    }

    @Test fun validPayloadCreatesStableNotificationCommandUsingBuilderChannel() {
        val payload = PushMessagePayload.parse(valid)!!
        val command = (IncomingPushPolicy.decide(
            payload,
            PushSessionSnapshot(true, 99L),
            chatsScreen(),
        ) as IncomingPushDecision.Notify).command

        assertEquals(MESSAGE_NOTIFICATION_CHANNEL_ID, command.channelId)
        assertEquals(81, command.notificationId)
        assertEquals(notificationIdForMessage(payload.messageId), command.notificationId)
    }

    private fun chatsScreen() = ForegroundChatState(appInForeground = true, activeChatId = null)
}
