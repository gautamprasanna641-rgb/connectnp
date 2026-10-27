package com.example.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream

enum class MediaPickerType {
    IMAGE_ONLY,
    VIDEO_ONLY,
    IMAGE_AND_VIDEO
}

class GalleryLauncherState(
    val launch: () -> Unit
)

/**
 * Copies the media file to the app's cache directory so background threads,
 * Firebase Storage uploaders, and image loaders retain persistent read permissions.
 */
fun persistMediaUri(context: Context, sourceUri: Uri, isVideo: Boolean = false): Uri {
    return try {
        // Attempt to take persistable URI permission if offered
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(sourceUri, flags)
        } catch (_: Exception) {}

        val extension = if (isVideo) "mp4" else "jpg"
        val cacheDir = File(context.cacheDir, "gallery_cache").apply { mkdirs() }
        val targetFile = File(cacheDir, "media_${System.currentTimeMillis()}.$extension")

        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
                output.flush()
            }
        }
        if (targetFile.exists() && targetFile.length() > 0) {
            Uri.fromFile(targetFile)
        } else {
            sourceUri
        }
    } catch (e: Exception) {
        Log.w("GalleryPickerHelper", "persistMediaUri fallback to source: ${e.message}")
        sourceUri
    }
}

/**
 * Reusable Jetpack Compose helper to request gallery permissions at runtime
 * (READ_MEDIA_IMAGES / READ_MEDIA_VIDEO on Android 13+ and READ_EXTERNAL_STORAGE on older versions)
 * and launch the system Photo/Video Picker with fallback to GetContent intent.
 */
@Composable
fun rememberGalleryLauncher(
    pickerType: MediaPickerType = MediaPickerType.IMAGE_ONLY,
    onMediaSelected: (Uri) -> Unit,
    onPermissionDenied: (() -> Unit)? = null
): GalleryLauncherState {
    val context = LocalContext.current

    val visualMediaType = when (pickerType) {
        MediaPickerType.IMAGE_ONLY -> ActivityResultContracts.PickVisualMedia.ImageOnly
        MediaPickerType.VIDEO_ONLY -> ActivityResultContracts.PickVisualMedia.VideoOnly
        MediaPickerType.IMAGE_AND_VIDEO -> ActivityResultContracts.PickVisualMedia.ImageAndVideo
    }

    val mimeType = when (pickerType) {
        MediaPickerType.IMAGE_ONLY -> "image/*"
        MediaPickerType.VIDEO_ONLY -> "video/*"
        MediaPickerType.IMAGE_AND_VIDEO -> "*/*"
    }

    // Fallback GetContent launcher
    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val isVideo = pickerType == MediaPickerType.VIDEO_ONLY
            val safeUri = persistMediaUri(context, uri, isVideo)
            onMediaSelected(safeUri)
        }
    }

    // Primary PickVisualMedia launcher
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val isVideo = pickerType == MediaPickerType.VIDEO_ONLY
            val safeUri = persistMediaUri(context, uri, isVideo)
            onMediaSelected(safeUri)
        }
    }

    val launchPickerAction = {
        try {
            if (ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)) {
                pickerLauncher.launch(PickVisualMediaRequest(visualMediaType))
            } else {
                getContentLauncher.launch(mimeType)
            }
        } catch (e: Exception) {
            Log.w("GalleryPickerHelper", "PickVisualMedia failed, using GetContent fallback: ${e.message}")
            try {
                getContentLauncher.launch(mimeType)
            } catch (fallbackEx: Exception) {
                Toast.makeText(context, "Unable to open gallery: ${fallbackEx.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val anyGranted = permissions.values.any { it }
        val allGranted = permissions.values.all { it }
        if (allGranted || anyGranted) {
            launchPickerAction()
        } else {
            // Even if normal permission was denied, attempt PhotoPicker as it works without permissions on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context)) {
                launchPickerAction()
            } else {
                Toast.makeText(
                    context,
                    "Storage permission is needed to select photos or videos.",
                    Toast.LENGTH_SHORT
                ).show()
                onPermissionDenied?.invoke()
            }
        }
    }

    return remember(pickerType) {
        GalleryLauncherState(
            launch = {
                val permissionsToRequest = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                        when (pickerType) {
                            MediaPickerType.IMAGE_ONLY -> arrayOf(
                                Manifest.permission.READ_MEDIA_IMAGES,
                                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                            )
                            MediaPickerType.VIDEO_ONLY -> arrayOf(
                                Manifest.permission.READ_MEDIA_VIDEO,
                                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                            )
                            MediaPickerType.IMAGE_AND_VIDEO -> arrayOf(
                                Manifest.permission.READ_MEDIA_IMAGES,
                                Manifest.permission.READ_MEDIA_VIDEO,
                                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                            )
                        }
                    }
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

                if (allGranted || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ActivityResultContracts.PickVisualMedia.isPhotoPickerAvailable(context))) {
                    launchPickerAction()
                } else {
                    permissionLauncher.launch(permissionsToRequest)
                }
            }
        )
    }
}
