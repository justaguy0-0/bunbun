package com.example.bunbun.data.api

import com.example.bunbun.BuildConfig
import com.example.bunbun.data.local.SessionManager
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    val json = Json { ignoreUnknownKeys = true }

    fun create(sessionManager: SessionManager): BunbunApi {
        require(BuildConfig.BUNBUN_API_BASE_URL.startsWith("https://")) { "Bunbun API base URL must use HTTPS" }
        require(BuildConfig.BUNBUN_API_BASE_URL.endsWith('/')) { "Bunbun API base URL must end with /" }
        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder().apply {
                    sessionManager.peekToken()?.let { header("Authorization", "Bearer $it") }
                }.build()
                chain.proceed(request)
            }
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            clientBuilder.addInterceptor(SafeNetworkLoggingInterceptor())
        }

        val client = clientBuilder.build()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BUNBUN_API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BunbunApi::class.java)
    }
}
