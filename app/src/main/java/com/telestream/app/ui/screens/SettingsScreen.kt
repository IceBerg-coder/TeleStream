package com.telestream.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telestream.app.TeleStreamApp
import com.telestream.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val prefs = remember { TeleStreamApp.instance.appPreferences }

    var botToken by remember { mutableStateOf(prefs.botToken) }
    var channelId by remember { mutableStateOf(prefs.channelId) }
    var apiId by remember { mutableStateOf(prefs.apiId) }
    var apiHash by remember { mutableStateOf(prefs.apiHash) }
    var showSavedSnackbar by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Telegram Configuration", color = TextPrimary, fontWeight = FontWeight.Bold) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Session Management Card
            val authManager = remember { TeleStreamApp.instance.authManager }
            val authState by authManager.authState.collectAsState()

            Card(
                colors = CardDefaults.cardColors(containerColor = if (prefs.isUserLoggedIn) Color(0xFF1E1428) else DarkSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (prefs.isUserLoggedIn) Color(0xFFD946EF).copy(alpha = 0.5f) else DarkCardBorder,
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (prefs.isUserLoggedIn) "Telegram User Session" else "Private Channel Access",
                                color = if (prefs.isUserLoggedIn) Color(0xFFD946EF) else PrimaryBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = if (prefs.isUserLoggedIn) prefs.userPhoneNumber.ifEmpty { "Connected Member" } else "Not logged in",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        if (prefs.isUserLoggedIn) {
                            OutlinedButton(
                                onClick = { authManager.logout() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Log Out", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (prefs.isUserLoggedIn)
                            "You are logged in with your Telegram account. TeleStream can stream videos directly from any private channel you belong to."
                        else
                            "Log in with your Telegram account to access and stream from third-party private channels where you are a member.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // Explanatory Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "100% Free Telegram Storage",
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Telegram allows free video hosting up to 2GB per file. Provide your free Bot Token from @BotFather to let TeleStream stream directly to your phone without any middleman server.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // Input Fields
            OutlinedTextField(
                value = botToken,
                onValueChange = { botToken = it },
                label = { Text("Telegram Bot Token") },
                placeholder = { Text("123456789:ABCdefGhIJKlmNoPQRstuVWXyz") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = DarkCardBorder
                )
            )

            OutlinedTextField(
                value = channelId,
                onValueChange = { channelId = it },
                label = { Text("Target Channel Username or ID") },
                placeholder = { Text("@my_movie_channel or -100xxxxxxxx") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = DarkCardBorder
                )
            )

            Divider(color = DarkCardBorder, modifier = Modifier.padding(vertical = 8.dp))

            Text(
                text = "Optional: MTProto Client Credentials",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = apiId,
                onValueChange = { apiId = it },
                label = { Text("API ID (from my.telegram.org)") },
                placeholder = { Text("12345678") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = DarkCardBorder
                )
            )

            OutlinedTextField(
                value = apiHash,
                onValueChange = { apiHash = it },
                label = { Text("API Hash") },
                placeholder = { Text("abcdef0123456789...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = DarkCardBorder
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button
            Button(
                onClick = {
                    prefs.botToken = botToken
                    prefs.channelId = channelId
                    prefs.apiId = apiId
                    prefs.apiHash = apiHash
                    showSavedSnackbar = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Configuration", color = Color.White, fontWeight = FontWeight.Bold)
            }

            if (showSavedSnackbar) {
                Text(
                    text = "✓ Configuration saved successfully!",
                    color = StatusOnline,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
