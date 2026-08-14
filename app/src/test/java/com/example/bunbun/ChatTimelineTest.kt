package com.example.bunbun

import com.example.bunbun.data.local.CachedMessage
import com.example.bunbun.data.local.MessageSendState
import com.example.bunbun.ui.chat.ChatTimelineItem
import com.example.bunbun.ui.chat.buildChatTimeline
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

class ChatTimelineTest {
    private val zone = ZoneId.of("Europe/Saratov")
    private val locale = Locale("ru")

    @Test fun groupsTodayYesterdayAndPreviousYear() {
        val now = Instant.parse("2026-08-14T12:00:00Z").toEpochMilli()
        val items = buildChatTimeline(
            listOf(
                message("old", "2025-12-31T10:00:00Z"),
                message("yesterday", "2026-08-12T21:00:00Z"),
                message("today", "2026-08-13T21:00:00Z"),
            ),
            now,
            zone,
            locale,
        )
        val labels = items.filterIsInstance<ChatTimelineItem.DateHeader>().map { it.label }
        assertEquals(listOf("31 ДЕКАБРЯ 2025", "ВЧЕРА", "СЕГОДНЯ"), labels)
    }

    @Test fun utcDayBoundaryUsesUserTimezone() {
        val now = Instant.parse("2026-08-14T20:00:00Z").toEpochMilli()
        val items = buildChatTimeline(
            listOf(message("before", "2026-08-14T19:59:59Z"), message("after", "2026-08-14T20:00:01Z")),
            now,
            zone,
            locale,
        )
        assertEquals(2, items.filterIsInstance<ChatTimelineItem.DateHeader>().size)
    }

    @Test fun stableKeysUseLocalIdentity() {
        val items = buildChatTimeline(listOf(message("same-client-id", "2026-08-14T12:00:00Z")), zoneId = zone)
        assertEquals("message:same-client-id", items.filterIsInstance<ChatTimelineItem.Message>().single().key)
    }

    private fun message(id: String, instant: String) = CachedMessage(
        localId = id,
        serverId = null,
        clientMessageId = id,
        chatId = 1,
        senderId = 1,
        text = id,
        createdAtMillis = Instant.parse(instant).toEpochMilli(),
        isMine = true,
        sendState = MessageSendState.PENDING,
        readByPeer = false,
        failureReason = null,
    )
}
