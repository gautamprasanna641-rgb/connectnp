package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WatchVideo
import com.example.ui.theme.ConnectNpBlue
import com.example.ui.theme.NepalCrimson

@Composable
fun WatchScreen(
    videos: List<WatchVideo>,
    isNepali: Boolean,
    onToggleSubscribe: (String) -> Unit
) {
    var activeVideo by remember { mutableStateOf(videos.firstOrNull()) }
    var isPlaying by remember { mutableStateOf(true) }
    var videoProgress by remember { mutableFloatStateOf(0.35f) }
    var isLiked by remember { mutableStateOf(false) }

    val currentVideo = activeVideo ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("watch_screen")
    ) {
        // Video Player Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Color.Black)
        ) {
            if (currentVideo.imageResId != null) {
                Image(
                    painter = painterResource(id = currentVideo.imageResId),
                    contentDescription = currentVideo.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Dark overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )

            // Center Play/Pause button
            IconButton(
                onClick = { isPlaying = !isPlaying },
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Top Quality Badge
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Text(
                    text = "4K 60fps",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Bottom Scrubber Bar & Duration
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("06:45", color = Color.White, fontSize = 10.sp)
                    Text(currentVideo.duration, color = Color.White, fontSize = 10.sp)
                }
                Slider(
                    value = videoProgress,
                    onValueChange = { videoProgress = it },
                    colors = SliderDefaults.colors(
                        thumbColor = NepalCrimson,
                        activeTrackColor = NepalCrimson,
                        inactiveTrackColor = Color.White.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp)
                )
            }
        }

        // Details & Recommended Videos
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = currentVideo.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${currentVideo.views} • ${currentVideo.timeAgo} • CONNECTNP WATCH",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Channel Row with Subscribe button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ConnectNpBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentVideo.channel.name.take(1),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentVideo.channel.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${currentVideo.channel.followersCount} subscribers",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { onToggleSubscribe(currentVideo.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentVideo.isSubscribed) MaterialTheme.colorScheme.surfaceVariant else NepalCrimson
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (currentVideo.isSubscribed) (if (isNepali) "सदस्यता लिएको" else "Subscribed") else (if (isNepali) "सदस्यता लिनुहोस्" else "Subscribe"),
                                color = if (currentVideo.isSubscribed) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Actions Row (Like, Dislike, Share, Download, Playlist)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        FilledTonalButton(
                            onClick = { isLiked = !isLiked },
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                contentDescription = "Like",
                                tint = if (isLiked) ConnectNpBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(currentVideo.likesCount, fontSize = 12.sp)
                        }

                        FilledTonalButton(
                            onClick = {},
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", fontSize = 12.sp)
                        }

                        FilledTonalButton(
                            onClick = {},
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.PlaylistAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save", fontSize = 12.sp)
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    Text(
                        text = if (isNepali) "अर्को सिफारिस भिडियोहरू (Up Next)" else "Up Next on CONNECTNP",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // Other Recommended Videos
            items(videos.filter { it.id != currentVideo.id }) { video ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { activeVideo = video }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .height(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                    ) {
                        if (video.imageResId != null) {
                            Image(
                                painter = painterResource(id = video.imageResId),
                                contentDescription = video.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Black.copy(alpha = 0.8f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                        ) {
                            Text(
                                text = video.duration,
                                color = Color.White,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = video.title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 2
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = video.channel.name,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${video.views} • ${video.timeAgo}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
