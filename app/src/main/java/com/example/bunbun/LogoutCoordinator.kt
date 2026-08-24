package com.example.bunbun

import com.example.bunbun.data.repository.BunbunRepository
import com.example.bunbun.presence.PresenceSynchronizer
import com.example.bunbun.push.ForegroundChatTracker
import com.example.bunbun.push.PendingChatNavigation

class LogoutCoordinator(
    private val presence: PresenceSynchronizer,
    private val repository: BunbunRepository,
    private val foregroundChatTracker: ForegroundChatTracker,
    private val pendingChatNavigation: PendingChatNavigation,
    private val clearNotifications: () -> Unit,
) {
    suspend fun logout() {
        presence.stop()
        foregroundChatTracker.clear()
        pendingChatNavigation.clear()
        runCatching(clearNotifications)
        repository.logout()
    }
}
