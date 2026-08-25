package com.example.bunbun

import com.example.bunbun.data.api.BunbunApi
import com.example.bunbun.data.api.NetworkModule
import com.example.bunbun.data.model.ApiEnvelope
import com.example.bunbun.data.model.ChatsData
import com.example.bunbun.data.model.MessagesData
import com.example.bunbun.data.model.SendMessageRequest
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class OfflineApiContractTest {
    @Test fun sendAlwaysIncludesStableClientMessageId() = runBlocking {
        lateinit var captured: Request
        val clientId = "123e4567-e89b-12d3-a456-426614174000"
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            captured = chain.request()
            Response.Builder()
                .request(captured)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(
                    """{"ok":true,"data":{"message":{"id":9,"chat_id":2,"sender_id":1,"text":"A","created_at":"2026-08-14T10:00:00Z","client_message_id":"$clientId"}}}"""
                        .toResponseBody("application/json".toMediaType()),
                )
                .build()
        }.build()
        val api = Retrofit.Builder()
            .baseUrl("https://picnic-bk.ru/bunbun-api/")
            .client(client)
            .addConverterFactory(NetworkModule.json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BunbunApi::class.java)

        val response = api.sendMessage(SendMessageRequest(2, "A", clientId))
        val body = Buffer().use { buffer -> captured.body!!.writeTo(buffer); buffer.readUtf8() }

        assertTrue(body.contains("\"client_message_id\":\"$clientId\""))
        assertEquals(clientId, response.body()!!.data!!.message.clientMessageId)
    }

    @Test fun emptyPollStillCarriesPeerReadCursor() {
        val decoded = NetworkModule.json.decodeFromString<ApiEnvelope<MessagesData>>(
            """{"ok":true,"data":{"messages":[],"peer_last_read_message_id":123}}""",
        )
        assertTrue(decoded.data!!.messages.isEmpty())
        assertEquals(123L, decoded.data!!.peerLastReadMessageId)
    }

    @Test fun chatListCarriesCurrentUsersReadCursor() {
        val decoded = NetworkModule.json.decodeFromString<ApiEnvelope<ChatsData>>(
            """{"ok":true,"data":{"chats":[{"id":2,"type":"direct","peer":{"id":9,"username":"peer","display_name":"Peer","created_at":"2026-08-14T10:00:00Z"},"unread_count":2,"my_last_read_message_id":123}]}}""",
        )

        assertEquals(123L, decoded.data!!.chats.single().myLastReadMessageId)
    }
}
