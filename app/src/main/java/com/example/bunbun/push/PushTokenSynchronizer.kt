package com.example.bunbun.push

import android.content.Context
import android.util.Log
import com.example.bunbun.BuildConfig
import com.example.bunbun.data.api.BunbunApi
import com.example.bunbun.data.model.PushTokenRequest
import com.example.bunbun.data.repository.unwrapApiResponse
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.withTimeoutOrNull

interface PushTokenStorage {
    fun token(): String?
    fun savePending(token: String)
    fun markSynchronized()
    fun markPending()
    fun isSynchronized(): Boolean
}

fun interface PushSessionSource {
    suspend fun currentOrLoad(): String?
}

class PushTokenPreferences(context: Context) : PushTokenStorage {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun token(): String? = preferences.getString(KEY_TOKEN, null)

    override fun savePending(token: String) {
        preferences.edit().putString(KEY_TOKEN, token).putBoolean(KEY_SYNCHRONIZED, false).apply()
    }

    override fun markSynchronized() {
        preferences.edit().putBoolean(KEY_SYNCHRONIZED, true).apply()
    }

    override fun markPending() {
        preferences.edit().putBoolean(KEY_SYNCHRONIZED, false).apply()
    }

    override fun isSynchronized(): Boolean = preferences.getBoolean(KEY_SYNCHRONIZED, false)

    private companion object {
        const val PREFERENCES_NAME = "bunbun_push"
        const val KEY_TOKEN = "fcm_token"
        const val KEY_SYNCHRONIZED = "fcm_token_synchronized"
    }
}

fun interface PushRegistrationTrigger {
    suspend fun ensureRegistered()
}

class FirebasePushRegistrationTrigger : PushRegistrationTrigger {
    override suspend fun ensureRegistered(): Unit = suspendCoroutine { continuation ->
        FirebaseMessaging.getInstance().register().addOnCompleteListener {
            continuation.resume(Unit)
        }
    }
}

interface PushRegistrationRemote {
    suspend fun register(token: String)
    suspend fun unregister(token: String)
}

class ApiPushRegistrationRemote(private val api: BunbunApi) : PushRegistrationRemote {
    override suspend fun register(token: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "push registration request fields=[token, platform]")
        }
        unwrapApiResponse(api.registerPush(PushTokenRequest(token = token, platform = PLATFORM_ANDROID)))
    }

    override suspend fun unregister(token: String) {
        unwrapApiResponse(api.unregisterPush(PushTokenRequest(token = token, platform = PLATFORM_ANDROID)))
    }

    private companion object {
        const val TAG = "BunbunPush"
        const val PLATFORM_ANDROID = "android"
    }
}

class PushTokenSynchronizer(
    private val storage: PushTokenStorage,
    private val sessions: PushSessionSource,
    private val remote: PushRegistrationRemote,
    private val registrationTrigger: PushRegistrationTrigger,
) {
    suspend fun onRegistered(registrationId: String) {
        if (registrationId.isBlank()) return
        storage.savePending(registrationId)
        if (sessions.currentOrLoad() != null) synchronize(registrationId)
    }

    suspend fun afterAuthentication() {
        withTimeoutOrNull(SYNC_TIMEOUT_MS) {
            val registrationId = storage.token()
            if (registrationId == null) {
                registrationTrigger.ensureRegistered()
            } else if (!storage.isSynchronized()) {
                synchronize(registrationId)
            }
        }
    }

    suspend fun unregisterBeforeLogout() {
        val token = storage.token() ?: return
        withTimeoutOrNull(SYNC_TIMEOUT_MS) { remote.unregister(token) }
        storage.markPending()
    }

    fun markSignedOut() = storage.markPending()

    private suspend fun synchronize(token: String) {
        remote.register(token)
        storage.markSynchronized()
    }

    private companion object { const val SYNC_TIMEOUT_MS = 3_000L }
}
