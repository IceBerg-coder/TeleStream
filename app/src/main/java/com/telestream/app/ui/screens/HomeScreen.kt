package com.telestream.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telestream.app.TeleStreamApp
import com.telestream.app.data.model.VideoItem
import com.telestream.app.ui.components.VideoCard
import com.telestream.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPlayVideo: (streamUrl: String, title: String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLogin: () -> Unit,
    onOpenChannels: () -> Unit
) {
    val proxy = remember { TeleStreamApp.instance.streamProxy }
    val authManager = remember { TeleStreamApp.instance.authManager }
    val authState by authManager.authState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showCustomUrlDialog by remember { mutableStateOf(false) }
    var customUrlInput by remember { mutableStateOf("") }
    var customTitleInput by remember { mutableStateOf("") }

    // Sample/demo videos available immediately for testing + Telegram ready
    val defaultVideos = remember {
        listOf(
            VideoItem(
                id = "demo_1",
                title = "Big Buck Bunny 4K (Fast-Start Test Stream)",
                description = "High bitrate test stream validating HTTP Range headers and localhost chunk proxying",
                durationText = "09:56",
                channelTitle = "@TeleStreamArchive",
                thumbnailUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=800&auto=format&fit=crop",
                directUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                fileSizeFormatted = "158 MB"
            ),
            VideoItem(
                id = "demo_2",
                title = "Elephants Dream (H.264 / AAC 1080p)",
                description = "Open movie file testing audio/video synchronization through local proxy bridge",
                durationText = "10:53",
                channelTitle = "@CinemaCloud",
                thumbnailUrl = "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=800&auto=format&fit=crop",
                directUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                fileSizeFormatted = "220 MB"
            ),
            VideoItem(
                id = "demo_3",
                title = "Tears of Steel (Sci-Fi VFX Test)",
                description = "Demonstrating instant seek buffering and low latency streaming",
                durationText = "12:14",
                channelTitle = "@SciFiStorage",
                thumbnailUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&auto=format&fit=crop",
                directUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                fileSizeFormatted = "340 MB"
            )
        )
    }

    val filteredVideos = remember(searchQuery, defaultVideos) {
        if (searchQuery.isBlank()) defaultVideos
        else defaultVideos.filter { it.title.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        containerColor = DarkBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCustomUrlDialog = true },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Stream Custom URL")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title & Settings Action
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(StatusOnline)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ZERO-COST DIRECT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusOnline,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "TeleStream",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(DarkSurface)
                            .border(1.dp, DarkCardBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextPrimary
                        )
                    }
                }
            }

            // Hero Featured Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(DarkSurfaceVariant, DarkSurface)
                            )
                        )
                        .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp))
                        .clickable {
                            val hero = defaultVideos.first()
                            val streamUrl = proxy.getStreamUrl(hero.directUrl)
                            onPlayVideo(streamUrl, hero.title)
                        }
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PrimaryBlue.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "FEATURED STREAM",
                                color = PrimaryBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Stream Any Telegram Video",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Streams directly from Telegram servers at maximum bandwidth with 0 server costs.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(PrimaryBlue)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Watch Demo",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Private Channels & User Authentication Status Card
            item {
                if (authState is com.telestream.app.data.telegram.AuthState.LoggedIn) {
                    val user = authState as com.telestream.app.data.telegram.AuthState.LoggedIn
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, Color(0xFFD946EF).copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .clickable(onClick = onOpenChannels),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1428))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "🔒 MY PRIVATE CHANNELS",
                                        color = Color(0xFFD946EF),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Browse Joined Channels",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Logged in as ${user.name} • 3 channels ready",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            Button(
                                onClick = onOpenChannels,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD946EF)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Browse", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, PrimaryBlue.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .clickable(onClick = onOpenLogin),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "🔓 STREAM PRIVATE CHANNELS",
                                        color = AccentCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Log In With Telegram",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Stream from channels you don't own with your Telegram account.",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            Button(
                                onClick = onOpenLogin,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Log In", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search catalog or channel...", color = TextMuted) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = TextMuted)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = DarkCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // Section Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Available Videos",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${filteredVideos.size} items",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }

            // Video Grid List
            items(filteredVideos) { video ->
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
                Spacer(modifier = Modifier.height(60.dp))
            }
        }

        // Custom Stream Dialog
        if (showCustomUrlDialog) {
            AlertDialog(
                onDismissRequest = { showCustomUrlDialog = false },
                containerColor = DarkSurface,
                title = { Text("Stream Telegram URL / File", color = TextPrimary) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Enter a Telegram video link, direct MP4 URL, or Telegram file_id:",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        OutlinedTextField(
                            value = customTitleInput,
                            onValueChange = { customTitleInput = it },
                            label = { Text("Video Title") },
                            placeholder = { Text("My Movie") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        OutlinedTextField(
                            value = customUrlInput,
                            onValueChange = { customUrlInput = it },
                            label = { Text("Direct URL or File ID") },
                            placeholder = { Text("https://... or BAcAA...") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (customUrlInput.isNotBlank()) {
                                showCustomUrlDialog = false
                                val targetUrl = if (customUrlInput.startsWith("http")) {
                                    proxy.getStreamUrl(directUrl = customUrlInput.trim())
                                } else {
                                    proxy.getStreamUrl(fileId = customUrlInput.trim())
                                }
                                onPlayVideo(
                                    targetUrl,
                                    customTitleInput.ifBlank { "Telegram Stream" }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Stream Now", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomUrlDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }
    }
}
