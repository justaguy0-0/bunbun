package com.example.bunbun

import com.example.bunbun.push.ChatNavigationTarget
import com.example.bunbun.push.NotificationSuppressionPolicy
import com.example.bunbun.push.PushMessagePayload
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

    @Test fun rejectsMalformedPayload() {
        assertNull(PushMessagePayload.parse(valid - "chat_id"))
        assertNull(PushMessagePayload.parse(valid + ("chat_id" to "not-a-number")))
        assertNull(PushMessagePayload.parse(valid + ("type" to "unknown")))
        assertNull(PushMessagePayload.parse(valid + ("preview" to " ")))
    }

    @Test fun suppressesOnlyForegroundActiveChat() {
        assertTrue(NotificationSuppressionPolicy.shouldSuppress(true, 42L, 42L))
        assertFalse(NotificationSuppressionPolicy.shouldSuppress(false, 42L, 42L))
        assertFalse(NotificationSuppressionPolicy.shouldSuppress(true, 7L, 42L))
        assertFalse(NotificationSuppressionPolicy.shouldSuppress(true, null, 42L))
    }
}
