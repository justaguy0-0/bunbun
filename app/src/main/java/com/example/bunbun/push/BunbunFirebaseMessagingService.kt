package com.example.bunbun.push

import android.annotation.SuppressLint
import com.example.bunbun.BunbunApplication
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// Firebase Messaging 25.x replaces onNewToken with onRegistered/FID. Current Android lint
// still checks only the legacy callback, so suppress that obsolete detector explicitly.
@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class BunbunFirebaseMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val container get() = (application as BunbunApplication).container

    override fun onRegistered(installationId: String) {
        scope.launch { runCatching { container.pushTokenSynchronizer.onRegistered(installationId) } }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val payload = PushMessagePayload.parse(message.data) ?: return
        scope.launch {
            val accountId = container.repository.authenticatedAccountId() ?: return@launch
            runCatching {
                container.repository.syncChats(accountId)
                container.repository.syncMessages(accountId, payload.chatId)
            }
            if (container.repository.authenticatedAccountId() != accountId) return@launch
            if (container.foregroundChatTracker.shouldSuppress(payload.chatId)) return@launch
            BunbunNotifications.show(this@BunbunFirebaseMessagingService, payload)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
