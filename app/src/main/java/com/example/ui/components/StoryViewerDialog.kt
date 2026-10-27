package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.Story
import com.example.ui.theme.ConnectNpBlue
import com.example.ui.theme.NepalCrimson

@Composable
fun StoryViewerDialog(
    stories: List<Story>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onStoryViewed: (String) -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(initialIndex) }
    val currentStory = stories.getOrNull(currentIndex) ?: return
    var replyText by remember { mutableStateOf("") }
    var sentReaction by remember { mutableStateOf<String?>(null) }

    val progress = remember(currentIndex) { Animatable(0f) }

    LaunchedEffect(currentIndex) {
        onStoryViewed(currentStory.id)
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 5000, easing = LinearEasing)
        )
        if (currentIndex < stories.size - 1) {
            currentIndex++
        } else {
            onDismiss()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("story_viewer_dialog")
        ) {
            // Background Image or Gradient
            if (!currentStory.mediaUrl.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(currentStory.mediaUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = currentStory.caption,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Dark tint overlay for readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.6f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.8f)
                                )
                            )
                        )
                )
            } else if (currentStory.mediaResId != null) {
                Image(
                    painter = painterResource(id = currentStory.mediaResId),
                    contentDescription = currentStory.caption,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Dark tint overlay for readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.6f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.8f)
                                )
                            )
                        )
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(ConnectNpBlue, NepalCrimson, Color.Black)
                            )
                        )
                )
            }

            // Left / Right tap zones to navigate
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable {
                            if (currentIndex > 0) currentIndex--
                        }
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable {
                            if (currentIndex < stories.size - 1) currentIndex++ else onDismiss()
                        }
                )
            }

            // Top Header: Progress indicators and User info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp)
            ) {
                // Progress segments
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    stories.forEachIndexed { index, _ ->
                        val segmentProgress = when {
                            index < currentIndex -> 1f
                            index == currentIndex -> progress.value
                            else -> 0f
                        }
                        LinearProgressIndicator(
                            progress = { segmentProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Author row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentStory.author.name.take(1),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentStory.author.name,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            if (currentStory.isCloseFriends) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF42B72A)
                                ) {
                                    Text(
                                        text = "Close Friends",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = currentStory.timestamp,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Story",
                            tint = Color.White
                        )
                    }
                }
            }

            // Bottom Content: Caption + Reaction emojis + Reply box
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (currentStory.caption.isNotEmpty()) {
                    Text(
                        text = currentStory.caption,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                if (sentReaction != null) {
                    Text(
                        text = "Sent reaction $sentReaction!",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Quick Reactions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("❤️", "🙏", "🏔️", "🔥", "🇳🇵", "👏").forEach { emoji ->
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { sentReaction = emoji }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Reply input
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("Send reply to ${currentStory.author.name}...", color = Color.White.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            focusedContainerColor = Color.Black.copy(alpha = 0.4f),
                            unfocusedContainerColor = Color.Black.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (replyText.isNotBlank()) {
                                sentReaction = "💬 Message sent"
                                replyText = ""
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(ConnectNpBlue)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Reply",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
