package com.example.bunbun.data.api

import android.util.Log
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.Locale

/**
 * Debug-only request metadata logger. It deliberately never reads request bodies or headers.
 */
internal class SafeNetworkLoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        Log.d(TAG, "--> ${request.method} ${request.url}")

        return try {
            val response = chain.proceed(request)
            logResponseChain(response)
            if (!response.isSuccessful) {
                val errorBody = response.peekBody(MAX_ERROR_BODY_BYTES).string()
                Log.d(TAG, "<-- error body: ${redactSensitiveData(errorBody)}")
            }
            response
        } catch (error: IOException) {
            Log.d(TAG, "<-- FAILED ${request.method} ${request.url}: ${error.javaClass.simpleName}")
            throw error
        }
    }

    private fun logResponseChain(response: Response) {
        val responses = generateSequence(response as Response?) { it.priorResponse }
            .toList()
            .asReversed()

        responses.forEachIndexed { index, hop ->
            val redirectTarget = if (hop.isRedirect) {
                hop.header("Location")?.let { location -> hop.request.url.resolve(location) }
            } else {
                null
            }
            val redirectSuffix = redirectTarget?.let { " redirect=$it" }.orEmpty()
            Log.d(
                TAG,
                "<-- hop=${index + 1}/${responses.size} ${hop.request.method} " +
                    "${hop.request.url} status=${hop.code}$redirectSuffix",
            )
        }
    }

    private fun redactSensitiveData(raw: String): String {
        val json = runCatching { NetworkModule.json.parseToJsonElement(raw) }.getOrNull()
        if (json != null) return redactJson(json).toString()

        return SENSITIVE_TEXT_FIELD.replace(raw) { match ->
            "${match.groupValues[1]}\"<redacted>\""
        }
    }

    private fun redactJson(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> JsonObject(element.mapValues { (key, value) ->
            if (key.isSensitiveField()) JsonPrimitive("<redacted>") else redactJson(value)
        })
        is JsonArray -> JsonArray(element.map(::redactJson))
        else -> element
    }

    private fun String.isSensitiveField(): Boolean {
        val normalized = replace(CAMEL_CASE_BOUNDARY, "$1_$2")
            .lowercase(Locale.ROOT)
            .replace('-', '_')
        return normalized in SENSITIVE_FIELDS || normalized.endsWith("_password") || normalized.endsWith("_token")
    }

    private companion object {
        const val TAG = "BunbunHttp"
        const val MAX_ERROR_BODY_BYTES = 64L * 1024L
        val SENSITIVE_FIELDS = setOf(
            "password",
            "access_token",
            "refresh_token",
            "authorization",
            "credential",
            "credentials",
        )
        val SENSITIVE_TEXT_FIELD = Regex(
            """(?i)([\"']?(?:password|access[-_]?token|refresh[-_]?token|authorization|credentials?)[\"']?\s*[:=]\s*)([\"'][^\"']*[\"']|[^\s,}&]+)""",
        )
        val CAMEL_CASE_BOUNDARY = Regex("([a-z0-9])([A-Z])")
    }
}
