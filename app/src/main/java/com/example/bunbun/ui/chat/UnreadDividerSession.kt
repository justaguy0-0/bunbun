package com.example.bunbun.ui.chat

import com.example.bunbun.data.local.CachedMessage

data class UnreadDividerUiState(
    val firstUnreadIncomingMessageId: Long? = null,
    val visible: Boolean = false,
)

/** One instance belongs to one ChatViewModel and captures at most one boundary for that opening. */
class UnreadDividerSession {
    private var captureFinished = false
    private var hideScheduled = false

    var uiState: UnreadDividerUiState = UnreadDividerUiState()
        private set

    val hasCaptured: Boolean get() = captureFinished

    fun capture(
        messages: List<CachedMessage>,
        currentUserId: Long,
        myLastReadMessageId: Long?,
        initialDataReady: Boolean,
    ): UnreadDividerUiState {
        if (captureFinished || !initialDataReady) return uiState
        captureFinished = true
        val boundary = firstUnreadIncomingMessageId(messages, myLastReadMessageId, currentUserId)
        uiState = UnreadDividerUiState(boundary, visible = boundary != null)
        return uiState
    }

    /** Returns true exactly once when the five-second hide job should be started. */
    fun onReadConfirmed(): Boolean {
        if (uiState.firstUnreadIncomingMessageId == null || hideScheduled) return false
        hideScheduled = true
        return true
    }

    fun onHoldTimeout(): UnreadDividerUiState {
        uiState = uiState.copy(visible = false)
        return uiState
    }

    fun finishFade(): UnreadDividerUiState {
        uiState = UnreadDividerUiState()
        return uiState
    }
}

fun firstUnreadIncomingMessageId(
    messages: List<CachedMessage>,
    myLastReadMessageId: Long?,
    currentUserId: Long,
): Long? {
    val cursor = myLastReadMessageId ?: 0L
    return messages.asSequence()
        .filter { message ->
            val serverId = message.serverId
            serverId != null && serverId > cursor && message.senderId != currentUserId
        }
        .mapNotNull(CachedMessage::serverId)
        .minOrNull()
}
