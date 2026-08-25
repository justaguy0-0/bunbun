package com.example.bunbun

import android.content.Context
import android.util.Log
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
import com.example.bunbun.push.BUNBUN_PUSH_TAG
import com.example.bunbun.push.FirebasePushRegistrationTrigger
import com.example.bunbun.push.ForegroundChatTracker
import com.example.bunbun.push.NotificationPermissionPreferences
import com.example.bunbun.push.PendingChatNavigation
import com.example.bunbun.push.PushTokenPreferences
import com.example.bunbun.push.PushTokenSynchronizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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

    fun pushSessionSnapshot() = sessions.pushSessionSnapshot()

    fun handleFirebaseRegistration(installationId: String) {
        applicationScope.launch {
            runCatching { pushTokenSynchronizer.onRegistered(installationId) }
                .onSuccess { Log.i(BUNBUN_PUSH_TAG, "onRegistered handled") }
                .onFailure {
                    Log.w(BUNBUN_PUSH_TAG, "onRegistered failed type=${it.javaClass.simpleName}")
                }
        }
    }

    fun synchronizeAfterPush(accountId: Long?, chatId: Long) {
        if (accountId == null) {
            Log.i(BUNBUN_PUSH_TAG, "post-notification sync skipped reason=ACCOUNT_ID_UNAVAILABLE")
            return
        }
        applicationScope.launch {
            runCatching {
                if (repository.authenticatedAccountId() != accountId) return@launch
                repository.syncChats(accountId)
                repository.syncMessages(accountId, chatId)
            }.onFailure {
                Log.w(BUNBUN_PUSH_TAG, "post-notification sync failed type=${it.javaClass.simpleName}")
            }
        }
    }
}
