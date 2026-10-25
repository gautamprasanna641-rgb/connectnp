package com.example.ui.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.R
import com.example.data.ConnectNpRepository
import com.example.model.User
import com.example.ui.theme.ConnectNpBlue
import com.example.ui.theme.NepalCrimson
import com.example.util.MediaPickerType
import com.example.util.rememberGalleryLauncher

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostDialog(
    isNepali: Boolean,
    currentUser: User? = null,
    onDismiss: () -> Unit,
    onSubmitPost: (content: String, location: String?, mediaResId: Int?, mediaUri: Uri?) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var selectedLocation by remember { mutableStateOf("Kathmandu, Nepal") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var attachImage by remember { mutableStateOf(false) }
    var selectedMediaResId by remember { mutableIntStateOf(R.drawable.himalaya_banner_1792096400169) }
    var privacy by remember { mutableStateOf("Public") }

    val galleryLauncher = rememberGalleryLauncher(
        pickerType = MediaPickerType.IMAGE_AND_VIDEO,
        onMediaSelected = { uri ->
            selectedImageUri = uri
            attachImage = false // prefer picked URI from user's gallery
        }
    )

    val locations = listOf(
        "Kathmandu, Nepal",
        "Pokhara, Nepal",
        "Lalitpur, Nepal",
        "Everest Base Camp, Solukhumbu",
        "Bhaktapur Durbar Square",
        "Chitwan National Park"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("create_post_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isNepali) "नयाँ पोस्ट सिर्जना गर्नुहोस्" else "Create Post",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Author Info & Privacy
                val activeUser = currentUser ?: remember { ConnectNpRepository.getCurrentActiveUser() }
                val userFirstName = activeUser.name.split(" ").firstOrNull()?.ifBlank { "there" } ?: "there"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(ConnectNpBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!activeUser.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = activeUser.avatarUrl,
                                contentDescription = activeUser.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = activeUser.name.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = activeUser.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Public,
                                        contentDescription = "Public",
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = privacy,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Text Input
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = {
                        Text(
                            if (isNepali) "तपाईंको दिमागमा के छ, $userFirstName? केहि शेयर गर्नुहोस्..."
                            else "What's on your mind, $userFirstName? Connect with Nepal & the world..."
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("create_post_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                // Attached Gallery Image/Video Preview
                if (selectedImageUri != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, ConnectNpBlue.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    ) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = "Selected media from phone gallery",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { selectedImageUri = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove photo",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else if (attachImage) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        Image(
                            painter = painterResource(id = selectedMediaResId),
                            contentDescription = "Attached Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { attachImage = false },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Location Picker Chips
                Text(
                    text = if (isNepali) "📍 स्थान ट्याग:" else "📍 Add Location:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    locations.take(3).forEach { loc ->
                        val isSelected = selectedLocation == loc
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedLocation = loc },
                            label = { Text(loc.split(",").first(), fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Attachment Toggles (Gallery Picker + Presets)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isNepali) "तस्वीर / भिडियो थप्नुहोस्:" else "Add Photo / Media:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Real Phone Gallery Button with runtime permission request
                        Button(
                            onClick = { galleryLauncher.launch() },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ConnectNpBlue.copy(alpha = 0.15f),
                                contentColor = ConnectNpBlue
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("gallery_photo_picker_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddPhotoAlternate,
                                contentDescription = "Open Gallery",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isNepali) "ग्यालरी" else "Gallery",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(onClick = {
                            attachImage = true
                            selectedImageUri = null
                            selectedMediaResId = R.drawable.himalaya_banner_1792096400169
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Landscape,
                                contentDescription = "Preset Landscape",
                                tint = Color(0xFF42B72A)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Submit Button
                Button(
                    onClick = {
                        if (content.isNotBlank()) {
                            onSubmitPost(
                                content,
                                selectedLocation,
                                if (selectedImageUri == null && attachImage) selectedMediaResId else null,
                                selectedImageUri
                            )
                            onDismiss()
                        }
                    },
                    enabled = content.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_post_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ConnectNpBlue)
                ) {
                    Text(
                        text = if (isNepali) "पोस्ट प्रकाशित गर्नुहोस् 🚀" else "Publish Post 🚀",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
