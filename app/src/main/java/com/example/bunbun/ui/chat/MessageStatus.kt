package com.example.bunbun.ui.chat

import com.example.bunbun.data.local.MessageSendState

fun messageStatusSymbol(state: MessageSendState): String = when (state) {
    MessageSendState.PENDING, MessageSendState.SENDING -> "◷"
    MessageSendState.SENT -> "✓"
    MessageSendState.READ -> "✓✓"
    MessageSendState.FAILED -> "!"
}
