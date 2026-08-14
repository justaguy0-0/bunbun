package com.example.bunbun.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.bunbun.push.PushSessionSource

class SessionManager(private val tokenStore: EncryptedTokenStore) : PushSessionSource {
    @Volatile private var token: String? = null
    fun peekToken(): String? = token
    suspend fun load(): String? = withContext(Dispatchers.IO) { tokenStore.read().also { token = it } }
    override suspend fun currentOrLoad(): String? = peekToken() ?: load()
    suspend fun save(value: String) = withContext(Dispatchers.IO) { tokenStore.write(value); token = value }
    suspend fun clear() = withContext(Dispatchers.IO) { tokenStore.clear(); token = null }
}
