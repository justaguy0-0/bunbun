package com.example.bunbun.ui.chat

/** One instance belongs to one ChatScreen composition and positions its first real timeline once. */
class ChatInitialScrollState {
    private var completed = false

    fun consumeTargetIndex(itemCount: Int): Int? {
        if (completed || itemCount <= 0) return null
        completed = true
        return itemCount - 1
    }
}
