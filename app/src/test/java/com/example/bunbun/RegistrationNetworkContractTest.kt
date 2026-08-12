package com.example.bunbun

import com.example.bunbun.data.api.BunbunApi
import com.example.bunbun.data.api.NetworkModule
import com.example.bunbun.data.model.RegisterRequest
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class RegistrationNetworkContractTest {
    @Test
    fun registrationUsesPostAndExactProductionUrl() = runBlocking {
        lateinit var capturedRequest: Request
        val capturingClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                capturedRequest = chain.request()
                Response.Builder()
                    .request(capturedRequest)
                    .protocol(Protocol.HTTP_1_1)
                    .code(400)
                    .message("Test response")
                    .body("{}".toResponseBody("application/json".toMediaType()))
                    .build()
            }
            .build()
        val api = Retrofit.Builder()
            .baseUrl(BuildConfig.BUNBUN_API_BASE_URL)
            .client(capturingClient)
            .addConverterFactory(NetworkModule.json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BunbunApi::class.java)

        api.register(RegisterRequest("network_test", "Network Test", "not-logged"))

        assertEquals("POST", capturedRequest.method)
        assertEquals(
            "https://picnic-bk.ru/bunbun-api/api/v1/auth/register.php",
            capturedRequest.url.toString(),
        )
        assertNull(capturedRequest.url.query)
    }
}
