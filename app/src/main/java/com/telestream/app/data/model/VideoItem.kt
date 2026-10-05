package com.telestream.app.data.model

data class VideoItem(
    val id: String,
    val title: String,
    val description: String = "",
    val durationText: String = "00:00",
    val channelTitle: String = "Telegram Channel",
    val thumbnailUrl: String = "",
    val telegramFileId: String = "",
    val directUrl: String = "",
    val fileSizeFormatted: String = "0 MB"
)
