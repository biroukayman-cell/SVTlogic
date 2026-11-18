package com.example.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

object AppUpdateUrlHelper {

    private val DRIVE_FILE_D_REGEX = Regex(
        """https?://(?:drive|docs)\.google\.com/(?:[^?\s]+/)?file/d/([a-zA-Z0-9_-]+)""",
        RegexOption.IGNORE_CASE
    )
    private val DRIVE_QUERY_ID_REGEX = Regex(
        """https?://(?:drive\.google\.com/(?:open|uc)|drive\.usercontent\.google\.com/download)\?[^\s]*\bid=([a-zA-Z0-9_-]+)""",
        RegexOption.IGNORE_CASE
    )
    private val DRIVE_FOLDER_REGEX = Regex(
        """https?://drive\.google\.com/(?:drive/)?(?:u/\d+/)?folders/""",
        RegexOption.IGNORE_CASE
    )

    fun cleanRawUrl(rawUrl: String): String {
        return rawUrl.trim().trim('"', '\'', '<', '>')
    }

    fun isGoogleDriveFolderUrl(rawUrl: String): Boolean {
        val cleaned = cleanRawUrl(rawUrl)
        return DRIVE_FOLDER_REGEX.containsMatchIn(cleaned)
    }

    fun extractGoogleDriveFileId(rawUrl: String): String? {
        val cleaned = cleanRawUrl(rawUrl)
        if (cleaned.isBlank() || isGoogleDriveFolderUrl(cleaned)) return null
        DRIVE_FILE_D_REGEX.find(cleaned)?.groupValues?.getOrNull(1)?.let { id ->
            if (id.length >= 10) return id
        }
        DRIVE_QUERY_ID_REGEX.find(cleaned)?.groupValues?.getOrNull(1)?.let { id ->
            if (id.length >= 10) return id
        }
        return null
    }

    fun isGoogleDriveUrl(rawUrl: String): Boolean {
        val cleaned = cleanRawUrl(rawUrl).lowercase()
        return cleaned.contains("drive.google.com/") || cleaned.contains("drive.usercontent.google.com/")
    }

    fun normalizeDownloadUrl(rawUrl: String): String {
        val cleaned = cleanRawUrl(rawUrl)
        if (cleaned.isBlank()) return ""
        val driveFileId = extractGoogleDriveFileId(cleaned) ?: return cleaned
        return "https://drive.google.com/uc?export=download&confirm=t&id=$driveFileId"
    }

    fun buildUpdateViewIntent(context: Context, rawUrl: String): Intent {
        val normalized = normalizeDownloadUrl(rawUrl)
        val uri = Uri.parse(normalized)
        val baseIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (isGoogleDriveUrl(normalized)) {
            findBrowserPackage(context)?.let { pkg ->
                baseIntent.setPackage(pkg)
            }
        }
        return baseIntent
    }

    fun openUpdateUrl(context: Context, rawUrl: String): Boolean {
        val normalized = normalizeDownloadUrl(rawUrl)
        if (!normalized.startsWith("https://", ignoreCase = true) || normalized.length < 10) {
            return false
        }
        val intent = buildUpdateViewIntent(context, normalized)
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            if (intent.`package` != null) {
                try {
                    val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
                        addCategory(Intent.CATEGORY_BROWSABLE)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallback)
                    true
                } catch (_: Exception) {
                    false
                }
            } else {
                false
            }
        }
    }

    private fun findBrowserPackage(context: Context): String? {
        return runCatching {
            val probe = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
            val pm = context.packageManager
            val defaultPkg = pm.resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName
            if (!defaultPkg.isNullOrBlank() &&
                defaultPkg != "android" &&
                !defaultPkg.startsWith("com.android.internal") &&
                defaultPkg != context.packageName &&
                !defaultPkg.startsWith("com.google.android.apps.docs")
            ) {
                return@runCatching defaultPkg
            }
            val resolved = pm.queryIntentActivities(probe, PackageManager.MATCH_DEFAULT_ONLY)
            val candidates = resolved
                .mapNotNull { it.activityInfo?.packageName }
                .filter { pkg ->
                    pkg != context.packageName &&
                        pkg != "android" &&
                        !pkg.startsWith("com.android.internal") &&
                        !pkg.startsWith("com.google.android.apps.docs")
                }
            candidates.firstOrNull { it == "com.android.chrome" }
                ?: candidates.firstOrNull()
        }.getOrNull()
    }
}
