package com.example.bunbun

import com.example.bunbun.data.local.CachedMessage
import com.example.bunbun.data.local.MessageSendState
import com.example.bunbun.ui.chat.ChatTimelineItem
import com.example.bunbun.ui.chat.UnreadDividerSession
import com.example.bunbun.ui.chat.buildChatTimeline
import com.example.bunbun.ui.chat.firstUnreadIncomingMessageId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class UnreadDividerSessionTest {
    @Test fun noUnreadProducesNoDivider() {
        val state = session().capture(messages(1), ME, 1, initialDataReady = true)

        assertNull(state.firstUnreadIncomingMessageId)
        assertFalse(state.visible)
    }

    @Test fun oneUnreadIncomingProducesDividerBeforeIt() {
        val unread = incoming(2)
        val state = session().capture(listOf(outgoing(1), unread), ME, 1, true)
        val timeline = buildChatTimeline(listOf(outgoing(1), unread), firstUnreadIncomingMessageId = state.firstUnreadIncomingMessageId)

        assertEquals(2L, state.firstUnreadIncomingMessageId)
        assertEquals(
            listOf(ChatTimelineItem.NewMessagesDivider::class, ChatTimelineItem.Message::class),
            timeline.takeLast(2).map { it::class },
        )
    }

    @Test fun severalUnreadIncomingProduceOneDividerBeforeTheFirst() {
        val source = listOf(incoming(2), incoming(3), incoming(4))
        val boundary = firstUnreadIncomingMessageId(source, 1, ME)
        val timeline = buildChatTimeline(source, firstUnreadIncomingMessageId = boundary)

        assertEquals(2L, boundary)
        assertEquals(1, timeline.filterIsInstance<ChatTimelineItem.NewMessagesDivider>().size)
    }

    @Test fun outgoingAfterCursorNeverBecomesBoundary() {
        assertNull(firstUnreadIncomingMessageId(listOf(outgoing(8)), 7, ME))
    }

    @Test fun cursorAndIncomingOutgoingMixSelectCorrectFirstIncoming() {
        val source = listOf(incoming(8), outgoing(10), incoming(11), outgoing(12), incoming(13))

        assertEquals(11L, firstUnreadIncomingMessageId(source, 10, ME))
    }

    @Test fun pollingCannotMoveCapturedBoundary() {
        val session = session()
        session.capture(listOf(incoming(11)), ME, 10, true)

        val afterPoll = session.capture(listOf(incoming(11), incoming(12)), ME, 11, true)

        assertEquals(11L, afterPoll.firstUnreadIncomingMessageId)
    }

    @Test fun boundaryIsNotCapturedBeforeInitialReadCursorIsReady() {
        val session = session()
        val beforeSync = session.capture(listOf(incoming(2)), ME, null, initialDataReady = false)
        val afterSync = session.capture(listOf(incoming(11)), ME, 10, initialDataReady = true)

        assertNull(beforeSync.firstUnreadIncomingMessageId)
        assertEquals(11L, afterSync.firstUnreadIncomingMessageId)
    }

    @Test fun dividerFadesOnlyAfterReadConfirmationAndIsThenRemoved() {
        val session = session()
        session.capture(listOf(incoming(11)), ME, 10, true)

        assertTrue(session.onReadConfirmed())
        assertFalse(session.onReadConfirmed())
        assertFalse(session.onHoldTimeout().visible)
        assertNull(session.finishFade().firstUnreadIncomingMessageId)
    }

    @Test fun reopeningChatCreatesFreshBoundary() {
        val firstOpening = session().capture(listOf(incoming(11)), ME, 10, true)
        val secondOpening = session().capture(listOf(incoming(15)), ME, 14, true)

        assertEquals(11L, firstOpening.firstUnreadIncomingMessageId)
        assertEquals(15L, secondOpening.firstUnreadIncomingMessageId)
    }

    @Test fun dateHeaderRemainsBeforeNewMessagesDivider() {
        val timeline = buildChatTimeline(
            messages = listOf(incoming(11)),
            nowMillis = Instant.parse("2026-08-25T12:00:00Z").toEpochMilli(),
            zoneId = ZoneId.of("UTC"),
            firstUnreadIncomingMessageId = 11,
        )

        assertTrue(timeline[0] is ChatTimelineItem.DateHeader)
        assertTrue(timeline[1] is ChatTimelineItem.NewMessagesDivider)
        assertTrue(timeline[2] is ChatTimelineItem.Message)
    }

    private fun session() = UnreadDividerSession()

    private fun messages(id: Long) = listOf(incoming(id))

    private fun incoming(id: Long) = message(id, senderId = PEER)

    private fun outgoing(id: Long) = message(id, senderId = ME)

    private fun message(id: Long, senderId: Long) = CachedMessage(
        localId = "server:$id",
        serverId = id,
        clientMessageId = null,
        chatId = 1,
        senderId = senderId,
        text = "message $id",
        createdAtMillis = Instant.parse("2026-08-25T12:00:00Z").toEpochMilli() + id,
        isMine = senderId == ME,
        sendState = MessageSendState.SENT,
        readByPeer = false,
        failureReason = null,
    )

    private companion object {
        const val ME = 1L
        const val PEER = 2L
    }
}
