package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

enum class MediaPickerType {
    IMAGE_ONLY,
    VIDEO_ONLY,
    IMAGE_AND_VIDEO
}

class GalleryLauncherState(
    val launch: () -> Unit
)

/**
 * Reusable Jetpack Compose helper to request gallery permissions at runtime
 * (READ_MEDIA_IMAGES / READ_MEDIA_VIDEO on Android 13+ and READ_EXTERNAL_STORAGE on older versions)
 * and launch the system Photo Picker upon approval.
 */
@Composable
fun rememberGalleryLauncher(
    pickerType: MediaPickerType = MediaPickerType.IMAGE_ONLY,
    onMediaSelected: (Uri) -> Unit,
    onPermissionDenied: (() -> Unit)? = null
): GalleryLauncherState {
    val context = LocalContext.current

    // Media Picker launcher
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onMediaSelected(uri)
        }
    }

    val visualMediaType = when (pickerType) {
        MediaPickerType.IMAGE_ONLY -> ActivityResultContracts.PickVisualMedia.ImageOnly
        MediaPickerType.VIDEO_ONLY -> ActivityResultContracts.PickVisualMedia.VideoOnly
        MediaPickerType.IMAGE_AND_VIDEO -> ActivityResultContracts.PickVisualMedia.ImageAndVideo
    }

    // Permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val anyGranted = permissions.values.any { it }
        val allGranted = permissions.values.all { it }
        if (allGranted || anyGranted) {
            pickerLauncher.launch(PickVisualMediaRequest(visualMediaType))
        } else {
            Toast.makeText(
                context,
                "Gallery permission is required to select photos or videos.",
                Toast.LENGTH_SHORT
            ).show()
            onPermissionDenied?.invoke()
        }
    }

    return remember(pickerType) {
        GalleryLauncherState(
            launch = {
                val permissionsToRequest = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                        when (pickerType) {
                            MediaPickerType.IMAGE_ONLY -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                            MediaPickerType.VIDEO_ONLY -> arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
                            MediaPickerType.IMAGE_AND_VIDEO -> arrayOf(
                                Manifest.permission.READ_MEDIA_IMAGES,
                                Manifest.permission.READ_MEDIA_VIDEO
                            )
                        }
                    }
                    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                }

                val allGranted = permissionsToRequest.all { permission ->
                    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
                }

                if (allGranted) {
                    pickerLauncher.launch(PickVisualMediaRequest(visualMediaType))
                } else {
                    permissionLauncher.launch(permissionsToRequest)
                }
            }
        )
    }
}
