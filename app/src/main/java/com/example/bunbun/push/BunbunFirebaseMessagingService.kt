package com.example.bunbun.push

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.util.Log
import com.example.bunbun.BunbunApplication
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

// Firebase Messaging 25.x replaces onNewToken with onRegistered/FID. Current Android lint
// still checks only the legacy callback, so suppress that obsolete detector explicitly.
@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class BunbunFirebaseMessagingService : FirebaseMessagingService() {
    private val container get() = (application as BunbunApplication).container

    override fun onCreate() {
        super.onCreate()
        Log.i(BUNBUN_PUSH_TAG, "service created")
    }

    override fun onRegistered(installationId: String) {
        Log.i(BUNBUN_PUSH_TAG, "onRegistered received present=${installationId.isNotBlank()}")
        if (installationId.isBlank()) {
            Log.i(BUNBUN_PUSH_TAG, "dropped reason=EMPTY_INSTALLATION_ID")
            return
        }
        container.handleFirebaseRegistration(installationId)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.i(BUNBUN_PUSH_TAG, "onMessageReceived messageId=${message.messageId ?: "none"}")
        Log.i(BUNBUN_PUSH_TAG, "data keys=${message.data.keys.sorted()}")
        Log.i(BUNBUN_PUSH_TAG, "type=${message.data["type"] ?: "missing"}")

        val parseResult = PushMessagePayload.parseDetailed(message.data)
        if (parseResult is PushPayloadParseResult.Invalid) {
            Log.i(BUNBUN_PUSH_TAG, "payload parsed=false")
            Log.i(BUNBUN_PUSH_TAG, "dropped reason=${parseResult.reason}")
            return
        }
        val payload = (parseResult as PushPayloadParseResult.Valid).payload
        Log.i(
            BUNBUN_PUSH_TAG,
            "payload parsed=true messageId=${payload.messageId} chatId=${payload.chatId} senderId=${payload.senderId}",
        )

        val session = runCatching { container.pushSessionSnapshot() }.getOrElse {
            Log.w(BUNBUN_PUSH_TAG, "session read failed type=${it.javaClass.simpleName}")
            Log.i(BUNBUN_PUSH_TAG, "dropped reason=${PushDropReason.SESSION_READ_FAILED}")
            return
        }
        Log.i(BUNBUN_PUSH_TAG, "session active=${session.active} accountId=${session.accountId ?: "none"}")

        val foreground = container.foregroundChatTracker.snapshot()
        when (val decision = IncomingPushPolicy.decide(payload, session, foreground)) {
            is IncomingPushDecision.Drop -> {
                Log.i(BUNBUN_PUSH_TAG, "dropped reason=${decision.reason}")
                return
            }
            is IncomingPushDecision.Notify -> showNotification(decision.command, session)
        }
    }

    private fun showNotification(command: PushNotificationCommand, session: PushSessionSnapshot) {
        Log.i(BUNBUN_PUSH_TAG, "suppressed=false")
        val state = runCatching { BunbunNotifications.state(this) }.getOrElse {
            Log.w(BUNBUN_PUSH_TAG, "notification state failed type=${it.javaClass.simpleName}")
            Log.i(BUNBUN_PUSH_TAG, "dropped reason=${PushDropReason.NOTIFICATION_STATE_FAILED}")
            return
        }
        Log.i(BUNBUN_PUSH_TAG, "notificationsEnabled=${state.notificationsEnabled}")
        Log.i(BUNBUN_PUSH_TAG, "permissionGranted=${state.permissionGranted}")
        Log.i(
            BUNBUN_PUSH_TAG,
            "channel id=${command.channelId} exists=${state.channelExists} importance=${state.channelImportance ?: "none"}",
        )
        val dropReason = when {
            !state.notificationsEnabled -> PushDropReason.NOTIFICATIONS_DISABLED
            !state.permissionGranted -> PushDropReason.PERMISSION_DENIED
            !state.channelExists -> PushDropReason.CHANNEL_MISSING
            state.channelImportance == NotificationManager.IMPORTANCE_NONE -> PushDropReason.CHANNEL_DISABLED
            else -> null
        }
        if (dropReason != null) {
            Log.i(BUNBUN_PUSH_TAG, "dropped reason=$dropReason")
            return
        }

        runCatching { BunbunNotifications.notify(this, command) }
            .onSuccess {
                Log.i(BUNBUN_PUSH_TAG, "notify called id=${command.notificationId}")
                container.synchronizeAfterPush(session.accountId, command.payload.chatId)
            }
            .onFailure {
                Log.e(BUNBUN_PUSH_TAG, "notify failed type=${it.javaClass.simpleName}")
                Log.i(BUNBUN_PUSH_TAG, "dropped reason=${PushDropReason.NOTIFY_FAILED}")
            }
    }
}
