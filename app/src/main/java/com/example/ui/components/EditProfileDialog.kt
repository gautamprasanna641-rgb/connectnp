package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.ConnectNpFirestoreRepository
import com.example.data.ConnectNpRepository
import com.example.util.ImageCompressor
import com.example.ui.theme.ConnectNpBlue
import com.example.ui.theme.NepalCrimson
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun EditProfileDialog(
    initialName: String,
    initialBio: String,
    initialPhotoUrl: String?,
    isNepali: Boolean,
    onDismiss: () -> Unit,
    onProfileUpdated: (name: String, bio: String, photoUrl: String?) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val firestoreRepo = remember { ConnectNpFirestoreRepository(context) }

    var editName by remember { mutableStateOf(initialName) }
    var editBio by remember { mutableStateOf(initialBio) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    val galleryLauncher = com.example.util.rememberGalleryLauncher(
        pickerType = com.example.util.MediaPickerType.IMAGE_ONLY,
        onMediaSelected = { uri ->
            selectedImageUri = uri
        }
    )

    Dialog(onDismissRequest = { if (!isSaving) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("edit_profile_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isNepali) "प्रोफाइल सम्पादन" else "Edit Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isSaving
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Profile Picture Avatar with Camera overlay
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .border(3.dp, ConnectNpBlue, CircleShape)
                        .clickable(enabled = !isSaving) {
                            galleryLauncher.launch()
                        }
                        .testTag("edit_avatar_picker"),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        selectedImageUri != null -> {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Selected profile picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        !initialPhotoUrl.isNullOrBlank() -> {
                            AsyncImage(
                                model = initialPhotoUrl,
                                contentDescription = "Current profile picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(listOf(ConnectNpBlue, NepalCrimson))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = editName.take(1).ifBlank { "C" }.uppercase(),
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Camera Badge overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(ConnectNpBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Change Photo",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(
                    onClick = {
                        galleryLauncher.launch()
                    },
                    enabled = !isSaving
                ) {
                    Text(
                        text = if (isNepali) "फोटो छान्नुहोस्" else "Change Profile Photo",
                        fontSize = 12.sp,
                        color = ConnectNpBlue,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Name Input
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text(if (isNepali) "पूरा नाम" else "Full Name") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = ConnectNpBlue)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_name_input"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSaving
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Bio Input
                OutlinedTextField(
                    value = editBio,
                    onValueChange = { editBio = it },
                    label = { Text(if (isNepali) "बायो" else "Bio") },
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = ConnectNpBlue)
                    },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_bio_input"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSaving
                )

                if (statusMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusMessage,
                        fontSize = 11.sp,
                        color = ConnectNpBlue,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isSaving
                    ) {
                        Text(if (isNepali) "रद्द गर्नुहोस्" else "Cancel")
                    }

                    Button(
                        onClick = {
                            val cleanName = editName.trim()
                            if (cleanName.isBlank()) {
                                Toast.makeText(
                                    context,
                                    if (isNepali) "कृपया नाम प्रविष्ट गर्नुहोस्" else "Name cannot be empty",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@Button
                            }

                            val isImageChanged = selectedImageUri != null
                            val isNameChanged = cleanName != initialName
                            val isBioChanged = editBio.trim() != initialBio

                            // Fast path: if nothing changed, close immediately
                            if (!isImageChanged && !isNameChanged && !isBioChanged) {
                                onDismiss()
                                return@Button
                            }

                            isSaving = true
                            statusMessage = if (isNepali) "सुरक्षित गरिँदैछ..." else "Saving changes..."

                            coroutineScope.launch {
                                try {
                                    // 1. Immediately compress profile photo locally if changed for instant high-quality preview
                                    val localCompressedUri = if (isImageChanged && selectedImageUri != null) {
                                        withContext(Dispatchers.IO) {
                                            ImageCompressor.compressProfileImageToFileUri(context, selectedImageUri!!)
                                        }
                                    } else null

                                    val immediatePhotoUrl = localCompressedUri?.toString() ?: initialPhotoUrl

                                    // 2. Immediately update in-memory active user and notify parent screens
                                    ConnectNpRepository.updateActiveUser(
                                        name = cleanName,
                                        bio = editBio.trim(),
                                        photoUrl = immediatePhotoUrl
                                    )
                                    onProfileUpdated(cleanName, editBio.trim(), immediatePhotoUrl)

                                    // 3. Fast save of text (name and bio) to Firestore using optimized SetOptions.merge
                                    withContext(Dispatchers.IO) {
                                        try {
                                            firestoreRepo.updateUserProfile(
                                                name = cleanName,
                                                bio = editBio.trim(),
                                                photoUrl = immediatePhotoUrl
                                            )
                                        } catch (dbEx: Exception) {
                                            android.util.Log.w("EditProfile", "Firestore fast text update: ${dbEx.message}")
                                        }
                                    }

                                    // 4. (REQUIREMENT 2) CLOSE LOADING DIALOG IMMEDIATELY!
                                    // Do NOT wait for network image upload to finish.
                                    isSaving = false
                                    onDismiss()

                                    Toast.makeText(
                                        context,
                                        if (isNepali) "प्रोफाइल सुरक्षित गरियो! 🇳🇵" else "Profile saved! 🇳🇵",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    // 5. Asynchronously upload compressed image in background if an image was selected
                                    if (isImageChanged && selectedImageUri != null) {
                                        val uploadSourceUri = localCompressedUri ?: selectedImageUri!!
                                        CoroutineScope(Dispatchers.IO).launch {
                                            try {
                                                val uploadResult = firestoreRepo.uploadProfilePicture(uploadSourceUri, context)
                                                val remoteDownloadUrl = uploadResult.getOrNull()
                                                if (!remoteDownloadUrl.isNullOrBlank()) {
                                                    // Update Firestore with final persistent Storage URL
                                                    firestoreRepo.updateProfilePhotoUrl(remoteDownloadUrl)
                                                    // Update repository state with persistent URL
                                                    ConnectNpRepository.updateActiveUser(photoUrl = remoteDownloadUrl)
                                                }
                                            } catch (e: Exception) {
                                                android.util.Log.w("EditProfile", "Background Storage upload: ${e.message}")
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("EditProfile", "Save profile general error: ${e.message}")
                                    isSaving = false
                                    onDismiss()
                                    Toast.makeText(
                                        context,
                                        if (isNepali) "परिवर्तनहरू सुरक्षित गरियो" else "Changes saved",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_profile_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ConnectNpBlue),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isNepali) "बचत..." else "Saving...", fontSize = 13.sp)
                        } else {
                            Text(if (isNepali) "बचत गर्नुहोस्" else "Save Changes", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
