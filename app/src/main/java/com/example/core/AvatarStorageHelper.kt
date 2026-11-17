package com.example.core

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object AvatarStorageHelper {
    private const val AVATARS_DIR = "avatars"
    private const val MAX_DIMENSION_PX = 200
    private const val MAX_JPEG_BYTES = 110_000
    private const val DATA_URI_PREFIX = "data:image/jpeg;base64,"

    fun isDataUri(value: String?): Boolean {
        return !value.isNullOrBlank() &&
            value.startsWith("data:image/", ignoreCase = true) &&
            value.contains(";base64,", ignoreCase = true)
    }

    fun isRemoteHttpUrl(value: String?): Boolean {
        return !value.isNullOrBlank() &&
            (value.startsWith("https://", ignoreCase = true) || value.startsWith("http://", ignoreCase = true))
    }

    fun decodeBase64Payload(dataUri: String): ByteArray? {
        if (!isDataUri(dataUri)) return null
        val marker = ";base64,"
        val idx = dataUri.indexOf(marker, ignoreCase = true)
        if (idx < 0) return null
        val b64 = dataUri.substring(idx + marker.length).trim()
        if (b64.isEmpty()) return null
        return runCatching { Base64.decode(b64, Base64.DEFAULT) }.getOrNull()
    }

    fun encodeJpegBytesToDataUri(jpegBytes: ByteArray): String {
        val encoded = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
        return "$DATA_URI_PREFIX$encoded"
    }

    fun localAvatarFile(context: Context, userId: String): File {
        val safeId = userId.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "default" }
        val dir = File(context.applicationContext.filesDir, AVATARS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "$safeId.jpg")
    }

    private fun readUriBytes(resolver: ContentResolver, sourceUri: Uri): ByteArray? {
        runCatching {
            resolver.openInputStream(sourceUri)?.use { input ->
                val bytes = input.readBytes()
                if (bytes.isNotEmpty()) return bytes
            }
        }
        runCatching {
            resolver.openAssetFileDescriptor(sourceUri, "r")?.use { afd ->
                afd.createInputStream().use { input ->
                    val bytes = input.readBytes()
                    if (bytes.isNotEmpty()) return bytes
                }
            }
        }
        runCatching {
            resolver.openTypedAssetFileDescriptor(sourceUri, "image/*", null)?.use { afd ->
                afd.createInputStream().use { input ->
                    val bytes = input.readBytes()
                    if (bytes.isNotEmpty()) return bytes
                }
            }
        }
        return null
    }

    private fun decodeBitmapFromBytes(bytes: ByteArray): Bitmap? {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)

        val srcW = boundsOpts.outWidth
        val srcH = boundsOpts.outHeight
        if (srcW > 0 && srcH > 0) {
            var sampleSize = 1
            while (max(srcW / sampleSize, srcH / sampleSize) > MAX_DIMENSION_PX * 2) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
            if (decoded != null) return decoded
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun decodeWithImageDecoder(resolver: ContentResolver, sourceUri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return runCatching {
            val source = ImageDecoder.createSource(resolver, sourceUri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val w = info.size.width
                val h = info.size.height
                if (w > 0 && h > 0 && max(w, h) > MAX_DIMENSION_PX * 2) {
                    val scale = (MAX_DIMENSION_PX * 2).toFloat() / max(w, h).toFloat()
                    decoder.setTargetSize(
                        (w * scale).roundToInt().coerceAtLeast(1),
                        (h * scale).roundToInt().coerceAtLeast(1)
                    )
                }
            }
        }.getOrNull()
    }

    suspend fun saveAndEncodeAvatar(
        context: Context,
        userId: String,
        sourceUri: Uri
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val primaryResolver = context.contentResolver
            val appResolver = context.applicationContext.contentResolver

            val imageBytes = readUriBytes(primaryResolver, sourceUri)
                ?: readUriBytes(appResolver, sourceUri)

            val rawBitmap = (if (imageBytes != null && imageBytes.isNotEmpty()) {
                decodeBitmapFromBytes(imageBytes)
            } else {
                null
            }) ?: decodeWithImageDecoder(primaryResolver, sourceUri)
                ?: decodeWithImageDecoder(appResolver, sourceUri)
                ?: error("Cannot read selected image")

            val scaledBitmap = scaleBitmapToMax(rawBitmap, MAX_DIMENSION_PX)
            if (scaledBitmap !== rawBitmap) {
                rawBitmap.recycle()
            }

            val compressedBytes = compressWithinLimit(scaledBitmap, MAX_JPEG_BYTES)
            scaledBitmap.recycle()

            val localFile = localAvatarFile(context.applicationContext, userId)
            FileOutputStream(localFile).use { fos ->
                fos.write(compressedBytes)
                fos.flush()
            }

            encodeJpegBytesToDataUri(compressedBytes)
        }
    }

    suspend fun resolveLocalAvatarBitmap(
        context: Context,
        userId: String,
        avatarUrl: String?
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (!avatarUrl.isNullOrBlank() && isDataUri(avatarUrl)) {
            val bytes = decodeBase64Payload(avatarUrl)
            if (bytes != null && bytes.isNotEmpty()) {
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bmp != null) {
                    runCatching {
                        val file = localAvatarFile(context, userId)
                        if (!file.exists()) {
                            FileOutputStream(file).use { it.write(bytes) }
                        }
                    }
                    return@withContext bmp
                }
            }
        }
        val cachedFile = localAvatarFile(context, userId)
        if (cachedFile.exists() && cachedFile.length() > 0L) {
            return@withContext runCatching {
                BitmapFactory.decodeFile(cachedFile.absolutePath)
            }.getOrNull()
        }
        null
    }

    private fun scaleBitmapToMax(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxDim && h <= maxDim) return bitmap
        val ratio = maxDim.toFloat() / max(w, h).toFloat()
        val newW = (w * ratio).roundToInt().coerceAtLeast(1)
        val newH = (h * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    private fun compressWithinLimit(bitmap: Bitmap, maxBytes: Int): ByteArray {
        val out = ByteArrayOutputStream()
        var quality = 82
        while (quality >= 40) {
            out.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            if (out.size() <= maxBytes) break
            quality -= 10
        }
        return out.toByteArray()
    }
}
