package com.example.bunbun.data.local

import android.content.Context
import com.example.bunbun.data.model.UserDto

class SessionMetadataStore(context: Context) {
    private val preferences = context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun readUser(): UserDto? {
        val id = preferences.getLong(KEY_ID, -1L).takeIf { it > 0 } ?: return null
        val username = preferences.getString(KEY_USERNAME, null)?.takeIf { it.isNotBlank() } ?: return null
        val displayName = preferences.getString(KEY_DISPLAY_NAME, null)?.takeIf { it.isNotBlank() } ?: return null
        return UserDto(id, username, displayName, preferences.getString(KEY_CREATED_AT, "").orEmpty())
    }

    fun writeUser(user: UserDto) {
        preferences.edit()
            .putLong(KEY_ID, user.id)
            .putString(KEY_USERNAME, user.username)
            .putString(KEY_DISPLAY_NAME, user.displayName)
            .putString(KEY_CREATED_AT, user.createdAt)
            .commit()
    }

    fun clear() = preferences.edit().clear().commit()

    private companion object {
        const val NAME = "bunbun_session_metadata"
        const val KEY_ID = "active_user_id"
        const val KEY_USERNAME = "active_username"
        const val KEY_DISPLAY_NAME = "active_display_name"
        const val KEY_CREATED_AT = "active_created_at"
    }
}
