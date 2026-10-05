package com.telestream.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telestream.app.TeleStreamApp
import com.telestream.app.ui.components.VideoCard
import com.telestream.app.ui.theme.DarkBackground
import com.telestream.app.ui.theme.TextMuted
import com.telestream.app.ui.theme.TextPrimary

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
    val videos = remember(channelId) { authManager.getVideosForChannel(channelId) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(channelTitle, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("${videos.size} videos available", color = TextMuted, fontSize = 11.sp)
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
                        val streamUrl = proxy.getStreamUrl(directUrl = video.directUrl)
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
