package com.example.bunbun

import com.example.bunbun.data.model.MessageDto
import com.example.bunbun.ui.common.lastMessageId
import com.example.bunbun.ui.common.mergeMessages
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageMergeTest {
    @Test
    fun duplicateFromSendAndPollingIsKeptOnce() {
        val message = message(7)
        assertEquals(listOf(message), mergeMessages(listOf(message), listOf(message)))
    }

    @Test
    fun messagesAreSortedAndLastIdIsStable() {
        val merged = mergeMessages(listOf(message(9)), listOf(message(3), message(10)))
        assertEquals(listOf(3L, 9L, 10L), merged.map { it.id })
        assertEquals(10L, lastMessageId(merged))
    }

    private fun message(id: Long) = MessageDto(id, 1, 2, "text", "2026-01-01 00:00:00")
}

