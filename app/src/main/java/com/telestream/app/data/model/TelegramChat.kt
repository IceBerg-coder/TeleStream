package com.telestream.app.data.model

data class TelegramChat(
    val id: Long,
    val title: String,
    val username: String = "",
    val photoUrl: String = "",
    val isChannel: Boolean = true,
    val isPrivate: Boolean = true,
    val memberCountText: String = "",
    val lastMessageText: String = ""
)
