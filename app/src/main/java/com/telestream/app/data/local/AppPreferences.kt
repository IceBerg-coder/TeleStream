package com.telestream.app.data.local

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("telestream_prefs", Context.MODE_PRIVATE)

    var botToken: String
        get() = prefs.getString(KEY_BOT_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BOT_TOKEN, value.trim()).apply()

    var apiId: String
        get() = prefs.getString(KEY_API_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_ID, value.trim()).apply()

    var apiHash: String
        get() = prefs.getString(KEY_API_HASH, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_HASH, value.trim()).apply()

    var channelId: String
        get() = prefs.getString(KEY_CHANNEL_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CHANNEL_ID, value.trim()).apply()

    var isUserLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var userPhoneNumber: String
        get() = prefs.getString(KEY_USER_PHONE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_PHONE, value).apply()

    var userDisplayName: String
        get() = prefs.getString(KEY_USER_NAME, "Telegram User") ?: "Telegram User"
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userSessionToken: String
        get() = prefs.getString(KEY_SESSION_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SESSION_TOKEN, value).apply()

    var streamServerPort: Int
        get() = prefs.getInt(KEY_PORT, 8765)
        set(value) = prefs.edit().putInt(KEY_PORT, value).apply()

    val isConfigured: Boolean
        get() = isUserLoggedIn || botToken.isNotEmpty() || (apiId.isNotEmpty() && apiHash.isNotEmpty())

    fun logout() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .putString(KEY_USER_PHONE, "")
            .putString(KEY_USER_NAME, "")
            .putString(KEY_SESSION_TOKEN, "")
            .apply()
    }

    companion object {
        private const val KEY_BOT_TOKEN = "bot_token"
        private const val KEY_API_ID = "api_id"
        private const val KEY_API_HASH = "api_hash"
        private const val KEY_CHANNEL_ID = "channel_id"
        private const val KEY_PORT = "stream_server_port"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_SESSION_TOKEN = "session_token"
    }
}
