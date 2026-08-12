package com.example.bunbun.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiEnvelope<T>(val ok: Boolean, val data: T? = null, val error: ApiErrorDto? = null)

@Serializable data class ApiErrorDto(val code: String, val message: String)

@Serializable
data class UserDto(
    val id: Long,
    val username: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class MessageDto(
    val id: Long,
    @SerialName("chat_id") val chatId: Long,
    @SerialName("sender_id") val senderId: Long,
    val text: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class ChatDto(
    val id: Long,
    val type: String,
    val peer: UserDto,
    @SerialName("last_message") val lastMessage: MessageDto? = null,
    @SerialName("unread_count") val unreadCount: Int = 0,
)

@Serializable data class LoginRequest(val username: String, val password: String)
@Serializable data class RegisterRequest(val username: String, @SerialName("display_name") val displayName: String, val password: String)
@Serializable data class CreateDirectRequest(@SerialName("user_id") val userId: Long)
@Serializable data class SendMessageRequest(@SerialName("chat_id") val chatId: Long, val text: String)
@Serializable data class MarkReadRequest(@SerialName("chat_id") val chatId: Long, @SerialName("message_id") val messageId: Long? = null)

@Serializable
data class AuthData(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_at") val expiresAt: String,
    val user: UserDto,
)

@Serializable data class UserData(val user: UserDto)
@Serializable data class UsersData(val users: List<UserDto>)
@Serializable data class ChatsData(val chats: List<ChatDto>)
@Serializable data class ChatData(val chat: ChatDto)
@Serializable data class MessagesData(val messages: List<MessageDto>)
@Serializable data class MessageData(val message: MessageDto)
@Serializable data class LogoutData(@SerialName("logged_out") val loggedOut: Boolean)
@Serializable data class MarkReadData(@SerialName("last_read_message_id") val lastReadMessageId: Long)

