package com.example.bunbun.push

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class ForegroundChatTracker {
    private val foreground = AtomicBoolean(false)
    private val activeChat = AtomicLong(NO_CHAT)

    fun setForeground(value: Boolean) = foreground.set(value)
    fun setActiveChat(chatId: Long) = activeChat.set(chatId)
    fun clearActiveChat(chatId: Long) = activeChat.compareAndSet(chatId, NO_CHAT)
    fun clear() = activeChat.set(NO_CHAT)
    fun snapshot() = ForegroundChatState(
        appInForeground = foreground.get(),
        activeChatId = activeChat.get().takeUnless { it == NO_CHAT },
    )
    fun shouldSuppress(chatId: Long): Boolean = snapshot().let {
        NotificationSuppressionPolicy.shouldSuppress(it.appInForeground, it.activeChatId, chatId)
    }

    private companion object { const val NO_CHAT = -1L }
}

class PendingChatNavigation {
    private val mutableTarget = MutableStateFlow<ChatNavigationTarget?>(null)
    val target: StateFlow<ChatNavigationTarget?> = mutableTarget.asStateFlow()

    fun post(target: ChatNavigationTarget) { mutableTarget.value = target }
    fun consume(target: ChatNavigationTarget) { mutableTarget.compareAndSet(target, null) }
    fun clear() { mutableTarget.value = null }
}

class NotificationPermissionPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("bunbun_push", Context.MODE_PRIVATE)
    fun wasPrompted(): Boolean = preferences.getBoolean(KEY_PROMPTED, false)
    fun markPrompted() { preferences.edit().putBoolean(KEY_PROMPTED, true).apply() }

    private companion object { const val KEY_PROMPTED = "notification_permission_prompted" }
}
