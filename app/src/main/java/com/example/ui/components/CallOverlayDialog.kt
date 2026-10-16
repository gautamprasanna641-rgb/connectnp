package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.User
import com.example.ui.theme.ConnectNpBlue
import com.example.ui.theme.ConnectNpSuccess
import com.example.ui.theme.NepalCrimson
import kotlinx.coroutines.delay

@Composable
fun CallOverlayDialog(
    user: User,
    isVideoCall: Boolean,
    onEndCall: () -> Unit
) {
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(true) }
    var isCameraOff by remember { mutableStateOf(false) }
    var callSeconds by remember { mutableIntStateOf(0) }
    var callState by remember { mutableStateOf("Connecting via WebRTC...") }

    LaunchedEffect(Unit) {
        delay(1200)
        callState = "Ringing..."
        delay(1800)
        callState = "Connected • End-to-End Encrypted"
        while (true) {
            delay(1000)
            callSeconds++
        }
    }

    val formattedTime = remember(callSeconds) {
        val mins = callSeconds / 60
        val secs = callSeconds % 60
        String.format("%02d:%02d", mins, secs)
    }

    Dialog(
        onDismissRequest = onEndCall,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF101924),
                            Color(0xFF1A2634),
                            Color(0xFF0D1117)
                        )
                    )
                )
                .testTag("call_overlay_dialog")
        ) {
            // Header Info
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Avatar / Video simulation box
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(ConnectNpBlue, NepalCrimson)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.name.take(1),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = user.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (callSeconds > 0) formattedTime else callState,
                        fontSize = 13.sp,
                        color = if (callSeconds > 0) ConnectNpSuccess else Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Encrypted",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isVideoCall) "ConnectNP HD Video Call" else "ConnectNP Voice Call",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            // Bottom Actions Bar
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mute
                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(if (isMuted) Color.White else Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                            contentDescription = "Mute",
                            tint = if (isMuted) Color.Black else Color.White
                        )
                    }

                    // Video toggle
                    if (isVideoCall) {
                        IconButton(
                            onClick = { isCameraOff = !isCameraOff },
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(if (isCameraOff) Color.White else Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = if (isCameraOff) Icons.Filled.VideocamOff else Icons.Filled.Videocam,
                                contentDescription = "Camera",
                                tint = if (isCameraOff) Color.Black else Color.White
                            )
                        }
                    }

                    // Speaker
                    IconButton(
                        onClick = { isSpeakerOn = !isSpeakerOn },
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(if (isSpeakerOn) ConnectNpBlue else Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = if (isSpeakerOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeMute,
                            contentDescription = "Speaker",
                            tint = Color.White
                        )
                    }

                    // End call
                    IconButton(
                        onClick = onEndCall,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(NepalCrimson)
                            .testTag("end_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CallEnd,
                            contentDescription = "End Call",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}
