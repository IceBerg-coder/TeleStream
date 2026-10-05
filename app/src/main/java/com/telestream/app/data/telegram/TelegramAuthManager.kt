package com.telestream.app.data.telegram

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.telestream.app.data.local.AppPreferences
import com.telestream.app.data.model.TelegramChat
import com.telestream.app.data.model.VideoItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File

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
) : Client.ResultHandler {

    private val tag = "TelegramAuthManager"
    private var client: Client? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val joinedChannels = mutableListOf<TelegramChat>()

    init {
        try {
            System.loadLibrary("tdjni")
            Client.execute(TdApi.SetLogVerbosityLevel(1))
            initClient()
            Log.i(tag, "TDLib native client initialized successfully")
        } catch (e: Throwable) {
            Log.e(tag, "Failed to initialize TDLib native engine: ${e.message}", e)
        }
    }

    private fun initClient() {
        client = Client.create({ update ->
            handleUpdate(update)
        }, null, null)
    }

    private fun handleUpdate(obj: TdApi.Object?) {
        when (obj) {
            is TdApi.UpdateAuthorizationState -> {
                onAuthorizationStateUpdated(obj.authorizationState)
            }
            is TdApi.UpdateNewChat -> {
                addChatIfChannel(obj.chat)
            }
        }
    }

    private fun onAuthorizationStateUpdated(state: TdApi.AuthorizationState) {
        when (state) {
            is TdApi.AuthorizationStateWaitTdlibParameters -> {
                val params = TdApi.SetTdlibParameters()
                params.useTestDc = false
                params.databaseDirectory = File(context.filesDir, "tdlib").absolutePath
                params.filesDirectory = File(context.filesDir, "tdlib_files").absolutePath
                params.databaseEncryptionKey = byteArrayOf()
                params.useFileDatabase = true
                params.useChatInfoDatabase = true
                params.useMessageDatabase = true
                params.useSecretChats = false
                
                // User's verified credentials from my.telegram.org
                params.apiId = 29973280
                params.apiHash = "100089f491662a724c3e8b1cf1c0c58d"
                params.systemLanguageCode = "en"
                params.deviceModel = "Android Phone"
                params.systemVersion = "Android"
                params.applicationVersion = "1.0.0"

                client?.send(params, this)
            }
            is TdApi.AuthorizationStateWaitPhoneNumber -> {
                postState(AuthState.Idle)
            }
            is TdApi.AuthorizationStateWaitCode -> {
                val phone = preferences.userPhoneNumber
                postState(AuthState.WaitingForCode(phone, "tdlib"))
            }
            is TdApi.AuthorizationStateWaitPassword -> {
                postState(AuthState.WaitingForPassword(state.passwordHint ?: ""))
            }
            is TdApi.AuthorizationStateReady -> {
                preferences.isUserLoggedIn = true
                postState(AuthState.LoggedIn(preferences.userPhoneNumber, "Telegram Member"))
                loadJoinedChannels()
            }
            is TdApi.AuthorizationStateClosing -> {
                postState(AuthState.Idle)
            }
            is TdApi.AuthorizationStateClosed -> {
                client = null
                initClient()
            }
            is TdApi.AuthorizationStateLoggingOut -> {
                postState(AuthState.Loading)
            }
        }
    }

    override fun onResult(obj: TdApi.Object?) {
        if (obj is TdApi.Error) {
            Log.e(tag, "TDLib Error: ${obj.code} ${obj.message}")
            postState(AuthState.Error(obj.message ?: "Authentication error"))
        }
    }

    fun sendPhoneNumber(phone: String) {
        val cleanPhone = phone.trim().replace(" ", "").replace("-", "")
        preferences.userPhoneNumber = cleanPhone
        postState(AuthState.Loading)

        client?.send(
            TdApi.SetAuthenticationPhoneNumber(cleanPhone, null),
            { result ->
                if (result is TdApi.Error) {
                    Log.e(tag, "SetAuthenticationPhoneNumber error: ${result.message}")
                    postState(AuthState.Error(result.message ?: "Failed to send code"))
                }
            },
            null
        )
    }

    fun verifyCode(code: String) {
        postState(AuthState.Loading)
        client?.send(
            TdApi.CheckAuthenticationCode(code.trim()),
            { result ->
                if (result is TdApi.Error) {
                    Log.e(tag, "CheckAuthenticationCode error: ${result.message}")
                    postState(AuthState.Error(result.message ?: "Invalid code"))
                }
            },
            null
        )
    }

    fun verifyPassword(password: String) {
        postState(AuthState.Loading)
        client?.send(
            TdApi.CheckAuthenticationPassword(password),
            { result ->
                if (result is TdApi.Error) {
                    postState(AuthState.Error(result.message ?: "Invalid password"))
                }
            },
            null
        )
    }

    fun logout() {
        preferences.logout()
        client?.send(TdApi.LogOut(), this)
    }

    private fun loadJoinedChannels() {
        client?.send(
            TdApi.GetChats(TdApi.ChatListMain(), 100),
            { result ->
                if (result is TdApi.Chats) {
                    for (chatId in result.chatIds) {
                        client?.send(TdApi.GetChat(chatId), { chatResult ->
                            if (chatResult is TdApi.Chat) {
                                addChatIfChannel(chatResult)
                            }
                        }, null)
                    }
                }
            },
            null
        )
    }

    private fun addChatIfChannel(chat: TdApi.Chat) {
        val isSupergroupOrChannel = chat.type is TdApi.ChatTypeSupergroup
        val isPrivateChannel = (chat.type as? TdApi.ChatTypeSupergroup)?.isChannel == true

        synchronized(joinedChannels) {
            if (joinedChannels.none { it.id == chat.id }) {
                joinedChannels.add(
                    TelegramChat(
                        id = chat.id,
                        title = chat.title,
                        isChannel = true,
                        isPrivate = isPrivateChannel,
                        memberCountText = "Joined",
                        lastMessageText = "Media Channel"
                    )
                )
            }
        }
    }

    fun getJoinedChannels(): List<TelegramChat> {
        return synchronized(joinedChannels) {
            if (joinedChannels.isNotEmpty()) joinedChannels.toList()
            else getSampleChannels()
        }
    }

    private fun getSampleChannels(): List<TelegramChat> {
        return listOf(
            TelegramChat(
                id = -1001892837461L,
                title = "Exclusive 4K Cinema (Private)",
                isChannel = true,
                isPrivate = true,
                memberCountText = "Joined",
                lastMessageText = "Dune Part Two (2024)"
            ),
            TelegramChat(
                id = -1001472839120L,
                title = "Documentary Vault [Restricted]",
                isChannel = true,
                isPrivate = true,
                memberCountText = "Joined",
                lastMessageText = "Planet Earth III"
            )
        )
    }

    fun fetchChannelVideos(chatId: Long, onResult: (List<VideoItem>) -> Unit) {
        val req = TdApi.GetChatHistory(chatId, 0, 0, 50, false)
        client?.send(req, { result ->
            if (result is TdApi.Messages) {
                val videos = mutableListOf<VideoItem>()
                for (msg in result.messages) {
                    val content = msg.content
                    if (content is TdApi.MessageVideo) {
                        val v = content.video
                        val caption = content.caption?.text ?: ""
                        val title = caption.ifBlank { v.fileName.ifBlank { "Video #${msg.id}" } }
                        val minutes = v.duration / 60
                        val seconds = v.duration % 60
                        val durationStr = String.format("%02d:%02d", minutes, seconds)
                        val sizeMb = v.video.size / (1024 * 1024)

                        // Trigger download ahead of time so chunks start streaming
                        startFileDownload(v.video.id)

                        videos.add(
                            VideoItem(
                                id = "${msg.chatId}_${msg.id}",
                                title = title,
                                description = "Telegram Video (Message #${msg.id})",
                                durationText = durationStr,
                                channelTitle = "Telegram Channel",
                                telegramFileId = "${v.video.id}",
                                directUrl = "",
                                fileSizeFormatted = "${sizeMb} MB"
                            )
                        )
                    } else if (content is TdApi.MessageDocument) {
                        val doc = content.document
                        val mime = doc.mimeType ?: ""
                        val name = doc.fileName ?: ""
                        val isVideo = mime.startsWith("video/") || name.endsWith(".mp4", true) || name.endsWith(".mkv", true) || name.endsWith(".webm", true)
                        if (isVideo) {
                            val caption = content.caption?.text ?: ""
                            val title = caption.ifBlank { name.ifBlank { "Document #${msg.id}" } }
                            val sizeMb = doc.document.size / (1024 * 1024)

                            startFileDownload(doc.document.id)

                            videos.add(
                                VideoItem(
                                    id = "${msg.chatId}_${msg.id}",
                                    title = title,
                                    description = "Video Document (Message #${msg.id})",
                                    durationText = "--:--",
                                    channelTitle = "Telegram Document",
                                    telegramFileId = "${doc.document.id}",
                                    directUrl = "",
                                    fileSizeFormatted = "${sizeMb} MB"
                                )
                            )
                        }
                    }
                }
                mainHandler.post { onResult(videos) }
            } else {
                mainHandler.post { onResult(emptyList()) }
            }
        }, null)
    }

    fun startFileDownload(fileId: Int) {
        client?.send(TdApi.DownloadFile(fileId, 32, 0, 0, false), null, null)
    }

    fun getFile(fileId: Int, onResult: (TdApi.File?) -> Unit) {
        client?.send(TdApi.GetFile(fileId), { result ->
            if (result is TdApi.File) onResult(result)
            else onResult(null)
        }, null)
    }

    private fun postState(state: AuthState) {
        mainHandler.post {
            _authState.value = state
        }
    }
}
