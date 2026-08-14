package com.example.bunbun.data.api

import com.example.bunbun.data.model.*
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface BunbunApi {
    @POST("api/v1/auth/register.php") suspend fun register(@Body request: RegisterRequest): Response<ApiEnvelope<AuthData>>
    @POST("api/v1/auth/login.php") suspend fun login(@Body request: LoginRequest): Response<ApiEnvelope<AuthData>>
    @POST("api/v1/auth/logout.php") suspend fun logout(): Response<ApiEnvelope<LogoutData>>
    @GET("api/v1/auth/me.php") suspend fun me(): Response<ApiEnvelope<UserData>>
    @GET("api/v1/users/search.php") suspend fun searchUsers(@Query("q") query: String): Response<ApiEnvelope<UsersData>>
    @GET("api/v1/chats/list.php") suspend fun chats(): Response<ApiEnvelope<ChatsData>>
    @POST("api/v1/chats/create-direct.php") suspend fun createDirect(@Body request: CreateDirectRequest): Response<ApiEnvelope<ChatData>>
    @GET("api/v1/messages/list.php") suspend fun messages(@Query("chat_id") chatId: Long, @Query("after_id") afterId: Long? = null): Response<ApiEnvelope<MessagesData>>
    @POST("api/v1/messages/send.php") suspend fun sendMessage(@Body request: SendMessageRequest): Response<ApiEnvelope<MessageData>>
    @POST("api/v1/messages/mark-read.php") suspend fun markRead(@Body request: MarkReadRequest): Response<ApiEnvelope<MarkReadData>>
    @POST("api/v1/push/register.php") suspend fun registerPush(@Body request: PushTokenRequest): Response<ApiEnvelope<PushDeviceData>>
    @POST("api/v1/push/unregister.php") suspend fun unregisterPush(@Body request: PushTokenRequest): Response<ApiEnvelope<PushDeviceData>>
}
