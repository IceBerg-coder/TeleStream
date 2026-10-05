package com.telestream.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.telestream.app.TeleStreamApp
import com.telestream.app.data.telegram.AuthState
import com.telestream.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val authManager = remember { TeleStreamApp.instance.authManager }
    val authState by authManager.authState.collectAsState()

    var phoneNumber by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(authState) {
        if (authState is AuthState.LoggedIn) {
            onLoginSuccess()
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Log in to Telegram", color = TextPrimary, fontWeight = FontWeight.Bold) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurface)
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (authState) {
                            is AuthState.WaitingForPassword -> Icons.Default.Lock
                            is AuthState.WaitingForCode -> Icons.Default.Shield
                            else -> Icons.Default.Phone
                        },
                        contentDescription = "Auth",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when (authState) {
                        is AuthState.WaitingForCode -> "Enter Telegram Code"
                        is AuthState.WaitingForPassword -> "Enter 2FA Password"
                        else -> "Sign in with Telegram"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when (authState) {
                        is AuthState.WaitingForCode -> "We sent a 5-digit verification code to your Telegram app on your other devices."
                        is AuthState.WaitingForPassword -> "Your account is protected with a Two-Step Verification cloud password."
                        else -> "Log in with your Telegram account to stream directly from private channels and joined chats."
                    },
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                when (val state = authState) {
                    is AuthState.Idle, is AuthState.Error -> {
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            label = { Text("Phone Number") },
                            placeholder = { Text("+1234567890") },
                            leadingIcon = {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = PrimaryBlue)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = DarkCardBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (state is AuthState.Error) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = state.message, color = StatusError, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { authManager.sendPhoneNumber(phoneNumber) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("Next", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    is AuthState.WaitingForCode -> {
                        OutlinedTextField(
                            value = verificationCode,
                            onValueChange = { verificationCode = it },
                            label = { Text("5-Digit Code") },
                            placeholder = { Text("12345") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = DarkCardBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { authManager.verifyCode(verificationCode) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("Confirm Code", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    is AuthState.WaitingForPassword -> {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Cloud Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = DarkCardBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { authManager.verifyPassword(password) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("Log In", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }

                    is AuthState.Loading -> {
                        CircularProgressIndicator(color = PrimaryBlue, modifier = Modifier.size(40.dp))
                    }

                    is AuthState.LoggedIn -> {
                        Text(
                            text = "✓ Logged In as ${state.name}!",
                            color = StatusOnline,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Security",
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Encrypted direct MTProto connection",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
