package com.example.bunbun

import android.content.Context
import com.example.bunbun.data.api.NetworkModule
import com.example.bunbun.data.local.EncryptedTokenStore
import com.example.bunbun.data.local.BunbunDatabase
import com.example.bunbun.data.local.LocalDataStore
import com.example.bunbun.data.local.SessionManager
import com.example.bunbun.data.local.SessionMetadataStore
import com.example.bunbun.data.repository.BunbunRepository
import com.example.bunbun.outbox.OutboxScheduler
import com.example.bunbun.presence.PresenceSynchronizer
import com.example.bunbun.push.ApiPushRegistrationRemote
import com.example.bunbun.push.FirebasePushRegistrationTrigger
import com.example.bunbun.push.ForegroundChatTracker
import com.example.bunbun.push.NotificationPermissionPreferences
import com.example.bunbun.push.PendingChatNavigation
import com.example.bunbun.push.PushTokenPreferences
import com.example.bunbun.push.PushTokenSynchronizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val sessions = SessionManager(
        EncryptedTokenStore(appContext),
        SessionMetadataStore(appContext),
    )
    private val api = NetworkModule.create(sessions)
    val pushTokenSynchronizer = PushTokenSynchronizer(
        storage = PushTokenPreferences(appContext),
        sessions = sessions,
        remote = ApiPushRegistrationRemote(api),
        registrationTrigger = FirebasePushRegistrationTrigger(),
    )
    val foregroundChatTracker = ForegroundChatTracker()
    val pendingChatNavigation = PendingChatNavigation()
    val notificationPermissionPreferences = NotificationPermissionPreferences(appContext)
    private val database = BunbunDatabase.create(appContext)
    private val localDataStore = LocalDataStore(database)
    private val outboxScheduler = OutboxScheduler(appContext)
    val repository = BunbunRepository(api, sessions, localDataStore, outboxScheduler, pushTokenSynchronizer)
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val presenceSynchronizer = PresenceSynchronizer(applicationScope, repository::touchPresence)
    val logoutCoordinator = LogoutCoordinator(
        presenceSynchronizer,
        repository,
        foregroundChatTracker,
        pendingChatNavigation,
        clearNotifications = { com.example.bunbun.push.BunbunNotifications.clearAll(appContext) },
    )
}
