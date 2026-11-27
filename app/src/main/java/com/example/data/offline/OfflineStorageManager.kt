package com.example.data.offline

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.example.data.model.CourseResource
import com.example.data.model.QuestionAnswerRecord
import com.example.data.model.Quiz
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizQuestion
import com.example.data.model.VideoTracking
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date
import java.util.Locale
import java.util.UUID

class LocalUriUnavailableException(
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause)

class PickedFileTooLargeException(
    val sizeBytes: Long,
    val maxBytes: Long
) : IllegalStateException("Picked file size $sizeBytes exceeds max $maxBytes")

data class StagedPickedFile(
    val localFileUri: String,
    val displayName: String,
    val sizeBytes: Long
)

data class DownloadedResource(
    val resourceId: String,
    val classId: String,
    val teacherId: String,
    val title: String,
    val description: String,
    val type: String,
    val remoteUrl: String,
    val localFilePath: String,
    val fileSizeBytes: Long,
    val durationSeconds: Long,
    val downloadedAtMillis: Long,
    val publicDisplayPath: String = "",
    val publicDownloadUri: String = ""
) {
    fun toCourseResource(): CourseResource = CourseResource(
        id = resourceId,
        classId = classId,
        teacherId = teacherId,
        title = title,
        description = description,
        type = type,
        fileUrl = remoteUrl,
        durationSeconds = durationSeconds,
        isDownloadable = true
    )

    fun localFileUri(): String = Uri.fromFile(File(localFilePath)).toString()
}

data class PendingQuizAttempt(
    val attemptId: String,
    val quizId: String,
    val quizTitle: String,
    val classId: String,
    val teacherId: String,
    val studentId: String,
    val studentName: String,
    val score: Double,
    val maxScore: Double,
    val totalDurationSeconds: Long,
    val answers: List<QuestionAnswerRecord>,
    val createdAtMillis: Long
) {
    fun toQuizAttempt(): QuizAttempt = QuizAttempt(
        id = attemptId,
        quizId = quizId,
        quizTitle = quizTitle,
        classId = classId,
        teacherId = teacherId,
        studentId = studentId,
        studentName = studentName,
        score = score,
        maxScore = maxScore,
        totalDurationSeconds = totalDurationSeconds,
        answers = answers,
        completedAt = Timestamp(Date(createdAtMillis)),
        createdAt = Timestamp(Date(createdAtMillis))
    )
}

data class PendingVideoTracking(
    val resource: CourseResource,
    val studentName: String,
    val maxPositionSeconds: Long,
    val totalDurationSeconds: Long,
    val updatedAtMillis: Long,
    val watchedPercentageOverride: Double = 0.0
) {
    fun toVideoTracking(studentId: String): VideoTracking {
        val safeDur = (if (totalDurationSeconds > 0L) totalDurationSeconds else resource.durationSeconds).coerceAtLeast(1L)
        val safePos = maxPositionSeconds.coerceIn(0L, safeDur)
        val computedPct = ((safePos.toDouble() / safeDur.toDouble()) * 100.0).coerceIn(0.0, 100.0)
        val pct = maxOf(computedPct, watchedPercentageOverride.coerceIn(0.0, 100.0)).coerceIn(0.0, 100.0)
        val status = when {
            pct >= 90.0 -> "completed"
            pct > 0.0 -> "partial"
            else -> "not_started"
        }
        return VideoTracking(
            id = "${resource.id}_$studentId",
            resourceId = resource.id,
            classId = resource.classId,
            teacherId = resource.teacherId,
            studentId = studentId,
            studentName = studentName,
            maxPositionSeconds = safePos,
            durationSeconds = safeDur,
            watchedPercentage = pct,
            status = status
        )
    }
}

class OfflineStorageManager(
    context: Context,
    private val userId: String,
    private val firestore: FirebaseFirestore? = null
) {
    private val appContext = context.applicationContext
    private val safeUserDirName = userId.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "default" }
    private val baseDir = File(appContext.filesDir, "offline_svt/$safeUserDirName").apply { mkdirs() }
    private val resourcesFilesDir = File(baseDir, "resources").apply { mkdirs() }

    private val resourcesIndexFile = File(baseDir, "resources_index.json")
    private val quizzesIndexFile = File(baseDir, "quizzes_index.json")
    private val pendingAttemptsFile = File(baseDir, "pending_attempts.json")
    private val pendingVideoFile = File(baseDir, "pending_video_tracking.json")

    private val lock = Any()

    private val _downloadedResources = MutableStateFlow<Map<String, DownloadedResource>>(emptyMap())
    val downloadedResources: StateFlow<Map<String, DownloadedResource>> = _downloadedResources.asStateFlow()

    private val _downloadedQuizzes = MutableStateFlow<Map<String, Quiz>>(emptyMap())
    val downloadedQuizzes: StateFlow<Map<String, Quiz>> = _downloadedQuizzes.asStateFlow()

    private val _pendingAttempts = MutableStateFlow<List<PendingQuizAttempt>>(emptyList())
    val pendingAttempts: StateFlow<List<PendingQuizAttempt>> = _pendingAttempts.asStateFlow()

    private val _pendingVideoTrackings = MutableStateFlow<List<PendingVideoTracking>>(emptyList())
    val pendingVideoTrackings: StateFlow<List<PendingVideoTracking>> = _pendingVideoTrackings.asStateFlow()

    private val _downloadingProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadingProgress: StateFlow<Map<String, Float>> = _downloadingProgress.asStateFlow()

    init {
        loadAllFromDisk()
    }

    private fun loadAllFromDisk() {
        synchronized(lock) {
            _downloadedResources.value = readDownloadedResources()
            _downloadedQuizzes.value = readDownloadedQuizzes()
            _pendingAttempts.value = readPendingAttempts()
            _pendingVideoTrackings.value = readPendingVideoTrackings()
        }
    }

    suspend fun ensureLocalCacheForViewing(resource: CourseResource): File = withContext(Dispatchers.IO) {
        val existing = _downloadedResources.value[resource.id]
        if (existing != null) {
            val f = File(existing.localFilePath)
            if (isFileInsideDir(f, resourcesFilesDir) && f.exists() && f.length() > 0L) {
                return@withContext f
            }
        }
        val downloaded = downloadResource(resource, exportToPublicDownloads = false)
        File(downloaded.localFilePath)
    }

    suspend fun downloadResource(
        resource: CourseResource,
        exportToPublicDownloads: Boolean = true
    ): DownloadedResource = withContext(Dispatchers.IO) {
        val safeResId = resource.id.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "res_${System.currentTimeMillis()}" }
        val normalizedUrl = normalizeDownloadUrl(resource.fileUrl)
        val ext = inferExtension(normalizedUrl, resource.type)
        val targetFile = File(resourcesFilesDir, "$safeResId.$ext")
        val tempFile = File(resourcesFilesDir, "$safeResId.$ext.tmp")

        _downloadingProgress.value = _downloadingProgress.value + (resource.id to 0.05f)
        try {
            val alreadyCached = targetFile.exists() && targetFile.length() > 0L
            if (!alreadyCached) {
                val uri = Uri.parse(normalizedUrl)
                when (uri.scheme?.lowercase(Locale.US)) {
                    FIRESTORE_CHUNK_SCHEME -> {
                        val chunkResourceId = parseChunkResourceId(normalizedUrl)
                            ?: throw IllegalArgumentException("Invalid Firestore chunk URI: $normalizedUrl")
                        downloadFirestoreChunks(
                            chunkResourceId = chunkResourceId,
                            classId = resource.classId,
                            teacherId = resource.teacherId,
                            target = tempFile
                        ) { fraction ->
                            _downloadingProgress.value = _downloadingProgress.value + (resource.id to fraction.coerceIn(0.05f, 0.99f))
                        }
                    }
                    "https" -> {
                        downloadHttpFile(normalizedUrl, tempFile) { fraction ->
                            _downloadingProgress.value = _downloadingProgress.value + (resource.id to fraction.coerceIn(0.05f, 0.99f))
                        }
                    }
                    "content" -> {
                        copyUriFile(uri, tempFile) { fraction ->
                            _downloadingProgress.value = _downloadingProgress.value + (resource.id to fraction.coerceIn(0.05f, 0.99f))
                        }
                    }
                    "file" -> {
                        val localPath = uri.path ?: throw SecurityException("Missing local file path")
                        val candidateFile = File(localPath)
                        if (!isFileInsideDir(candidateFile, appContext.cacheDir)) {
                            throw SecurityException("Untrusted file:// URI outside app cache rejected")
                        }
                        copyUriFile(uri, tempFile) { fraction ->
                            _downloadingProgress.value = _downloadingProgress.value + (resource.id to fraction.coerceIn(0.05f, 0.99f))
                        }
                    }
                    else -> {
                        throw IllegalArgumentException("Unsupported or insecure URI scheme for offline download: ${resource.fileUrl}")
                    }
                }

                if (targetFile.exists()) targetFile.delete()
                if (!tempFile.renameTo(targetFile)) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }
            }

            val existingRecord = _downloadedResources.value[resource.id]
            var publicDisplayPath = existingRecord?.publicDisplayPath.orEmpty()
            var publicDownloadUri = existingRecord?.publicDownloadUri.orEmpty()
            if (exportToPublicDownloads && (publicDisplayPath.isBlank() || !publicTargetExists(publicDownloadUri))) {
                val mime = mimeTypeForResource(resource.type, normalizedUrl)
                val exported = runCatching {
                    saveFileToPublicDownloads(
                        sourceFile = targetFile,
                        title = resource.title,
                        fallbackId = safeResId,
                        ext = ext,
                        mimeType = mime
                    )
                }.getOrNull()
                if (exported != null) {
                    publicDisplayPath = exported.first
                    publicDownloadUri = exported.second
                }
            }

            val record = DownloadedResource(
                resourceId = resource.id,
                classId = resource.classId,
                teacherId = resource.teacherId,
                title = resource.title,
                description = resource.description,
                type = resource.type,
                remoteUrl = resource.fileUrl,
                localFilePath = targetFile.absolutePath,
                fileSizeBytes = targetFile.length(),
                durationSeconds = resource.durationSeconds,
                downloadedAtMillis = System.currentTimeMillis(),
                publicDisplayPath = publicDisplayPath,
                publicDownloadUri = publicDownloadUri
            )
            synchronized(lock) {
                val updated = _downloadedResources.value + (resource.id to record)
                writeDownloadedResources(updated)
                _downloadedResources.value = updated
            }
            record
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            throw e
        } finally {
            _downloadingProgress.value = _downloadingProgress.value - resource.id
        }
    }

    suspend fun removeDownloadedResource(resourceId: String) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val existing = _downloadedResources.value[resourceId]
            if (existing != null) {
                runCatching { File(existing.localFilePath).delete() }
                deletePublicDownload(existing.publicDownloadUri)
                val updated = _downloadedResources.value - resourceId
                writeDownloadedResources(updated)
                _downloadedResources.value = updated
            }
        }
    }

    private fun publicTargetExists(uriString: String): Boolean {
        if (uriString.isBlank()) return false
        return runCatching {
            val uri = Uri.parse(uriString)
            when (uri.scheme?.lowercase(Locale.US)) {
                "file" -> uri.path?.let { File(it).exists() } ?: false
                "content" -> appContext.contentResolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
                else -> false
            }
        }.getOrDefault(false)
    }

    private fun deletePublicDownload(uriString: String) {
        if (uriString.isBlank()) return
        runCatching {
            val uri = Uri.parse(uriString)
            when (uri.scheme?.lowercase(Locale.US)) {
                "file" -> uri.path?.let { File(it).delete() }
                "content" -> appContext.contentResolver.delete(uri, null, null)
            }
        }
    }

    private fun saveFileToPublicDownloads(
        sourceFile: File,
        title: String,
        fallbackId: String,
        ext: String,
        mimeType: String
    ): Pair<String, String> {
        val fileName = buildSafeDownloadFileName(title, fallbackId, ext)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$PUBLIC_DOWNLOAD_SUBDIR")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = appContext.contentResolver
            val insertedUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("MediaStore Downloads insert failed")
            try {
                resolver.openOutputStream(insertedUri)?.use { output ->
                    FileInputStream(sourceFile).use { input ->
                        input.copyTo(output, bufferSize = 16 * 1024)
                    }
                    output.flush()
                } ?: throw IllegalStateException("Cannot open MediaStore output stream")

                val doneValues = ContentValues().apply {
                    put(MediaStore.Downloads.IS_PENDING, 0)
                }
                resolver.update(insertedUri, doneValues, null, null)
                "Téléchargements/$PUBLIC_DOWNLOAD_SUBDIR/$fileName" to insertedUri.toString()
            } catch (e: Exception) {
                runCatching { resolver.delete(insertedUri, null, null) }
                throw e
            }
        } else {
            val downloadsDir = File(
                appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: baseDir,
                PUBLIC_DOWNLOAD_SUBDIR
            ).apply { mkdirs() }
            val destFile = File(downloadsDir, fileName)
            sourceFile.copyTo(destFile, overwrite = true)
            "Téléchargements/$PUBLIC_DOWNLOAD_SUBDIR/$fileName" to Uri.fromFile(destFile).toString()
        }
    }

    suspend fun saveQuizOffline(quiz: Quiz) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val updated = _downloadedQuizzes.value + (quiz.id to quiz.copy(isDownloadable = true))
            writeDownloadedQuizzes(updated)
            _downloadedQuizzes.value = updated
        }
    }

    suspend fun removeOfflineQuiz(quizId: String) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val updated = _downloadedQuizzes.value - quizId
            writeDownloadedQuizzes(updated)
            _downloadedQuizzes.value = updated
        }
    }

    suspend fun enqueuePendingAttempt(attempt: PendingQuizAttempt) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val filtered = _pendingAttempts.value.filterNot {
                it.attemptId == attempt.attemptId || it.quizId == attempt.quizId
            }
            val updated = filtered + attempt
            writePendingAttempts(updated)
            _pendingAttempts.value = updated
        }
    }

    suspend fun removePendingAttempt(attemptId: String) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val updated = _pendingAttempts.value.filterNot { it.attemptId == attemptId }
            writePendingAttempts(updated)
            _pendingAttempts.value = updated
        }
    }

    suspend fun enqueuePendingVideoTracking(tracking: PendingVideoTracking) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val existing = _pendingVideoTrackings.value.firstOrNull { it.resource.id == tracking.resource.id }
            val mergedPos = maxOf(existing?.maxPositionSeconds ?: 0L, tracking.maxPositionSeconds)
            val mergedDur = if (tracking.totalDurationSeconds > 0L) tracking.totalDurationSeconds else (existing?.totalDurationSeconds ?: 0L)
            val mergedPct = maxOf(existing?.watchedPercentageOverride ?: 0.0, tracking.watchedPercentageOverride)
            val merged = tracking.copy(
                maxPositionSeconds = mergedPos,
                totalDurationSeconds = mergedDur,
                watchedPercentageOverride = mergedPct
            )
            val updated = _pendingVideoTrackings.value.filterNot { it.resource.id == tracking.resource.id } + merged
            writePendingVideoTrackings(updated)
            _pendingVideoTrackings.value = updated
        }
    }

    suspend fun removePendingVideoTracking(resourceId: String) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val updated = _pendingVideoTrackings.value.filterNot { it.resource.id == resourceId }
            writePendingVideoTrackings(updated)
            _pendingVideoTrackings.value = updated
        }
    }

    private fun downloadHttpFile(urlString: String, target: File, onProgress: (Float) -> Unit) {
        var currentUrl = urlString
        var redirects = 0
        var connection: HttpURLConnection? = null
        try {
            while (redirects < 5) {
                if (!isSafeRemoteHttpsUrl(currentUrl)) {
                    throw SecurityException("Blocked non-HTTPS or unsafe remote host")
                }
                val url = URL(currentUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = false
                    requestMethod = "GET"
                }
                val code = conn.responseCode
                if (code in 300..399) {
                    val loc = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (loc.isNullOrBlank()) throw IllegalStateException("Redirect without location ($code)")
                    val nextUrl = URL(url, loc).toString()
                    if (!isSafeRemoteHttpsUrl(nextUrl)) {
                        throw SecurityException("Blocked unsafe redirect target")
                    }
                    currentUrl = nextUrl
                    redirects++
                } else {
                    connection = conn
                    break
                }
            }
            val conn = connection ?: throw IllegalStateException("Too many redirects")
            val code = conn.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("HTTP error $code while downloading resource")
            }
            val totalBytes = conn.contentLengthLong
            if (totalBytes > MAX_HTTP_DOWNLOAD_BYTES) {
                throw IllegalStateException("Remote resource exceeds max download limit")
            }
            conn.inputStream.use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var downloaded = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        downloaded += read
                        if (downloaded > MAX_HTTP_DOWNLOAD_BYTES) {
                            throw IllegalStateException("Remote resource exceeds max download limit")
                        }
                        output.write(buffer, 0, read)
                        if (totalBytes > 0L) {
                            onProgress(downloaded.toFloat() / totalBytes.toFloat())
                        }
                    }
                    output.flush()
                }
            }
        } finally {
            connection?.disconnect()
        }
    }

    private suspend fun downloadFirestoreChunks(
        chunkResourceId: String,
        classId: String,
        teacherId: String,
        target: File,
        onProgress: (Float) -> Unit
    ) {
        val db = firestore ?: throw IllegalStateException("Firestore instance missing for chunk download")
        var query = db.collection("resource_chunks")
            .whereEqualTo("resourceId", chunkResourceId)
        if (teacherId.isNotBlank() && userId == teacherId) {
            query = query.whereEqualTo("teacherId", userId)
        } else if (classId.isNotBlank()) {
            query = query.whereEqualTo("classId", classId)
        } else {
            throw LocalUriUnavailableException("Missing classId or teacher ownership for chunk download ($chunkResourceId)")
        }
        val snap = query
            .get()
            .await()
        if (snap.isEmpty) {
            throw LocalUriUnavailableException("Resource file chunks not found on server ($chunkResourceId)")
        }
        val sortedDocs = snap.documents.sortedBy { it.getLong("chunkIndex") ?: 0L }
        val expectedTotal = sortedDocs.firstOrNull()?.getLong("totalChunks")?.toInt() ?: sortedDocs.size
        if (sortedDocs.size < expectedTotal) {
            throw IllegalStateException("Incomplete resource file (${sortedDocs.size}/$expectedTotal parts)")
        }
        FileOutputStream(target).use { output ->
            sortedDocs.forEachIndexed { index, doc ->
                val bytes = doc.getBlob("data")?.toBytes()
                    ?: throw IllegalStateException("Corrupt chunk $index for $chunkResourceId")
                output.write(bytes)
                onProgress(((index + 1).toFloat() / sortedDocs.size.toFloat()).coerceIn(0.05f, 0.99f))
            }
            output.flush()
        }
        onProgress(1f)
    }

    private fun copyUriFile(uri: Uri, target: File, onProgress: (Float) -> Unit) {
        val input = try {
            appContext.contentResolver.openInputStream(uri)
        } catch (sec: SecurityException) {
            throw LocalUriUnavailableException("Local URI permission denied: $uri", sec)
        } catch (e: Exception) {
            throw LocalUriUnavailableException("Cannot open local URI: $uri", e)
        } ?: throw LocalUriUnavailableException("Cannot open local file URI for offline caching")

        input.use { stream ->
            FileOutputStream(target).use { output ->
                val buffer = ByteArray(16 * 1024)
                while (true) {
                    val read = stream.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                }
                output.flush()
            }
        }
        onProgress(1f)
    }

    companion object {
        const val FIRESTORE_CHUNK_SCHEME = "firestore"
        const val FIRESTORE_CHUNK_HOST = "resource_chunks"
        const val PUBLIC_DOWNLOAD_SUBDIR = "BioTerre SVT"
        private const val MAX_HTTP_DOWNLOAD_BYTES = 100L * 1024L * 1024L
        private val CHUNK_ID_REGEX = Regex("^[a-zA-Z0-9_-]{1,128}$")

        internal fun isFileInsideDir(candidate: File, parentDir: File): Boolean {
            return runCatching {
                val parentPath = parentDir.canonicalPath.trimEnd(File.separatorChar) + File.separatorChar
                val candidatePath = candidate.canonicalPath
                candidatePath.startsWith(parentPath)
            }.getOrDefault(false)
        }

        internal fun isSafeRemoteHttpsUrl(rawUrl: String): Boolean {
            val url = runCatching { URL(rawUrl.trim()) }.getOrNull() ?: return false
            if (!url.protocol.equals("https", ignoreCase = true)) return false
            if (!url.userInfo.isNullOrBlank()) return false
            val host = url.host?.lowercase(Locale.US)?.trimEnd('.') ?: return false
            if (host.isBlank() || !host.contains('.')) return false
            if (host == "localhost" || host.endsWith(".local") || host.endsWith(".internal")) return false
            if (host == "0.0.0.0" || host == "::1" || host.startsWith("127.") ||
                host.startsWith("10.") || host.startsWith("192.168.") || host.startsWith("169.254.")
            ) {
                return false
            }
            if (host.startsWith("172.")) {
                val secondOctet = host.removePrefix("172.").substringBefore('.').toIntOrNull()
                if (secondOctet != null && secondOctet in 16..31) return false
            }
            return true
        }

        suspend fun stagePickedUriToCache(
            context: Context,
            uri: Uri,
            typeHint: String,
            maxBytes: Long
        ): StagedPickedFile = withContext(Dispatchers.IO) {
            val appCtx = context.applicationContext
            val resolver = appCtx.contentResolver
            var displayName = ""
            runCatching {
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) displayName = c.getString(0).orEmpty()
                }
            }
            val mime = resolver.getType(uri)
            val fromMime = mime?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            val ext = when {
                !fromMime.isNullOrBlank() -> fromMime.lowercase(Locale.US)
                displayName.contains('.') -> displayName.substringAfterLast('.').lowercase(Locale.US).take(5)
                else -> inferExtension(uri.toString(), typeHint)
            }
            val uploadDir = File(appCtx.cacheDir, "picked_uploads").apply { mkdirs() }
            val target = File(uploadDir, "pick_${UUID.randomUUID().toString().take(8)}.$ext")
            val input = try {
                resolver.openInputStream(uri)
            } catch (sec: SecurityException) {
                throw LocalUriUnavailableException("Permission denied reading picked URI: $uri", sec)
            } catch (e: Exception) {
                throw LocalUriUnavailableException("Cannot open picked URI: $uri", e)
            } ?: throw LocalUriUnavailableException("Empty input stream for picked URI")

            var totalBytes = 0L
            try {
                input.use { stream ->
                    FileOutputStream(target).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        while (true) {
                            val read = stream.read(buffer)
                            if (read == -1) break
                            totalBytes += read
                            if (totalBytes > maxBytes) {
                                throw PickedFileTooLargeException(totalBytes, maxBytes)
                            }
                            output.write(buffer, 0, read)
                        }
                        output.flush()
                    }
                }
            } catch (e: Exception) {
                runCatching { target.delete() }
                throw e
            }
            StagedPickedFile(
                localFileUri = Uri.fromFile(target).toString(),
                displayName = displayName.ifBlank { target.name },
                sizeBytes = totalBytes
            )
        }

        fun buildSafeDownloadFileName(title: String, fallbackId: String, ext: String): String {
            val cleanBase = title.trim()
                .replace(Regex("[^a-zA-Z0-9._-]"), "_")
                .replace(Regex("_+"), "_")
                .trim('_')
                .take(48)
                .ifBlank { fallbackId.take(32).ifBlank { "bioterre_file" } }
            val cleanExt = ext.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "").ifBlank { "bin" }
            return if (cleanBase.endsWith(".$cleanExt", ignoreCase = true)) cleanBase else "$cleanBase.$cleanExt"
        }

        fun formatBytesReadable(bytes: Long): String {
            return when {
                bytes >= 1024L * 1024L -> "%.1f Mo".format(Locale.US, bytes / (1024.0 * 1024.0))
                bytes >= 1024L -> "${bytes / 1024L} Ko"
                else -> "$bytes o"
            }
        }

        fun parseChunkResourceId(fileUrl: String): String? {
            val trimmed = fileUrl.trim()
            val prefix = "$FIRESTORE_CHUNK_SCHEME://$FIRESTORE_CHUNK_HOST/"
            if (!trimmed.startsWith(prefix, ignoreCase = true)) return null
            val remainder = trimmed.substring(prefix.length)
            val idPart = remainder.substringBefore('?').substringBefore('/').trim()
            return idPart.takeIf { CHUNK_ID_REGEX.matches(it) }
        }

        fun normalizeDownloadUrl(rawUrl: String): String {
            val trimmed = rawUrl.trim()
            val fileMatch = Regex("""https?://drive\.google\.com/file/d/([a-zA-Z0-9_-]+)""").find(trimmed)
            if (fileMatch != null) {
                return "https://drive.google.com/uc?export=download&id=${fileMatch.groupValues[1]}"
            }
            val openMatch = Regex("""https?://drive\.google\.com/(?:open|uc)\?[^#]*\bid=([a-zA-Z0-9_-]+)""").find(trimmed)
            if (openMatch != null && !trimmed.contains("export=download")) {
                return "https://drive.google.com/uc?export=download&id=${openMatch.groupValues[1]}"
            }
            return trimmed
        }

        fun inferExtension(urlString: String, type: String): String {
            val extQuery = Regex("""[?&]ext=([a-zA-Z0-9]{1,6})""").find(urlString)?.groupValues?.getOrNull(1)
            if (!extQuery.isNullOrBlank()) return extQuery.lowercase(Locale.US)
            val pathNoQuery = urlString.substringBefore('?').substringBefore('#')
            val dotIdx = pathNoQuery.lastIndexOf('.')
            val slashIdx = pathNoQuery.lastIndexOf('/')
            if (dotIdx > slashIdx && dotIdx >= 0 && dotIdx < pathNoQuery.length - 1) {
                val candidate = pathNoQuery.substring(dotIdx + 1).lowercase(Locale.US)
                if (candidate.length in 1..5 && candidate.all { it.isLetterOrDigit() }) {
                    return candidate
                }
            }
            val fromUrl = runCatching { MimeTypeMap.getFileExtensionFromUrl(urlString) }.getOrNull().orEmpty().lowercase(Locale.US)
            if (fromUrl.isNotBlank() && fromUrl.length <= 5) return fromUrl
            return when (type) {
                "pdf" -> "pdf"
                "word" -> "docx"
                "image" -> "jpg"
                "video" -> "mp4"
                else -> "bin"
            }
        }

        fun mimeTypeForResource(type: String, pathOrUrl: String): String {
            val ext = inferExtension(pathOrUrl, type)
            return when (ext) {
                "pdf" -> "application/pdf"
                "doc" -> "application/msword"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "webp" -> "image/webp"
                "mp4" -> "video/mp4"
                "mkv" -> "video/x-matroska"
                "webm" -> "video/webm"
                "3gp" -> "video/3gpp"
                else -> when (type) {
                    "pdf" -> "application/pdf"
                    "word" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    "image" -> "image/*"
                    "video" -> "video/*"
                    else -> "*/*"
                }
            }
        }
    }

    // ---------------------------------------------------------
    // JSON Serialization Helpers (zero reflection / zero extra deps)
    // ---------------------------------------------------------
    private fun readDownloadedResources(): Map<String, DownloadedResource> {
        if (!resourcesIndexFile.exists()) return emptyMap()
        return try {
            val arr = JSONArray(resourcesIndexFile.readText())
            val map = linkedMapOf<String, DownloadedResource>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val path = obj.optString("localFilePath")
                val file = File(path)
                if (path.isNotBlank() && isFileInsideDir(file, resourcesFilesDir) && file.exists()) {
                    val rec = DownloadedResource(
                        resourceId = obj.getString("resourceId"),
                        classId = obj.optString("classId"),
                        teacherId = obj.optString("teacherId"),
                        title = obj.optString("title"),
                        description = obj.optString("description"),
                        type = obj.optString("type", "pdf"),
                        remoteUrl = obj.optString("remoteUrl"),
                        localFilePath = path,
                        fileSizeBytes = obj.optLong("fileSizeBytes", 0L),
                        durationSeconds = obj.optLong("durationSeconds", 0L),
                        downloadedAtMillis = obj.optLong("downloadedAtMillis", 0L),
                        publicDisplayPath = obj.optString("publicDisplayPath"),
                        publicDownloadUri = obj.optString("publicDownloadUri")
                    )
                    map[rec.resourceId] = rec
                }
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun writeDownloadedResources(map: Map<String, DownloadedResource>) {
        val arr = JSONArray()
        map.values.forEach { rec ->
            arr.put(
                JSONObject().apply {
                    put("resourceId", rec.resourceId)
                    put("classId", rec.classId)
                    put("teacherId", rec.teacherId)
                    put("title", rec.title)
                    put("description", rec.description)
                    put("type", rec.type)
                    put("remoteUrl", rec.remoteUrl)
                    put("localFilePath", rec.localFilePath)
                    put("fileSizeBytes", rec.fileSizeBytes)
                    put("durationSeconds", rec.durationSeconds)
                    put("downloadedAtMillis", rec.downloadedAtMillis)
                    put("publicDisplayPath", rec.publicDisplayPath)
                    put("publicDownloadUri", rec.publicDownloadUri)
                }
            )
        }
        resourcesIndexFile.writeText(arr.toString())
    }

    private fun readDownloadedQuizzes(): Map<String, Quiz> {
        if (!quizzesIndexFile.exists()) return emptyMap()
        return try {
            val arr = JSONArray(quizzesIndexFile.readText())
            val map = linkedMapOf<String, Quiz>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val quiz = quizFromJson(obj)
                map[quiz.id] = quiz
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun writeDownloadedQuizzes(map: Map<String, Quiz>) {
        val arr = JSONArray()
        map.values.forEach { quiz ->
            arr.put(quizToJson(quiz))
        }
        quizzesIndexFile.writeText(arr.toString())
    }

    private fun readPendingAttempts(): List<PendingQuizAttempt> {
        if (!pendingAttemptsFile.exists()) return emptyList()
        return try {
            val arr = JSONArray(pendingAttemptsFile.readText())
            val list = mutableListOf<PendingQuizAttempt>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val ansArr = obj.getJSONArray("answers")
                val answers = mutableListOf<QuestionAnswerRecord>()
                for (j in 0 until ansArr.length()) {
                    val a = ansArr.getJSONObject(j)
                    answers.add(
                        QuestionAnswerRecord(
                            questionIndex = a.getInt("questionIndex"),
                            selectedIndex = a.getInt("selectedIndex"),
                            isCorrect = a.getBoolean("isCorrect"),
                            timeSpentSeconds = a.getLong("timeSpentSeconds")
                        )
                    )
                }
                list.add(
                    PendingQuizAttempt(
                        attemptId = obj.getString("attemptId"),
                        quizId = obj.getString("quizId"),
                        quizTitle = obj.optString("quizTitle"),
                        classId = obj.optString("classId"),
                        teacherId = obj.optString("teacherId"),
                        studentId = obj.optString("studentId"),
                        studentName = obj.optString("studentName"),
                        score = obj.getDouble("score"),
                        maxScore = obj.getDouble("maxScore"),
                        totalDurationSeconds = obj.getLong("totalDurationSeconds"),
                        answers = answers,
                        createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun writePendingAttempts(list: List<PendingQuizAttempt>) {
        val arr = JSONArray()
        list.forEach { att ->
            val ansArr = JSONArray()
            att.answers.forEach { rec ->
                ansArr.put(
                    JSONObject().apply {
                        put("questionIndex", rec.questionIndex)
                        put("selectedIndex", rec.selectedIndex)
                        put("isCorrect", rec.isCorrect)
                        put("timeSpentSeconds", rec.timeSpentSeconds)
                    }
                )
            }
            arr.put(
                JSONObject().apply {
                    put("attemptId", att.attemptId)
                    put("quizId", att.quizId)
                    put("quizTitle", att.quizTitle)
                    put("classId", att.classId)
                    put("teacherId", att.teacherId)
                    put("studentId", att.studentId)
                    put("studentName", att.studentName)
                    put("score", att.score)
                    put("maxScore", att.maxScore)
                    put("totalDurationSeconds", att.totalDurationSeconds)
                    put("answers", ansArr)
                    put("createdAtMillis", att.createdAtMillis)
                }
            )
        }
        pendingAttemptsFile.writeText(arr.toString())
    }

    private fun readPendingVideoTrackings(): List<PendingVideoTracking> {
        if (!pendingVideoFile.exists()) return emptyList()
        return try {
            val arr = JSONArray(pendingVideoFile.readText())
            val list = mutableListOf<PendingVideoTracking>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val resObj = obj.getJSONObject("resource")
                val res = CourseResource(
                    id = resObj.getString("id"),
                    classId = resObj.optString("classId"),
                    teacherId = resObj.optString("teacherId"),
                    title = resObj.optString("title"),
                    description = resObj.optString("description"),
                    type = resObj.optString("type", "video"),
                    fileUrl = resObj.optString("fileUrl"),
                    durationSeconds = resObj.optLong("durationSeconds", 0L),
                    isDownloadable = resObj.optBoolean("isDownloadable", true)
                )
                list.add(
                    PendingVideoTracking(
                        resource = res,
                        studentName = obj.optString("studentName"),
                        maxPositionSeconds = obj.optLong("maxPositionSeconds", 0L),
                        totalDurationSeconds = obj.optLong("totalDurationSeconds", 0L),
                        updatedAtMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis()),
                        watchedPercentageOverride = obj.optDouble("watchedPercentageOverride", 0.0)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun writePendingVideoTrackings(list: List<PendingVideoTracking>) {
        val arr = JSONArray()
        list.forEach { tr ->
            arr.put(
                JSONObject().apply {
                    put(
                        "resource",
                        JSONObject().apply {
                            put("id", tr.resource.id)
                            put("classId", tr.resource.classId)
                            put("teacherId", tr.resource.teacherId)
                            put("title", tr.resource.title)
                            put("description", tr.resource.description)
                            put("type", tr.resource.type)
                            put("fileUrl", tr.resource.fileUrl)
                            put("durationSeconds", tr.resource.durationSeconds)
                            put("isDownloadable", tr.resource.isDownloadable)
                        }
                    )
                    put("studentName", tr.studentName)
                    put("maxPositionSeconds", tr.maxPositionSeconds)
                    put("totalDurationSeconds", tr.totalDurationSeconds)
                    put("updatedAtMillis", tr.updatedAtMillis)
                    put("watchedPercentageOverride", tr.watchedPercentageOverride)
                }
            )
        }
        pendingVideoFile.writeText(arr.toString())
    }

    private fun quizToJson(quiz: Quiz): JSONObject {
        val qArr = JSONArray()
        quiz.questions.forEach { q ->
            val optsArr = JSONArray()
            q.options.forEach { optsArr.put(it) }
            qArr.put(
                JSONObject().apply {
                    put("questionText", q.questionText)
                    put("options", optsArr)
                    put("correctIndex", q.correctIndex)
                    put("explanation", q.explanation)
                    if (q.imageUrl.isNotBlank()) {
                        put("imageUrl", q.imageUrl)
                    }
                }
            )
        }
        return JSONObject().apply {
            put("id", quiz.id)
            put("classId", quiz.classId)
            put("teacherId", quiz.teacherId)
            put("title", quiz.title)
            put("description", quiz.description)
            put("chapterSvt", quiz.chapterSvt)
            put("isDownloadable", quiz.isDownloadable)
            put("questions", qArr)
        }
    }

    private fun quizFromJson(obj: JSONObject): Quiz {
        val qArr = obj.getJSONArray("questions")
        val questions = mutableListOf<QuizQuestion>()
        for (i in 0 until qArr.length()) {
            val qObj = qArr.getJSONObject(i)
            val optsArr = qObj.getJSONArray("options")
            val opts = mutableListOf<String>()
            for (j in 0 until optsArr.length()) {
                opts.add(optsArr.getString(j))
            }
            questions.add(
                QuizQuestion(
                    questionText = qObj.getString("questionText"),
                    options = opts,
                    correctIndex = qObj.getInt("correctIndex"),
                    explanation = qObj.optString("explanation"),
                    imageUrl = qObj.optString("imageUrl", "")
                )
            )
        }
        return Quiz(
            id = obj.getString("id"),
            classId = obj.optString("classId"),
            teacherId = obj.optString("teacherId"),
            title = obj.optString("title"),
            description = obj.optString("description"),
            chapterSvt = obj.optString("chapterSvt"),
            questions = questions,
            isPublished = true,
            isDownloadable = obj.optBoolean("isDownloadable", true)
        )
    }
}
