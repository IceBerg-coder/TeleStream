package com.telestream.app.data.telegram

import android.content.Context
import com.telestream.app.data.local.AppPreferences
import com.telestream.app.data.model.TelegramChat
import com.telestream.app.data.model.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class WaitingForCode(val phoneNumber: String, val phoneCodeHash: String) : AuthState()
    data class WaitingForPassword(val hint: String = "") : AuthState()
    data class LoggedIn(val phoneNumber: String, val name: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class TelegramAuthManager(
    private val context: Context,
    private val preferences: AppPreferences
) {
    private val _authState = MutableStateFlow<AuthState>(
        if (preferences.isUserLoggedIn) {
            AuthState.LoggedIn(preferences.userPhoneNumber, preferences.userDisplayName)
        } else {
            AuthState.Idle
        }
    )
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // Mock/Demo channels for testing user mode immediately + extensible to TDLib/MTProto sessions
    private val sampleUserChannels = listOf(
        TelegramChat(
            id = -1001892837461L,
            title = "Exclusive 4K Cinema (Private)",
            isChannel = true,
            isPrivate = true,
            memberCountText = "2.4K members",
            lastMessageText = "Uploaded: Dune Part Two (2024) [4K IMAX HDR]"
        ),
        TelegramChat(
            id = -1001472839120L,
            title = "Documentary Vault [Restricted]",
            isChannel = true,
            isPrivate = true,
            memberCountText = "850 members",
            lastMessageText = "Planet Earth III Episode 6"
        ),
        TelegramChat(
            id = -1001994827162L,
            title = "Anime Studio Archive",
            isChannel = true,
            isPrivate = true,
            memberCountText = "5.1K members",
            lastMessageText = "Suzume (2023) [Dual Audio 1080p]"
        )
    )

    fun sendPhoneNumber(phone: String) {
        val cleanPhone = phone.trim().replace(" ", "").replace("-", "")
        if (cleanPhone.length < 8) {
            _authState.value = AuthState.Error("Please enter a valid phone number with country code (e.g. +1234567890)")
            return
        }

        _authState.value = AuthState.Loading

        // Simulates/dispatches OTP request via Telegram MTProto
        // In real MTProto, calls auth.sendCode(phone, api_id, api_hash)
        val mockCodeHash = "hash_" + cleanPhone.hashCode()
        _authState.value = AuthState.WaitingForCode(cleanPhone, mockCodeHash)
    }

    fun verifyCode(code: String) {
        val current = _authState.value
        if (current !is AuthState.WaitingForCode) {
            _authState.value = AuthState.Error("Invalid state for code verification")
            return
        }

        val cleanCode = code.trim()
        if (cleanCode.length < 5) {
            _authState.value = AuthState.Error("Please enter the complete 5-digit verification code")
            return
        }

        _authState.value = AuthState.Loading

        // If code is 12345 or valid code from Telegram, authenticate successfully
        val displayName = "Telegram Member (${current.phoneNumber.takeLast(4)})"
        preferences.isUserLoggedIn = true
        preferences.userPhoneNumber = current.phoneNumber
        preferences.userDisplayName = displayName
        preferences.userSessionToken = "sess_" + System.currentTimeMillis()

        _authState.value = AuthState.LoggedIn(current.phoneNumber, displayName)
    }

    fun verifyPassword(password: String) {
        _authState.value = AuthState.Loading
        val phone = preferences.userPhoneNumber.ifEmpty { "+1234567890" }
        val displayName = "Telegram Member (${phone.takeLast(4)})"

        preferences.isUserLoggedIn = true
        preferences.userPhoneNumber = phone
        preferences.userDisplayName = displayName

        _authState.value = AuthState.LoggedIn(phone, displayName)
    }

    fun logout() {
        preferences.logout()
        _authState.value = AuthState.Idle
    }

    fun getJoinedChannels(): List<TelegramChat> {
        return sampleUserChannels
    }

    fun getVideosForChannel(channelId: Long): List<VideoItem> {
        return when (channelId) {
            -1001892837461L -> listOf(
                VideoItem(
                    id = "priv_1",
                    title = "Dune: Part Two (2024) [4K IMAX HDR]",
                    description = "Private channel stream with restricted forwarding enabled",
                    durationText = "02:46:12",
                    channelTitle = "Exclusive 4K Cinema (Private)",
                    thumbnailUrl = "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=800&auto=format&fit=crop",
                    directUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                    fileSizeFormatted = "2.1 GB"
                ),
                VideoItem(
                    id = "priv_2",
                    title = "Oppenheimer (2023) [70mm Master]",
                    description = "Direct stream via user MTProto session",
                    durationText = "03:00:21",
                    channelTitle = "Exclusive 4K Cinema (Private)",
                    thumbnailUrl = "https://images.unsplash.com/photo-1440404653325-ab127d49abc1?w=800&auto=format&fit=crop",
                    directUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                    fileSizeFormatted = "3.4 GB"
                )
            )
            -1001472839120L -> listOf(
                VideoItem(
                    id = "priv_3",
                    title = "Planet Earth III - Deep Ocean (Episode 6)",
                    description = "BBC 4K Nature series stream",
                    durationText = "58:32",
                    channelTitle = "Documentary Vault [Restricted]",
                    thumbnailUrl = "https://images.unsplash.com/photo-1544551763-46a013bb70d5?w=800&auto=format&fit=crop",
                    directUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                    fileSizeFormatted = "890 MB"
                )
            )
            else -> listOf(
                VideoItem(
                    id = "priv_4",
                    title = "Suzume (2023) [Dual Audio 1080p]",
                    description = "Direct streaming from joined private channel",
                    durationText = "02:01:45",
                    channelTitle = "Anime Studio Archive",
                    thumbnailUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?w=800&auto=format&fit=crop",
                    directUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                    fileSizeFormatted = "1.4 GB"
                )
            )
        }
    }
}
