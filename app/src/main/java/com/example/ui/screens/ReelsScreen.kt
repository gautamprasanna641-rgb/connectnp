package com.example.ui.screens

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.model.Reel
import com.example.ui.theme.ConnectNpBlue
import com.example.ui.theme.NepalCrimson
import com.example.util.MediaPickerType
import com.example.util.rememberGalleryLauncher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsScreen(
    reels: List<Reel>,
    isNepali: Boolean,
    onToggleLike: (String) -> Unit,
    onToggleSave: (String) -> Unit,
    onCreateReel: ((title: String, soundTitle: String, videoUri: Uri?) -> Unit)? = null
) {
    val listState = rememberLazyListState()
    var activeCommentReelId by remember { mutableStateOf<String?>(null) }
    var showCreateReelDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("reels_screen")
    ) {
        if (reels.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoCameraBack,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isNepali) "कुनै रिल्स फेला परेन" else "No Reels Yet",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isNepali) "पहिलो रिल अपलोड गर्नुहोस् र ट्रेन्ड सुरु गर्नुहोस्!"
                               else "Be the first to upload a Reel and start a trend!",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showCreateReelDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NepalCrimson),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isNepali) "रिल बनाउनुहोस्" else "Create Reel")
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(reels, key = { _, reel -> reel.id }) { index, reel ->
                    ReelPageItem(
                        reel = reel,
                        isNepali = isNepali,
                        onToggleLike = { onToggleLike(reel.id) },
                        onToggleSave = { onToggleSave(reel.id) },
                        onOpenComments = { activeCommentReelId = reel.id }
                    )
                }
            }
        }

        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isNepali) "रिल्स (REELS)" else "CONNECTNP REELS",
                fontFamily = com.example.ui.theme.PoppinsFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                letterSpacing = 1.sp,
                color = Color.White
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NepalCrimson
                ) {
                    Text(
                        text = "LIVE 🔴",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { showCreateReelDialog = true },
                    modifier = Modifier.testTag("create_reel_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Create Reel",
                        tint = Color.White
                    )
                }
            }
        }
    }

    if (activeCommentReelId != null) {
        ModalBottomSheet(onDismissRequest = { activeCommentReelId = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = if (isNepali) "रिल्स टिप्पणीहरू" else "Reel Comments",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "🔥 Love the vibes and authentic music from Nepal!",
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "🇳🇵 Super proud of ConnectNP community! Best reels experience.",
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showCreateReelDialog) {
        CreateReelDialog(
            isNepali = isNepali,
            onDismiss = { showCreateReelDialog = false },
            onSubmitReel = { title, soundTitle, videoUri ->
                onCreateReel?.invoke(title, soundTitle, videoUri)
                showCreateReelDialog = false
            }
        )
    }
}

@Composable
fun LazyItemScope.ReelPageItem(
    reel: Reel,
    isNepali: Boolean,
    onToggleLike: () -> Unit,
    onToggleSave: () -> Unit,
    onOpenComments: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(true) }

    // Spinning Vinyl record animation
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        modifier = Modifier
            .fillParentMaxHeight()
            .fillMaxWidth()
            .clickable { isPlaying = !isPlaying }
    ) {
        // Dynamic Video Media / Simulation Background
        if (!reel.videoUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(reel.videoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = reel.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    reel.videoGradientColors.map { Color(it) }
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    reel.videoGradientColors.map { Color(it) }
                                )
                            )
                    )
                }
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            reel.videoGradientColors.map { Color(it) }
                        )
                    )
            )
        }

        // Overlay scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
        )

        // Play/Pause indicator icon when paused
        if (!isPlaying) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Paused",
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        // Right side Action Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 70.dp, end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Like
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onToggleLike,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                        .testTag("reel_like_${reel.id}")
                ) {
                    Icon(
                        imageVector = if (reel.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (reel.isLiked) NepalCrimson else Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = "${reel.likesCount}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Comment
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = onOpenComments,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChatBubble,
                        contentDescription = "Comments",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "${reel.commentsCount}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Share / Remix
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { /* Share */ },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "${reel.sharesCount}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Save
            IconButton(
                onClick = onToggleSave,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Icon(
                    imageVector = if (reel.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Save",
                    tint = if (reel.isSaved) ConnectNpBlue else Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Spinning Sound Vinyl Record
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .rotate(if (isPlaying) rotation else 0f)
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape)
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Music",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Bottom Left Creator Info & Audio
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .padding(start = 16.dp, bottom = 64.dp)
        ) {
            // Creator Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(ConnectNpBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = reel.creator.name.take(1),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = reel.creator.handle,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                    modifier = Modifier.clickable { /* Follow */ }
                ) {
                    Text(
                        text = "Follow",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Caption
            Text(
                text = reel.title,
                color = Color.White,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Audio track label
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = reel.soundTitle,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun CreateReelDialog(
    isNepali: Boolean,
    onDismiss: () -> Unit,
    onSubmitReel: (title: String, soundTitle: String, videoUri: Uri?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var soundTitle by remember { mutableStateOf("Trending Sound • Nepal") }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

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
                .testTag("create_reel_dialog")
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
                        text = if (isNepali) "नयाँ रिल सिर्जना गर्नुहोस्" else "Create ConnectNP Reel",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss, enabled = !isUploading) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Video Picker Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clickable(enabled = !isUploading) { videoPickerLauncher.launch() }
                        .testTag("pick_reel_video_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedVideoUri != null)
                            NepalCrimson.copy(alpha = 0.08f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selectedVideoUri != null) NepalCrimson else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                ) {
                    if (selectedVideoUri != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(selectedVideoUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Selected Reel Video",
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
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = null,
                                tint = NepalCrimson,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isNepali) "ग्यालरीबाट भिडियो छान्नुहोस्" else "Select Video from Gallery",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isNepali) "MP4, MOV वा HD भिडियो" else "MP4, MOV or HD Video Reel",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Reel Caption / Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isNepali) "शीर्षक / क्याप्सन" else "Caption / Title") },
                    placeholder = { Text(if (isNepali) "केहि रमाइलो लेख्नुहोस्... #Nepal #Vibes" else "Describe your reel... #Nepal #Trending") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reel_caption_input"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isUploading,
                    singleLine = false,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Sound / Audio Title
                OutlinedTextField(
                    value = soundTitle,
                    onValueChange = { soundTitle = it },
                    label = { Text(if (isNepali) "ध्वनि / संगीत" else "Audio Track / Sound") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = NepalCrimson)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reel_sound_input"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isUploading,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(18.dp))

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
                                val finalTitle = title.ifBlank { "ConnectNP Reel Vibes" }
                                onSubmitReel(finalTitle, soundTitle, selectedVideoUri)
                            }
                        },
                        enabled = !isUploading && (title.isNotBlank() || selectedVideoUri != null),
                        colors = ButtonDefaults.buttonColors(containerColor = NepalCrimson),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("submit_reel_button")
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isNepali) "रिल पोस्ट गर्नुहोस्" else "Post Reel")
                    }
                }
            }
        }
    }
}
