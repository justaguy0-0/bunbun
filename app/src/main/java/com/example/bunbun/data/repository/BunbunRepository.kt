package com.example.bunbun.data.repository

import com.example.bunbun.data.api.BunbunApi
import com.example.bunbun.data.api.NetworkModule
import com.example.bunbun.data.local.SessionManager
import com.example.bunbun.data.model.*
import com.example.bunbun.push.PushTokenSynchronizer
import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import java.io.IOException

class ApiException(val code: String, message: String, val status: Int) : IOException(message)

class BunbunRepository(
    private val api: BunbunApi,
    private val sessions: SessionManager,
    private val pushTokens: PushTokenSynchronizer? = null,
) {
    suspend fun restoreSession(): UserDto? {
        if (sessions.load() == null) return null
        return try {
            unwrapApiResponse(api.me()).user.also { runCatching { pushTokens?.afterAuthentication() } }
        } catch (error: ApiException) {
            if (error.status == 401) { sessions.clear(); null } else throw error
        }
    }

    suspend fun login(username: String, password: String): UserDto {
        val data = unwrapApiResponse(api.login(LoginRequest(username.trim().lowercase(), password)))
        sessions.save(data.accessToken)
        runCatching { pushTokens?.afterAuthentication() }
        return data.user
    }

    suspend fun register(username: String, displayName: String, password: String): UserDto {
        val data = unwrapApiResponse(api.register(RegisterRequest(username.trim().lowercase(), displayName.trim(), password)))
        sessions.save(data.accessToken)
        runCatching { pushTokens?.afterAuthentication() }
        return data.user
    }

    suspend fun logout() {
        try {
            runCatching { pushTokens?.unregisterBeforeLogout() }
            if (sessions.peekToken() != null) unwrapApiResponse(api.logout())
        } finally {
            sessions.clear()
            pushTokens?.markSignedOut()
        }
    }
    suspend fun chats(): List<ChatDto> = unwrapApiResponse(api.chats()).chats
    suspend fun searchUsers(query: String): List<UserDto> = unwrapApiResponse(api.searchUsers(query.trim())).users
    suspend fun createDirect(userId: Long): ChatDto = unwrapApiResponse(api.createDirect(CreateDirectRequest(userId))).chat
    suspend fun messages(chatId: Long, afterId: Long? = null): List<MessageDto> = unwrapApiResponse(api.messages(chatId, afterId)).messages
    suspend fun sendMessage(chatId: Long, text: String): MessageDto = unwrapApiResponse(api.sendMessage(SendMessageRequest(chatId, text))).message
    suspend fun markRead(chatId: Long, messageId: Long) { unwrapApiResponse(api.markRead(MarkReadRequest(chatId, messageId))) }
}

fun <T> unwrapApiResponse(response: Response<ApiEnvelope<T>>): T {
        val body = response.body()
        if (response.isSuccessful && body?.ok == true && body.data != null) return body.data
        val parsedError = response.errorBody()?.string()?.let { raw ->
            runCatching { NetworkModule.json.decodeFromString<ApiEnvelope<JsonElement>>(raw).error }.getOrNull()
        } ?: body?.error
        throw ApiException(
            parsedError?.code ?: "HTTP_${response.code()}",
            parsedError?.message ?: "Request failed (${response.code()})",
            response.code(),
        )
}
