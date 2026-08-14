package com.example.bunbun.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.bunbun.push.PushSessionSource
import com.example.bunbun.data.model.UserDto

class SessionManager(
    private val tokenStore: EncryptedTokenStore,
    private val metadataStore: SessionMetadataStore,
) : PushSessionSource {
    @Volatile private var token: String? = null
    fun peekToken(): String? = token
    suspend fun load(): String? = withContext(Dispatchers.IO) { tokenStore.read().also { token = it } }
    override suspend fun currentOrLoad(): String? = peekToken() ?: load()
    suspend fun save(value: String) = withContext(Dispatchers.IO) { tokenStore.write(value); token = value }
    suspend fun setActiveUser(user: UserDto) = withContext(Dispatchers.IO) { metadataStore.writeUser(user) }
    suspend fun activeUser(): UserDto? = withContext(Dispatchers.IO) { metadataStore.readUser() }
    suspend fun activeUserId(): Long? = activeUser()?.id
    suspend fun clear() = withContext(Dispatchers.IO) {
        tokenStore.clear()
        metadataStore.clear()
        token = null
    }
}
