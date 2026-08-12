package com.example.bunbun.data.repository

import com.example.bunbun.data.api.BunbunApi
import com.example.bunbun.data.api.NetworkModule
import com.example.bunbun.data.local.SessionManager
import com.example.bunbun.data.model.*
import kotlinx.serialization.json.JsonElement
import retrofit2.Response
import java.io.IOException

class ApiException(val code: String, message: String, val status: Int) : IOException(message)

class BunbunRepository(private val api: BunbunApi, private val sessions: SessionManager) {
    suspend fun restoreSession(): UserDto? {
        if (sessions.load() == null) return null
        return try { unwrap(api.me()).user } catch (error: ApiException) {
            if (error.status == 401) { sessions.clear(); null } else throw error
        }
    }

    suspend fun login(username: String, password: String): UserDto {
        val data = unwrap(api.login(LoginRequest(username.trim().lowercase(), password)))
        sessions.save(data.accessToken)
        return data.user
    }

    suspend fun register(username: String, displayName: String, password: String): UserDto {
        val data = unwrap(api.register(RegisterRequest(username.trim().lowercase(), displayName.trim(), password)))
        sessions.save(data.accessToken)
        return data.user
    }

    suspend fun logout() { try { if (sessions.peekToken() != null) unwrap(api.logout()) } finally { sessions.clear() } }
    suspend fun chats(): List<ChatDto> = unwrap(api.chats()).chats
    suspend fun searchUsers(query: String): List<UserDto> = unwrap(api.searchUsers(query.trim())).users
    suspend fun createDirect(userId: Long): ChatDto = unwrap(api.createDirect(CreateDirectRequest(userId))).chat
    suspend fun messages(chatId: Long, afterId: Long? = null): List<MessageDto> = unwrap(api.messages(chatId, afterId)).messages
    suspend fun sendMessage(chatId: Long, text: String): MessageDto = unwrap(api.sendMessage(SendMessageRequest(chatId, text))).message
    suspend fun markRead(chatId: Long, messageId: Long) { unwrap(api.markRead(MarkReadRequest(chatId, messageId))) }

    private fun <T> unwrap(response: Response<ApiEnvelope<T>>): T {
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
}

