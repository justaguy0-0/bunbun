package com.example.bunbun.ui.chat

/** Edge detector scoped to one ChatScreen composition. */
class ImeOpenScrollState(initiallyVisible: Boolean = false) {
    private var wasVisible = initiallyVisible

    fun onVisibilityChanged(isVisible: Boolean): Boolean {
        val shouldScroll = !wasVisible && isVisible
        wasVisible = isVisible
        return shouldScroll
    }
}
