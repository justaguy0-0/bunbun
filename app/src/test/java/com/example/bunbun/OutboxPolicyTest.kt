package com.example.bunbun

import com.example.bunbun.data.local.MessageSendState
import com.example.bunbun.outbox.OutboxFailureDisposition
import com.example.bunbun.outbox.classifyOutboxHttpFailure
import com.example.bunbun.ui.chat.messageStatusSymbol
import org.junit.Assert.assertEquals
import org.junit.Test

class OutboxPolicyTest {
    @Test fun authFailureKeepsMessageForLogin() {
        assertEquals(OutboxFailureDisposition.AUTH_REQUIRED, classifyOutboxHttpFailure(401))
    }

    @Test fun temporaryFailuresRetry() {
        assertEquals(OutboxFailureDisposition.RETRY, classifyOutboxHttpFailure(408))
        assertEquals(OutboxFailureDisposition.RETRY, classifyOutboxHttpFailure(429))
        assertEquals(OutboxFailureDisposition.RETRY, classifyOutboxHttpFailure(503))
    }

    @Test fun permanentClientFailureIsVisible() {
        assertEquals(OutboxFailureDisposition.FAILED, classifyOutboxHttpFailure(422))
        assertEquals("!", messageStatusSymbol(MessageSendState.FAILED))
    }

    @Test fun statusVisualsAreDistinct() {
        assertEquals("◷", messageStatusSymbol(MessageSendState.PENDING))
        assertEquals("✓", messageStatusSymbol(MessageSendState.SENT))
        assertEquals("✓✓", messageStatusSymbol(MessageSendState.READ))
    }
}
