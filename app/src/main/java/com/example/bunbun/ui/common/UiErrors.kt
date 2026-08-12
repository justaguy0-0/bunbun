package com.example.bunbun.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.bunbun.R
import com.example.bunbun.data.repository.ApiException
import java.util.Locale

fun Throwable.asUiError(fallbackCode: String): String = when (this) {
    is ApiException -> "$code|${message.orEmpty()}"
    else -> message ?: fallbackCode
}

@Composable
fun localizedErrorMessage(raw: String): String {
    val parts = raw.split('|', limit = 2)
    val code = parts.first().uppercase(Locale.ROOT)
    val detail = parts.getOrElse(1) { raw }
    val normalized = "$code ${detail.uppercase(Locale.ROOT)}"

    if (detail.any { it in 'А'..'я' || it == 'Ё' || it == 'ё' }) return detail

    @StringRes val resource = when {
        "INVALID_CREDENTIALS" in normalized || "INVALID CREDENTIAL" in normalized -> R.string.error_invalid_credentials
        "USERNAME_TAKEN" in normalized || "ALREADY TAKEN" in normalized || "ALREADY EXISTS" in normalized -> R.string.error_username_taken
        "LOCAL_USERNAME_INVALID" in normalized || ("USERNAME" in normalized && "3-32" in normalized) -> R.string.error_username_format
        "LOCAL_DISPLAY_NAME_INVALID" in normalized || "DISPLAY NAME MUST" in normalized -> R.string.error_display_name_format
        "LOCAL_PASSWORD_INVALID" in normalized || ("PASSWORD" in normalized && "8-72" in normalized) -> R.string.error_password_format
        "VALIDATION_ERROR" in normalized || "FIELD " in normalized && " REQUIRED" in normalized -> R.string.error_validation
        "TIMEOUT" in normalized || "TIMED OUT" in normalized || "TOO LONG" in normalized -> R.string.error_timeout
        "UNKNOWNHOST" in normalized || "UNABLE TO RESOLVE HOST" in normalized ||
            "FAILED TO CONNECT" in normalized || "NETWORK" in normalized || "CONNECTION" in normalized -> R.string.error_network
        "INVALID_TOKEN" in normalized || "UNAUTHORIZED" in normalized || "HTTP_401" in normalized -> R.string.error_session_expired
        else -> R.string.error_unknown
    }
    return stringResource(resource)
}
