package com.example.bunbun.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SessionManager(private val tokenStore: EncryptedTokenStore) {
    @Volatile private var token: String? = null
    fun peekToken(): String? = token
    suspend fun load(): String? = withContext(Dispatchers.IO) { tokenStore.read().also { token = it } }
    suspend fun save(value: String) = withContext(Dispatchers.IO) { tokenStore.write(value); token = value }
    suspend fun clear() = withContext(Dispatchers.IO) { tokenStore.clear(); token = null }
}

