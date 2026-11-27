package com.example.core

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.PersistableBundle
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter

data class QrScanResult(
    val rawText: String,
    val classToken: String,
    val massarNumber: String?
)

object QrCodeUtils {
    private val decodeHints: Map<DecodeHintType, Any> = mapOf(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.TRY_HARDER to true,
        DecodeHintType.CHARACTER_SET to "UTF-8"
    )

    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap {
        val hints = mapOf(
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )
        val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap.setPixel(
                    x,
                    y,
                    if (bitMatrix[x, y]) Color.rgb(20, 90, 62) else Color.WHITE
                )
            }
        }
        return bitmap
    }

    fun parseScanPayload(rawValue: String): QrScanResult {
        val token = extractTokenFromScan(rawValue)
        val massar = extractMassarFromScan(rawValue)
        return QrScanResult(
            rawText = rawValue.trim(),
            classToken = token,
            massarNumber = massar
        )
    }

    fun extractTokenFromScan(rawValue: String): String {
        val unwrapped = unwrapWebQrQuery(rawValue.trim()).first
        val withoutPrefix = when {
            unwrapped.startsWith("BIOTERRE:", ignoreCase = true) ->
                unwrapped.substring("BIOTERRE:".length).trim()
            else -> unwrapped
        }
        return withoutPrefix.substringBefore("|").trim()
    }

    fun extractMassarFromScan(rawValue: String): String? {
        val (unwrapped, queryMassar) = unwrapWebQrQuery(rawValue.trim())
        val withoutPrefix = when {
            unwrapped.startsWith("BIOTERRE:", ignoreCase = true) ->
                unwrapped.substring("BIOTERRE:".length).trim()
            else -> unwrapped
        }
        if ("|" in withoutPrefix) {
            return withoutPrefix.substringAfter("|").trim().takeIf { it.isNotBlank() }
        }
        return queryMassar?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun unwrapWebQrQuery(trimmed: String): Pair<String, String?> {
        if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return trimmed to null
        }
        val uri = runCatching { java.net.URI(trimmed) }.getOrNull() ?: return trimmed to null
        val rawQuery = uri.rawQuery.orEmpty()
        if (rawQuery.isBlank()) return trimmed to null

        val params = rawQuery
            .split('&')
            .mapNotNull { part ->
                val idx = part.indexOf('=')
                if (idx <= 0) null
                else {
                    val key = runCatching { java.net.URLDecoder.decode(part.substring(0, idx), "UTF-8") }.getOrDefault(part.substring(0, idx))
                    val value = runCatching { java.net.URLDecoder.decode(part.substring(idx + 1), "UTF-8") }.getOrDefault(part.substring(idx + 1))
                    key.lowercase() to value
                }
            }
            .toMap()

        val qrOrToken = params["qr"]?.takeIf { it.isNotBlank() }
            ?: params["token"]?.takeIf { it.isNotBlank() }
            ?: params["code"]?.takeIf { it.isNotBlank() }
            ?: return trimmed to null
        val massar = params["massar"]?.takeIf { it.isNotBlank() }
        return qrOrToken to massar
    }

    fun copyTextToClipboard(context: Context, label: String, text: String): Boolean {
        return runCatching {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return false
            val clip = ClipData.newPlainText(label, text).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val sensitiveExtra = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ClipDescription.EXTRA_IS_SENSITIVE
                    } else {
                        "android.content.extra.IS_SENSITIVE"
                    }
                    description.extras = PersistableBundle().apply {
                        putBoolean(sensitiveExtra, true)
                    }
                }
            }
            clipboard.setPrimaryClip(clip)
            true
        }.getOrDefault(false)
    }

    fun decodeQrFromBitmap(bitmap: Bitmap): QrScanResult? {
        val scaled = if (bitmap.width > 1400 || bitmap.height > 1400) {
            val ratio = minOf(1400f / bitmap.width.toFloat(), 1400f / bitmap.height.toFloat())
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt().coerceAtLeast(1),
                (bitmap.height * ratio).toInt().coerceAtLeast(1),
                true
            )
        } else {
            bitmap
        }
        val width = scaled.width
        val height = scaled.height
        val pixels = IntArray(width * height)
        scaled.getPixels(pixels, 0, width, 0, 0, width, height)
        val source: LuminanceSource = RGBLuminanceSource(width, height, pixels)
        val decoded = decodeWithFallbacks(source) ?: return null
        return parseScanPayload(decoded)
    }

    internal fun decodeWithFallbacks(source: LuminanceSource): String? {
        val reader = MultiFormatReader().apply { setHints(decodeHints) }
        val candidates = listOf(
            { BinaryBitmap(HybridBinarizer(source)) },
            { BinaryBitmap(GlobalHistogramBinarizer(source)) },
            { BinaryBitmap(HybridBinarizer(source.invert())) }
        )
        for (makeBitmap in candidates) {
            try {
                val res = reader.decodeWithState(makeBitmap())
                if (!res.text.isNullOrBlank()) {
                    return res.text
                }
            } catch (_: Exception) {
                // Try next binarizer
            } finally {
                reader.reset()
            }
        }
        return null
    }
}

class QrImageAnalyzer(
    private val onQrDetected: (QrScanResult) -> Unit
) : ImageAnalysis.Analyzer {

    @Volatile
    private var alreadyDetected = false

    fun reset() {
        alreadyDetected = false
    }

    override fun analyze(image: ImageProxy) {
        if (alreadyDetected) {
            image.close()
            return
        }
        try {
            val plane = image.planes.firstOrNull()
            if (plane != null) {
                val lumaBytes = extractLumaPlane(plane, image.width, image.height)
                val source = PlanarYUVLuminanceSource(
                    lumaBytes,
                    image.width,
                    image.height,
                    0,
                    0,
                    image.width,
                    image.height,
                    false
                )
                val decodedText = QrCodeUtils.decodeWithFallbacks(source)
                if (!decodedText.isNullOrBlank()) {
                    alreadyDetected = true
                    onQrDetected(QrCodeUtils.parseScanPayload(decodedText))
                }
            }
        } catch (_: Exception) {
            // Frame did not contain a readable QR code
        } finally {
            image.close()
        }
    }

    private fun extractLumaPlane(plane: ImageProxy.PlaneProxy, width: Int, height: Int): ByteArray {
        val buffer = plane.buffer.duplicate()
        buffer.rewind()
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val remaining = buffer.remaining()

        if (pixelStride == 1 && rowStride == width && remaining >= width * height) {
            val out = ByteArray(width * height)
            buffer.get(out, 0, out.size)
            return out
        }

        val raw = ByteArray(remaining)
        buffer.get(raw)
        val compact = ByteArray(width * height)
        var dstIdx = 0
        for (row in 0 until height) {
            val rowOffset = row * rowStride
            if (pixelStride == 1) {
                val copyLen = minOf(width, raw.size - rowOffset)
                if (copyLen > 0) {
                    System.arraycopy(raw, rowOffset, compact, dstIdx, copyLen)
                }
                dstIdx += width
            } else {
                for (col in 0 until width) {
                    val srcIdx = rowOffset + col * pixelStride
                    if (srcIdx < raw.size) {
                        compact[dstIdx] = raw[srcIdx]
                    }
                    dstIdx++
                }
            }
        }
        return compact
    }
}
