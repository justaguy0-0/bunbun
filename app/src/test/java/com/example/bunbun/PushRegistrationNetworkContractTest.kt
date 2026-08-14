package com.example.bunbun

import com.example.bunbun.data.api.BunbunApi
import com.example.bunbun.data.api.NetworkModule
import com.example.bunbun.data.model.PushTokenRequest
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class PushRegistrationNetworkContractTest {
    @Test
    fun pushRegistrationPostsExactJsonIncludingAndroidPlatform() = runBlocking {
        lateinit var capturedRequest: Request
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                capturedRequest = chain.request()
                Response.Builder()
                    .request(capturedRequest)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("Test response")
                    .body(
                        """{"ok":true,"data":{"registered":true}}"""
                            .toResponseBody("application/json".toMediaType()),
                    )
                    .build()
            }
            .build()
        val api = Retrofit.Builder()
            .baseUrl(BuildConfig.BUNBUN_API_BASE_URL)
            .client(client)
            .addConverterFactory(NetworkModule.json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BunbunApi::class.java)

        api.registerPush(PushTokenRequest(token = REGISTRATION_ID, platform = "android"))

        val body = Buffer().use { buffer ->
            capturedRequest.body!!.writeTo(buffer)
            buffer.readUtf8()
        }
        assertEquals("POST", capturedRequest.method)
        assertEquals(
            "https://picnic-bk.ru/bunbun-api/api/v1/push/register.php",
            capturedRequest.url.toString(),
        )
        assertEquals("""{"token":"$REGISTRATION_ID","platform":"android"}""", body)
        assertTrue(body.contains("\"platform\":\"android\""))
        assertFalse(body.contains("user_id"))
    }

    private companion object {
        const val REGISTRATION_ID = "test-registration-id-1234567890"
    }
}
