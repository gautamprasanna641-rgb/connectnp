package com.example.ui.screens

import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.model.WatchVideo
import com.example.ui.theme.ConnectNpBlue
import com.example.ui.theme.NepalCrimson
import com.example.util.MediaPickerType
import com.example.util.rememberGalleryLauncher

@Composable
fun WatchScreen(
    videos: List<WatchVideo>,
    isNepali: Boolean,
    onToggleSubscribe: (String) -> Unit,
    onUploadVideo: ((title: String, description: String, category: String, videoUri: Uri?) -> Unit)? = null
) {
    var activeVideo by remember(videos) { mutableStateOf(videos.firstOrNull()) }
    var isPlaying by remember { mutableStateOf(true) }
    var videoProgress by remember { mutableFloatStateOf(0.35f) }
    var isLiked by remember { mutableStateOf(false) }
    var showUploadDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("watch_screen")
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isNepali) "कनेक्टएनपी वाच" else "CONNECTNP WATCH",
                fontFamily = com.example.ui.theme.PoppinsFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.primary
            )
            FilledTonalButton(
                onClick = { showUploadDialog = true },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("upload_watch_video_button")
            ) {
                Icon(
                    imageVector = Icons.Default.VideoCall,
                    contentDescription = "Upload Video",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isNepali) "भिडियो अपलोड" else "Upload Video",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        val currentVideo = activeVideo
        if (currentVideo == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isNepali) "कुनै भिडियो उपलब्ध छैन" else "No Videos Available",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isNepali) "पहिलो भिडियो अपलोड गर्नुहोस्!" else "Be the first to upload a video!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showUploadDialog = true },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Upload, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isNepali) "भिडियो अपलोड गर्नुहोस्" else "Upload Video")
                    }
                }
            }
        } else {
            // Video Player Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(Color.Black)
            ) {
                if (!currentVideo.videoUrl.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(currentVideo.videoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = currentVideo.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (currentVideo.imageResId != null) {
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
                        if (!video.videoUrl.isNullOrBlank()) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(video.videoUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = video.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (video.imageResId != null) {
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

    if (showUploadDialog) {
        UploadWatchVideoDialog(
            isNepali = isNepali,
            onDismiss = { showUploadDialog = false },
            onSubmit = { title, description, category, videoUri ->
                onUploadVideo?.invoke(title, description, category, videoUri)
                showUploadDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadWatchVideoDialog(
    isNepali: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (title: String, description: String, category: String, videoUri: Uri?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Travel & Vlog") }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val categories = listOf("Travel & Vlog", "Music", "News", "Culture", "Tech", "Community")

    val videoPickerLauncher = rememberGalleryLauncher(
        pickerType = MediaPickerType.VIDEO_ONLY,
        onMediaSelected = { uri ->
            selectedVideoUri = uri
        }
    )

    Dialog(onDismissRequest = { if (!isUploading) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("upload_watch_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isNepali) "भिडियो अपलोड गर्नुहोस्" else "Upload Watch Video",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, enabled = !isUploading) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Video Picker Banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clickable(enabled = !isUploading) { videoPickerLauncher.launch() }
                        .testTag("pick_watch_video_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedVideoUri != null)
                            ConnectNpBlue.copy(alpha = 0.08f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selectedVideoUri != null) ConnectNpBlue else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                ) {
                    if (selectedVideoUri != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(selectedVideoUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Selected Video",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.75f),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isNepali) "भिडियो चयन गरियो (परिवर्तन गर्न थिच्नुहोस्)" else "Video Selected (Tap to change)",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoCall,
                                contentDescription = null,
                                tint = ConnectNpBlue,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isNepali) "ग्यालरीबाट भिडियो छान्नुहोस्" else "Select Video File",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isNepali) "MP4 वा HD भिडियो" else "MP4 or HD Video",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isNepali) "भिडियोको शीर्षक" else "Video Title") },
                    placeholder = { Text(if (isNepali) "उदा: हाम्रो नेपाल यात्रा २०२६" else "e.g. Kathmandu Valley Heritage Tour") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("watch_title_input"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isUploading,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category chips
                Text(
                    text = if (isNepali) "वर्ग (Category)" else "Category",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.take(3).forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (isNepali) "विवरण" else "Description") },
                    placeholder = { Text(if (isNepali) "भिडियोको बारेमा केहि लेख्नुहोस्..." else "Tell viewers about this video...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("watch_description_input"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isUploading,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isUploading
                    ) {
                        Text(if (isNepali) "रद्द गर्नुहोस्" else "Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (title.isNotBlank() || selectedVideoUri != null) {
                                isUploading = true
                                val finalTitle = title.ifBlank { "ConnectNP Watch Video" }
                                onSubmit(finalTitle, description, selectedCategory, selectedVideoUri)
                            }
                        },
                        enabled = !isUploading && (title.isNotBlank() || selectedVideoUri != null),
                        colors = ButtonDefaults.buttonColors(containerColor = ConnectNpBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("submit_watch_video_button")
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isNepali) "प्रकाशित गर्नुहोस्" else "Publish Video")
                    }
                }
            }
        }
    }
}
