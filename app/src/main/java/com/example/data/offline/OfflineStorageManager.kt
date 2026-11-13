package com.example.data.offline

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.example.data.model.CourseResource
import com.example.data.model.QuestionAnswerRecord
import com.example.data.model.Quiz
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizQuestion
import com.example.data.model.VideoTracking
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Date

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
    val downloadedAtMillis: Long
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
    val updatedAtMillis: Long
) {
    fun toVideoTracking(studentId: String): VideoTracking {
        val safeDur = totalDurationSeconds.coerceAtLeast(resource.durationSeconds).coerceAtLeast(1L)
        val pct = ((maxPositionSeconds.toDouble() / safeDur.toDouble()) * 100.0).coerceIn(0.0, 100.0)
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
            maxPositionSeconds = maxPositionSeconds,
            durationSeconds = safeDur,
            watchedPercentage = pct,
            status = status
        )
    }
}

class OfflineStorageManager(
    context: Context,
    private val userId: String
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

    suspend fun downloadResource(resource: CourseResource): DownloadedResource = withContext(Dispatchers.IO) {
        val safeResId = resource.id.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "res_${System.currentTimeMillis()}" }
        val ext = inferExtension(resource.fileUrl, resource.type)
        val targetFile = File(resourcesFilesDir, "$safeResId.$ext")
        val tempFile = File(resourcesFilesDir, "$safeResId.$ext.tmp")

        _downloadingProgress.value = _downloadingProgress.value + (resource.id to 0.05f)
        try {
            val uri = Uri.parse(resource.fileUrl)
            when (uri.scheme?.lowercase()) {
                "http", "https" -> {
                    downloadHttpFile(resource.fileUrl, tempFile) { fraction ->
                        _downloadingProgress.value = _downloadingProgress.value + (resource.id to fraction.coerceIn(0.05f, 0.99f))
                    }
                }
                "content", "file" -> {
                    copyUriFile(uri, tempFile) { fraction ->
                        _downloadingProgress.value = _downloadingProgress.value + (resource.id to fraction.coerceIn(0.05f, 0.99f))
                    }
                }
                else -> {
                    throw IllegalArgumentException("Unsupported URI scheme for offline download: ${resource.fileUrl}")
                }
            }

            if (targetFile.exists()) targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
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
                downloadedAtMillis = System.currentTimeMillis()
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
                val updated = _downloadedResources.value - resourceId
                writeDownloadedResources(updated)
                _downloadedResources.value = updated
            }
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
            val mergedDur = maxOf(existing?.totalDurationSeconds ?: 0L, tracking.totalDurationSeconds)
            val merged = tracking.copy(
                maxPositionSeconds = mergedPos,
                totalDurationSeconds = mergedDur
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
                val url = URL(currentUrl)
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = true
                    requestMethod = "GET"
                }
                val code = conn.responseCode
                if (code in 300..399) {
                    val loc = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (loc.isNullOrBlank()) throw IllegalStateException("Redirect without location ($code)")
                    currentUrl = loc
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
            conn.inputStream.use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var downloaded = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloaded += read
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

    private fun copyUriFile(uri: Uri, target: File, onProgress: (Float) -> Unit) {
        val input = appContext.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open local file URI for offline caching")
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

    private fun inferExtension(urlString: String, type: String): String {
        val fromUrl = MimeTypeMap.getFileExtensionFromUrl(urlString).orEmpty().lowercase()
        if (fromUrl.isNotBlank() && fromUrl.length <= 5) return fromUrl
        return when (type) {
            "pdf" -> "pdf"
            "word" -> "docx"
            "image" -> "jpg"
            "video" -> "mp4"
            else -> "bin"
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
                if (path.isNotBlank() && File(path).exists()) {
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
                        downloadedAtMillis = obj.optLong("downloadedAtMillis", 0L)
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
                        updatedAtMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis())
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
                    explanation = qObj.optString("explanation")
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
