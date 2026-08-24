package com.example.bunbun

import com.example.bunbun.data.api.BunbunApi
import com.example.bunbun.data.api.NetworkModule
import com.example.bunbun.data.model.ApiEnvelope
import com.example.bunbun.data.model.UpdateProfileRequest
import com.example.bunbun.data.model.UserData
import com.example.bunbun.data.model.UserDto
import com.example.bunbun.data.repository.ApiException
import com.example.bunbun.data.repository.normalizeDisplayName
import com.example.bunbun.data.repository.unwrapApiResponse
import com.example.bunbun.data.repository.validateUpdatedProfile
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
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class ProfileUpdateNetworkContractTest {
    @Test fun displayNameUpdatePostsOnlyDisplayNameAndKeepsUsername() = runBlocking {
        lateinit var captured: Request
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            captured = chain.request()
            Response.Builder()
                .request(captured)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(
                    """{"ok":true,"data":{"user":{"id":1,"username":"ivan_login","display_name":"Иван П.","created_at":"2026-08-24 00:00:00"}}}"""
                        .toResponseBody("application/json".toMediaType()),
                )
                .build()
        }.build()
        val api = Retrofit.Builder()
            .baseUrl(BuildConfig.BUNBUN_API_BASE_URL)
            .client(client)
            .addConverterFactory(NetworkModule.json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BunbunApi::class.java)

        val response = api.updateProfile(UpdateProfileRequest("Иван П."))
        val body = Buffer().use { buffer -> captured.body!!.writeTo(buffer); buffer.readUtf8() }

        assertTrue(captured.url.encodedPath.endsWith("/api/v1/profile/update.php"))
        assertEquals("POST", captured.method)
        assertEquals("""{"display_name":"Иван П."}""", body)
        assertFalse(body.contains("username"))
        assertFalse(body.contains("user_id"))
        assertEquals("ivan_login", response.body()!!.data!!.user.username)
    }

    @Test fun blankNameAndInvalidResponsesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { normalizeDisplayName("   ") }
        val current = user(displayName = "Иван")
        assertThrows(ApiException::class.java) {
            validateUpdatedProfile(current, current.copy(username = "changed", displayName = "Иван П."))
        }
        assertThrows(ApiException::class.java) {
            unwrapApiResponse(retrofit2.Response.success(ApiEnvelope<UserData>(ok = true, data = null)))
        }
    }

    private fun user(displayName: String) = UserDto(1, "ivan_login", displayName, "2026-08-24 00:00:00")
}
