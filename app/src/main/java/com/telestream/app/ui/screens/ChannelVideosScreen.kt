package com.telestream.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telestream.app.TeleStreamApp
import com.telestream.app.data.model.VideoItem
import com.telestream.app.ui.components.VideoCard
import com.telestream.app.ui.theme.DarkBackground
import com.telestream.app.ui.theme.PrimaryBlue
import com.telestream.app.ui.theme.TextMuted
import com.telestream.app.ui.theme.TextPrimary
import com.telestream.app.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelVideosScreen(
    channelId: Long,
    channelTitle: String,
    onPlayVideo: (streamUrl: String, title: String) -> Unit,
    onBack: () -> Unit
) {
    val authManager = remember { TeleStreamApp.instance.authManager }
    val proxy = remember { TeleStreamApp.instance.streamProxy }

    var isLoading by remember { mutableStateOf(true) }
    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }

    LaunchedEffect(channelId) {
        isLoading = true
        authManager.fetchChannelVideos(channelId) { result ->
            videos = result
            isLoading = false
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(channelTitle, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(
                            if (isLoading) "Loading channel..." else "${videos.size} videos available",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryBlue)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Fetching channel messages...", color = TextSecondary, fontSize = 13.sp)
                }
            }
        } else if (videos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No Video Files Found",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "No video files or mp4/mkv documents found in this channel yet. Upload or forward a video to this channel in Telegram to stream it here!",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                items(videos) { video ->
                    VideoCard(
                        video = video,
                        onClick = {
                            val streamUrl = if (video.telegramFileId.isNotEmpty()) {
                                proxy.getStreamUrl(fileId = video.telegramFileId)
                            } else {
                                proxy.getStreamUrl(directUrl = video.directUrl)
                            }
                            onPlayVideo(streamUrl, video.title)
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
