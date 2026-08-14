package com.example.bunbun

import com.example.bunbun.ui.chat.ChatInitialScrollState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatInitialScrollStateTest {
    @Test fun waitsForFirstNonEmptyTimelineAndTargetsItsLastUiItem() {
        val state = ChatInitialScrollState()

        assertNull(state.consumeTargetIndex(0))
        assertEquals(104, state.consumeTargetIndex(105))
    }

    @Test fun roomPollingAndStatusUpdatesCannotRepeatInitialScroll() {
        val state = ChatInitialScrollState()

        assertEquals(7, state.consumeTargetIndex(8))
        assertNull(state.consumeTargetIndex(8))
        assertNull(state.consumeTargetIndex(10))
    }

    @Test fun newScreenInstancePositionsTheSameChatAgain() {
        val firstEntry = ChatInitialScrollState()
        val secondEntry = ChatInitialScrollState()

        assertEquals(11, firstEntry.consumeTargetIndex(12))
        assertEquals(11, secondEntry.consumeTargetIndex(12))
    }
}
