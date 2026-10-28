package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageCompressor {

    private const val TAG = "ImageCompressor"
    private const val DEFAULT_MAX_WIDTH = 512
    private const val DEFAULT_MAX_HEIGHT = 512
    private const val DEFAULT_JPEG_QUALITY = 80

    /**
     * Compresses the image at [imageUri] to a JPEG byte array, scaling down to at most
     * [maxWidth] x [maxHeight] pixels and compressing with [quality]% JPEG quality.
     */
    fun compressImageUriToBytes(
        context: Context,
        imageUri: Uri,
        maxWidth: Int = DEFAULT_MAX_WIDTH,
        maxHeight: Int = DEFAULT_MAX_HEIGHT,
        quality: Int = DEFAULT_JPEG_QUALITY
    ): ByteArray? {
        return try {
            val contentResolver = context.contentResolver

            // 1. First pass: decode bounds only to compute inSampleSize and avoid OOM
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(imageUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, boundsOptions)
            } ?: return null

            val origWidth = boundsOptions.outWidth
            val origHeight = boundsOptions.outHeight
            if (origWidth <= 0 || origHeight <= 0) return null

            var inSampleSize = 1
            if (origHeight > maxHeight || origWidth > maxWidth) {
                val halfHeight = origHeight / 2
                val halfWidth = origWidth / 2
                while ((halfHeight / inSampleSize) >= maxHeight && (halfWidth / inSampleSize) >= maxWidth) {
                    inSampleSize *= 2
                }
            }

            // 2. Second pass: decode sub-sampled bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            val sampledBitmap = contentResolver.openInputStream(imageUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOptions)
            } ?: return null

            // 3. Exact scaling if the sampled bitmap is still larger than max dimensions
            val finalBitmap = if (sampledBitmap.width > maxWidth || sampledBitmap.height > maxHeight) {
                val ratio = minOf(
                    maxWidth.toFloat() / sampledBitmap.width,
                    maxHeight.toFloat() / sampledBitmap.height
                )
                val targetWidth = (sampledBitmap.width * ratio).toInt().coerceAtLeast(1)
                val targetHeight = (sampledBitmap.height * ratio).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(sampledBitmap, targetWidth, targetHeight, true).also {
                    if (it != sampledBitmap) {
                        sampledBitmap.recycle()
                    }
                }
            } else {
                sampledBitmap
            }

            // 4. Compress to JPEG
            val byteStream = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteStream)
            val compressedBytes = byteStream.toByteArray()
            finalBitmap.recycle()

            Log.d(TAG, "Compressed image from ${origWidth}x${origHeight} to ${compressedBytes.size / 1024} KB")
            compressedBytes
        } catch (e: Exception) {
            Log.w(TAG, "Failed to compress image URI $imageUri: ${e.message}")
            null
        }
    }

    /**
     * Compresses the image at [imageUri] and saves it to a temporary file in the app cache directory,
     * returning a local file Uri for instant rendering.
     */
    fun compressImageUriToFileUri(
        context: Context,
        imageUri: Uri,
        maxWidth: Int = DEFAULT_MAX_WIDTH,
        maxHeight: Int = DEFAULT_MAX_HEIGHT,
        quality: Int = DEFAULT_JPEG_QUALITY
    ): Uri? {
        val bytes = compressImageUriToBytes(context, imageUri, maxWidth, maxHeight, quality) ?: return null
        return try {
            val cacheFile = File(context.cacheDir, "avatar_compressed_${System.currentTimeMillis()}.jpg")
            FileOutputStream(cacheFile).use { out ->
                out.write(bytes)
                out.flush()
            }
            Uri.fromFile(cacheFile)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save compressed image to cache: ${e.message}")
            null
        }
    }
    /**
     * Dedicated fast profile image compression to 384x384 JPEG at 75% quality (~25-45 KB),
     * ensuring rapid uploads and low memory consumption.
     */
    fun compressProfileImageToBytes(
        context: Context,
        imageUri: Uri
    ): ByteArray? {
        return compressImageUriToBytes(
            context = context,
            imageUri = imageUri,
            maxWidth = 384,
            maxHeight = 384,
            quality = 75
        )
    }

    /**
     * Compresses the profile image and saves it to cache for instant rendering without waiting for upload.
     */
    fun compressProfileImageToFileUri(
        context: Context,
        imageUri: Uri
    ): Uri? {
        return compressImageUriToFileUri(
            context = context,
            imageUri = imageUri,
            maxWidth = 384,
            maxHeight = 384,
            quality = 75
        )
    }

    /**
     * Compresses post images to at most 1080x1080 at 80% JPEG quality,
     * drastically reducing network payload from 5-15MB to ~150-250KB.
     */
    fun compressPostImageToBytes(
        context: Context,
        imageUri: Uri
    ): ByteArray? {
        return compressImageUriToBytes(
            context = context,
            imageUri = imageUri,
            maxWidth = 1080,
            maxHeight = 1080,
            quality = 80
        )
    }

    /**
     * Compresses marketplace product images to at most 800x800 at 80% JPEG quality,
     * ensuring fast grid loading while preserving product detail.
     */
    fun compressMarketplaceImageToBytes(
        context: Context,
        imageUri: Uri
    ): ByteArray? {
        return compressImageUriToBytes(
            context = context,
            imageUri = imageUri,
            maxWidth = 800,
            maxHeight = 800,
            quality = 80
        )
    }
}
