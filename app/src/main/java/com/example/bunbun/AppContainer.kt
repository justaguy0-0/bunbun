package com.example.bunbun

import android.content.Context
import com.example.bunbun.data.api.NetworkModule
import com.example.bunbun.data.local.EncryptedTokenStore
import com.example.bunbun.data.local.SessionManager
import com.example.bunbun.data.repository.BunbunRepository
import com.example.bunbun.push.ApiPushRegistrationRemote
import com.example.bunbun.push.FirebasePushRegistrationTrigger
import com.example.bunbun.push.ForegroundChatTracker
import com.example.bunbun.push.NotificationPermissionPreferences
import com.example.bunbun.push.PendingChatNavigation
import com.example.bunbun.push.PushTokenPreferences
import com.example.bunbun.push.PushTokenSynchronizer

class AppContainer(context: Context) {
    private val sessions = SessionManager(EncryptedTokenStore(context.applicationContext))
    private val api = NetworkModule.create(sessions)
    val pushTokenSynchronizer = PushTokenSynchronizer(
        storage = PushTokenPreferences(context.applicationContext),
        sessions = sessions,
        remote = ApiPushRegistrationRemote(api),
        registrationTrigger = FirebasePushRegistrationTrigger(),
    )
    val foregroundChatTracker = ForegroundChatTracker()
    val pendingChatNavigation = PendingChatNavigation()
    val notificationPermissionPreferences = NotificationPermissionPreferences(context.applicationContext)
    val repository = BunbunRepository(api, sessions, pushTokenSynchronizer)
}
