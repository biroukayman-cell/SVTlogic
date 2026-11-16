package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.badges.BadgeEngine
import com.example.data.badges.SvtBadgeDefinition
import com.example.data.model.Announcement
import com.example.data.model.AppUpdateRelease
import com.example.data.model.ClassCode
import com.example.data.model.CourseResource
import com.example.data.model.Enrollment
import com.example.data.model.PrivateMessage
import com.example.data.model.QuestionAnswerRecord
import com.example.data.model.Quiz
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizQuestion
import com.example.data.model.RosterStudent
import com.example.data.model.StudentNotification
import com.example.data.model.SvtClass
import com.example.data.model.UserProfile
import com.example.data.model.VideoTracking
import com.example.data.offline.OfflineStorageManager
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ResourceUploadTooLargeException(
    val sizeBytes: Long,
    val maxBytes: Long
) : IllegalStateException("File size $sizeBytes exceeds max allowed $maxBytes bytes")

class ResourceReadException(
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause)

class SvtRepository(private val db: FirebaseFirestore) {

    private val videoTrackingMutex = Mutex()

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    private fun <T> Flow<T>.logFirestoreErrors(op: OperationType, path: String, uid: String): Flow<T> {
        return this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val toLog = firestoreError ?: (error as? Exception)
            if (toLog != null && auth.currentUser?.uid == uid) {
                handleFirestoreError(toLog, op, path)
            }
            throw error
        }
    }

    private fun <T> Flow<T>.retryTransientFirestoreErrors(): Flow<T> {
        return this.retryWhen { cause, attempt ->
            val fsErr = generateSequence(cause) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            val code = fsErr?.code
            val isTransient = code == FirebaseFirestoreException.Code.UNAVAILABLE ||
                code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ||
                code == FirebaseFirestoreException.Code.ABORTED ||
                (code == FirebaseFirestoreException.Code.PERMISSION_DENIED && attempt < 2L)
            if (isTransient && attempt < 4L) {
                delay(650L * (attempt + 1L))
                true
            } else {
                false
            }
        }
    }

    // ---------------------------------------------------------
    // 1. User Profile & Streak
    // ---------------------------------------------------------
    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .retryTransientFirestoreErrors()
            .logFirestoreErrors(OperationType.GET, path, userId)
            .collect { emit(it) }
    }

    suspend fun createUserProfile(
        fullName: String,
        phoneNumber: String,
        role: String,
        avatarUrl: String? = null,
        massarNumber: String = "",
        birthDate: String = "",
        email: String = "",
        mustChangePassword: Boolean = false,
        boundDeviceId: String = "",
        boundDeviceModel: String = ""
    ): UserProfile {
        val uid = requireUserId()
        val today = todayDateString()
        val docRef = db.collection("users").document(uid)
        val cleanMassar = massarNumber.trim()
        val cleanBirthDate = birthDate.trim()
        val cleanEmail = email.trim()
        val acceptedTerms = role != "student"
        val cleanDeviceId = if (role == "student") boundDeviceId.trim() else ""
        val cleanDeviceModel = if (role == "student") boundDeviceModel.trim().take(96) else ""
        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "fullName" to fullName.trim(),
            "phoneNumber" to phoneNumber.trim(),
            "role" to role,
            "massarNumber" to cleanMassar,
            "birthDate" to cleanBirthDate,
            "email" to cleanEmail,
            "mustChangePassword" to mustChangePassword,
            "acceptedTerms" to acceptedTerms,
            "xp" to 0L,
            "loginStreak" to 1L,
            "lastLoginDate" to today,
            "unlockedBadgeIds" to emptyList<String>(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!avatarUrl.isNullOrBlank()) {
            payload["avatarUrl"] = avatarUrl
        }
        if (cleanDeviceId.isNotBlank()) {
            payload["boundDeviceId"] = cleanDeviceId
            payload["boundDeviceModel"] = cleanDeviceModel
        }
        try {
            docRef.set(payload).await()
            return UserProfile(
                id = uid,
                userId = uid,
                fullName = fullName.trim(),
                phoneNumber = phoneNumber.trim(),
                role = role,
                massarNumber = cleanMassar,
                birthDate = cleanBirthDate,
                email = cleanEmail,
                mustChangePassword = mustChangePassword,
                acceptedTerms = acceptedTerms,
                avatarUrl = avatarUrl,
                xp = 0L,
                loginStreak = 1L,
                lastLoginDate = today,
                unlockedBadgeIds = emptyList(),
                boundDeviceId = cleanDeviceId,
                boundDeviceModel = cleanDeviceModel
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun bindStudentDeviceIfUnbound(deviceId: String, deviceModel: String): Boolean {
        val uid = requireUserId()
        val cleanId = deviceId.trim()
        val cleanModel = deviceModel.trim().take(96)
        require(cleanId.isNotBlank()) { "Device ID cannot be blank" }
        val docRef = db.collection("users").document(uid)
        return try {
            val snap = runCatching { docRef.get().await() }.getOrNull()
            val existingId = snap?.getString("boundDeviceId").orEmpty().trim()
            if (existingId.isNotBlank()) {
                return existingId == cleanId
            }
            docRef.update(
                mapOf(
                    "boundDeviceId" to cleanId,
                    "boundDeviceModel" to cleanModel,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun resetStudentBoundDevice(studentId: String) {
        requireUserId()
        require(studentId.isNotBlank()) { "Student ID cannot be blank" }
        val docRef = db.collection("users").document(studentId)
        try {
            docRef.update(
                mapOf(
                    "boundDeviceId" to "",
                    "boundDeviceModel" to "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateProfileInfo(
        fullName: String,
        phoneNumber: String,
        avatarUrl: String?,
        birthDate: String? = null,
        mustChangePassword: Boolean? = null
    ) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val updates = mutableMapOf<String, Any>(
            "fullName" to fullName.trim(),
            "phoneNumber" to phoneNumber.trim(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!avatarUrl.isNullOrBlank()) {
            updates["avatarUrl"] = avatarUrl
        }
        if (birthDate != null) {
            updates["birthDate"] = birthDate.trim()
        }
        if (mustChangePassword != null) {
            updates["mustChangePassword"] = mustChangePassword
        }
        try {
            docRef.update(updates).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun markPasswordChanged() {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            docRef.update(
                mapOf(
                    "mustChangePassword" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun acceptTermsOfUse() {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            docRef.update(
                mapOf(
                    "acceptedTerms" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun refreshLoginStreakAndEvaluateBadges(
        profile: UserProfile,
        attempts: List<QuizAttempt>,
        videoTrackings: List<VideoTracking>,
        teacherId: String
    ): List<SvtBadgeDefinition> {
        val uid = requireUserId()
        if (profile.userId != uid) return emptyList()

        val today = todayDateString()
        val yesterday = yesterdayDateString()
        var newStreak = profile.loginStreak
        var streakChanged = false

        if (profile.lastLoginDate != today) {
            newStreak = if (profile.lastLoginDate == yesterday) {
                profile.loginStreak + 1L
            } else {
                1L
            }
            streakChanged = true
        }

        val updatedProfileForEval = profile.copy(
            loginStreak = newStreak,
            lastLoginDate = today
        )

        val newlyUnlocked = if (profile.role == "student") {
            BadgeEngine.evaluateUnlockedBadges(updatedProfileForEval, attempts, videoTrackings)
        } else {
            emptyList()
        }

        if (streakChanged || newlyUnlocked.isNotEmpty()) {
            val docRef = db.collection("users").document(uid)
            val addedXp = newlyUnlocked.sumOf { it.xpReward }
            val mergedBadges = (profile.unlockedBadgeIds + newlyUnlocked.map { it.id }).distinct()
            val updates = mapOf(
                "loginStreak" to newStreak,
                "lastLoginDate" to today,
                "xp" to (profile.xp + addedXp),
                "unlockedBadgeIds" to mergedBadges,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                docRef.update(updates).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, docRef.path)
                throw e
            }
        }
        return newlyUnlocked
    }

    // ---------------------------------------------------------
    // 2. Classes & QR Codes (Teacher + Student Lookup)
    // ---------------------------------------------------------
    fun observeTeacherClasses(teacherId: String): Flow<List<SvtClass>> = flow {
        val path = "classes"
        db.collection("classes")
            .whereEqualTo("teacherId", teacherId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(SvtClass::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .logFirestoreErrors(OperationType.LIST, path, teacherId)
            .collect { emit(it) }
    }

    fun observeClassById(classId: String): Flow<SvtClass?> = flow {
        val path = "classes/$classId"
        db.collection("classes").document(classId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(SvtClass::class.java, ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .retryTransientFirestoreErrors()
            .logFirestoreErrors(OperationType.GET, path, requireUserId())
            .collect { emit(it) }
    }

    suspend fun createClass(
        teacherName: String,
        name: String,
        level: String,
        academicYear: String
    ): SvtClass {
        val uid = requireUserId()
        val classId = "cls_${UUID.randomUUID().toString().take(8)}"
        val qrToken = "SVT-${UUID.randomUUID().toString().replace("-", "").take(8).uppercase(Locale.US)}"
        val classRef = db.collection("classes").document(classId)
        val codeRef = db.collection("class_codes").document(qrToken)

        val classPayload = mapOf(
            "teacherId" to uid,
            "teacherName" to teacherName.trim(),
            "name" to name.trim(),
            "academicYear" to academicYear.trim(),
            "level" to level.trim(),
            "qrToken" to qrToken,
            "visibleLabDomains" to SvtClass.DEFAULT_LAB_DOMAINS,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val codePayload = mapOf(
            "classId" to classId,
            "className" to name.trim(),
            "academicYear" to academicYear.trim(),
            "teacherId" to uid,
            "teacherName" to teacherName.trim(),
            "createdAt" to FieldValue.serverTimestamp()
        )

        try {
            val batch = db.batch()
            batch.set(classRef, classPayload)
            batch.set(codeRef, codePayload)
            batch.commit().await()
            return SvtClass(
                id = classId,
                teacherId = uid,
                teacherName = teacherName.trim(),
                name = name.trim(),
                academicYear = academicYear.trim(),
                level = level.trim(),
                qrToken = qrToken,
                visibleLabDomains = SvtClass.DEFAULT_LAB_DOMAINS
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, classRef.path)
            throw e
        }
    }

    suspend fun updateClassLabDomains(classId: String, domains: List<String>) {
        requireUserId()
        val sanitized = domains.map { it.trim() }.filter { it in SvtClass.DEFAULT_LAB_DOMAINS }.distinct()
        require(sanitized.isNotEmpty()) { "At least one Labo SVT domain must remain selected." }
        val docRef = db.collection("classes").document(classId)
        try {
            docRef.update(
                mapOf(
                    "visibleLabDomains" to sanitized,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun lookupClassByQrToken(qrToken: String): ClassCode? {
        requireUserId()
        val clean = qrToken.trim()
        if (clean.isEmpty()) return null
        val candidates = listOf(clean, clean.uppercase(Locale.US)).distinct()
        var lastPath = "class_codes/$clean"
        return try {
            for (candidate in candidates) {
                val docRef = db.collection("class_codes").document(candidate)
                lastPath = docRef.path
                val snap = docRef.get().await()
                if (snap.exists()) {
                    return snap.toObject(ClassCode::class.java, ServerTimestampBehavior.ESTIMATE)
                }
            }
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, lastPath)
            null
        }
    }

    suspend fun deleteClassCascade(svtClass: SvtClass) {
        val uid = requireUserId()
        val classId = svtClass.id
        require(classId.isNotBlank()) { "Class ID cannot be blank" }
        try {
            val docRefsToDelete = mutableListOf(
                db.collection("classes").document(classId),
                db.collection("class_codes").document(svtClass.qrToken)
            )
            val collectionsByClass = listOf(
                "enrollments",
                "class_rosters",
                "quizzes",
                "quiz_attempts",
                "resources",
                "resource_chunks",
                "video_tracking",
                "announcements",
                "private_messages",
                "app_updates"
            )
            for (col in collectionsByClass) {
                val snaps = db.collection(col)
                    .whereEqualTo("teacherId", uid)
                    .whereEqualTo("classId", classId)
                    .get()
                    .await()
                docRefsToDelete.addAll(snaps.documents.map { it.reference })
            }

            docRefsToDelete.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { batch.delete(it) }
                batch.commit().await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "classes/$classId")
            throw e
        }
    }

    // ---------------------------------------------------------
    // 3. Enrollments (QR Join -> Teacher Validation -> Cascade Delete)
    // ---------------------------------------------------------
    fun observeStudentEnrollment(studentId: String): Flow<Enrollment?> = flow {
        val path = "enrollments/$studentId"
        db.collection("enrollments").document(studentId)
            .snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(Enrollment::class.java, ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .retryTransientFirestoreErrors()
            .logFirestoreErrors(OperationType.GET, path, studentId)
            .collect { emit(it) }
    }

    fun observeTeacherEnrollments(teacherId: String): Flow<List<Enrollment>> = flow {
        val path = "enrollments"
        db.collection("enrollments")
            .whereEqualTo("teacherId", teacherId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Enrollment::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.requestedAt ?: Timestamp(0, 0) }
            }
            .logFirestoreErrors(OperationType.LIST, path, teacherId)
            .collect { emit(it) }
    }

    suspend fun requestEnrollment(
        classCode: ClassCode,
        studentName: String,
        studentPhone: String,
        massarNumber: String = "",
        qrToken: String = ""
    ): Enrollment {
        val uid = requireUserId()
        val docRef = db.collection("enrollments").document(uid)
        val existing = runCatching { docRef.get().await() }.getOrNull()
        val resolvedToken = qrToken.trim().ifBlank { classCode.id.trim() }
        val status = if (resolvedToken.isNotBlank()) "approved" else "pending"
        val cleanName = studentName.trim().ifBlank { "Élève SVT" }
        val cleanPhone = studentPhone.trim()
        val cleanMassar = massarNumber.trim()
        val payload = mutableMapOf<String, Any>(
            "studentId" to uid,
            "classId" to classCode.classId,
            "className" to classCode.className,
            "teacherId" to classCode.teacherId,
            "teacherName" to classCode.teacherName,
            "studentName" to cleanName,
            "studentPhone" to cleanPhone,
            "massarNumber" to cleanMassar,
            "status" to status,
            "requestedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (resolvedToken.isNotBlank()) {
            payload["qrToken"] = resolvedToken
            payload["validatedAt"] = FieldValue.serverTimestamp()
        }
        try {
            if (existing != null && existing.exists()) {
                docRef.delete().await()
            }
            docRef.set(payload).await()
            return Enrollment(
                id = uid,
                studentId = uid,
                classId = classCode.classId,
                className = classCode.className,
                teacherId = classCode.teacherId,
                teacherName = classCode.teacherName,
                studentName = cleanName,
                studentPhone = cleanPhone,
                massarNumber = cleanMassar,
                qrToken = resolvedToken,
                status = status
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun clearOwnEnrollment() {
        val uid = requireUserId()
        val docRef = db.collection("enrollments").document(uid)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun approveStudentEnrollment(
        enrollment: Enrollment,
        notifTitle: String,
        notifBody: String
    ) {
        val uid = requireUserId()
        val enrollRef = db.collection("enrollments").document(enrollment.studentId)
        try {
            val batch = db.batch()
            batch.update(
                enrollRef,
                mapOf(
                    "status" to "approved",
                    "validatedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // Sync approvedStudentIds onto existing quizzes, resources, and announcements of the class
            val classScopedCollections = listOf("quizzes", "resources", "announcements")
            for (col in classScopedCollections) {
                val docs = db.collection(col)
                    .whereEqualTo("teacherId", uid)
                    .whereEqualTo("classId", enrollment.classId)
                    .get()
                    .await()
                for (doc in docs.documents) {
                    batch.update(
                        doc.reference,
                        mapOf(
                            "approvedStudentIds" to FieldValue.arrayUnion(enrollment.studentId),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            val notifRef = db.collection("student_notifications").document()
            batch.set(
                notifRef,
                mapOf(
                    "studentId" to enrollment.studentId,
                    "teacherId" to uid,
                    "type" to "enrollment",
                    "title" to notifTitle,
                    "body" to notifBody,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, enrollRef.path)
            throw e
        }
    }

    suspend fun rejectStudentEnrollment(enrollment: Enrollment) {
        requireUserId()
        val enrollRef = db.collection("enrollments").document(enrollment.studentId)
        try {
            enrollRef.update(
                mapOf(
                    "status" to "rejected",
                    "validatedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, enrollRef.path)
            throw e
        }
    }

    suspend fun deleteStudentFromClass(enrollment: Enrollment) {
        val uid = requireUserId()
        val studentId = enrollment.studentId
        try {
            val batch = db.batch()
            batch.delete(db.collection("enrollments").document(studentId))

            val classScopedCollections = listOf("quizzes", "resources", "announcements")
            for (col in classScopedCollections) {
                val docs = db.collection(col)
                    .whereEqualTo("teacherId", uid)
                    .whereEqualTo("classId", enrollment.classId)
                    .get()
                    .await()
                for (doc in docs.documents) {
                    batch.update(
                        doc.reference,
                        mapOf(
                            "approvedStudentIds" to FieldValue.arrayRemove(studentId),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            val studentOwnedCollections = listOf(
                "quiz_attempts",
                "video_tracking",
                "private_messages"
            )
            for (col in studentOwnedCollections) {
                val docs = db.collection(col)
                    .whereEqualTo("teacherId", uid)
                    .whereEqualTo("studentId", studentId)
                    .get()
                    .await()
                for (doc in docs.documents) {
                    batch.delete(doc.reference)
                }
            }

            val rosterDocs = db.collection("class_rosters")
                .whereEqualTo("teacherId", uid)
                .whereEqualTo("classId", enrollment.classId)
                .whereEqualTo("studentUid", studentId)
                .get()
                .await()
            for (doc in rosterDocs.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "enrollments/$studentId")
            throw e
        }
    }

    // ---------------------------------------------------------
    // 4. Quizzes & Timed Attempts
    // ---------------------------------------------------------
    fun observeTeacherQuizzes(teacherId: String): Flow<List<Quiz>> = flow {
        db.collection("quizzes")
            .whereEqualTo("teacherId", teacherId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Quiz::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .logFirestoreErrors(OperationType.LIST, "quizzes", teacherId)
            .collect { emit(it) }
    }

    fun observeStudentQuizzes(classId: String): Flow<List<Quiz>> = flow {
        db.collection("quizzes")
            .whereEqualTo("classId", classId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Quiz::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .logFirestoreErrors(OperationType.LIST, "quizzes", classId)
            .collect { emit(it) }
    }

    fun observeTeacherAttempts(teacherId: String): Flow<List<QuizAttempt>> = flow {
        db.collection("quiz_attempts")
            .whereEqualTo("teacherId", teacherId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(QuizAttempt::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.completedAt ?: Timestamp(0, 0) }
            }
            .logFirestoreErrors(OperationType.LIST, "quiz_attempts", teacherId)
            .collect { emit(it) }
    }

    fun observeStudentAttempts(studentId: String): Flow<List<QuizAttempt>> = flow {
        db.collection("quiz_attempts")
            .whereEqualTo("studentId", studentId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(QuizAttempt::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.completedAt ?: Timestamp(0, 0) }
            }
            .logFirestoreErrors(OperationType.LIST, "quiz_attempts", studentId)
            .collect { emit(it) }
    }

    suspend fun createQuizForClass(
        classId: String,
        title: String,
        description: String,
        chapterSvt: String,
        questions: List<QuizQuestion>,
        approvedStudentIds: List<String>,
        notifTitle: String,
        notifBody: String,
        isDownloadable: Boolean = false
    ): String {
        val uid = requireUserId()
        val quizRef = db.collection("quizzes").document()
        val questionMaps = questions.map { q ->
            mapOf(
                "questionText" to q.questionText.trim(),
                "options" to q.options.map { it.trim() },
                "correctIndex" to q.correctIndex,
                "explanation" to q.explanation.trim()
            )
        }
        val payload = mapOf(
            "classId" to classId,
            "teacherId" to uid,
            "title" to title.trim(),
            "description" to description.trim(),
            "chapterSvt" to chapterSvt.trim(),
            "questions" to questionMaps,
            "approvedStudentIds" to approvedStudentIds.distinct().take(100),
            "isPublished" to true,
            "isDownloadable" to isDownloadable,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            val batch = db.batch()
            batch.set(quizRef, payload)
            for (studentId in approvedStudentIds.distinct()) {
                val notifRef = db.collection("student_notifications").document()
                batch.set(
                    notifRef,
                    mapOf(
                        "studentId" to studentId,
                        "teacherId" to uid,
                        "type" to "quiz",
                        "title" to notifTitle,
                        "body" to notifBody,
                        "isRead" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            return quizRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, quizRef.path)
            throw e
        }
    }

    suspend fun updateQuizDownloadable(quizId: String, isDownloadable: Boolean) {
        requireUserId()
        val docRef = db.collection("quizzes").document(quizId)
        try {
            docRef.update(
                mapOf(
                    "isDownloadable" to isDownloadable,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteQuiz(quiz: Quiz) {
        val uid = requireUserId()
        if (quiz.id.isBlank()) return
        try {
            val refsToDelete = mutableListOf(
                db.collection("quizzes").document(quiz.id)
            )
            val attemptSnaps = db.collection("quiz_attempts")
                .whereEqualTo("teacherId", uid)
                .whereEqualTo("quizId", quiz.id)
                .get()
                .await()
            refsToDelete.addAll(attemptSnaps.documents.map { it.reference })

            refsToDelete.chunked(400).forEach { group ->
                val batch = db.batch()
                group.forEach { batch.delete(it) }
                batch.commit().await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "quizzes/${quiz.id}")
            throw e
        }
    }

    suspend fun submitQuizAttempt(
        quiz: Quiz,
        studentName: String,
        selectedAnswers: List<Int>,
        perQuestionSeconds: List<Long>,
        totalDurationSeconds: Long,
        attemptId: String? = null
    ): QuizAttempt {
        val uid = requireUserId()
        val answersRecords = quiz.questions.mapIndexed { index, q ->
            val selected = selectedAnswers.getOrElse(index) { -1 }
            val isCorrect = selected == q.correctIndex
            val timeSpent = perQuestionSeconds.getOrElse(index) { 0L }.coerceAtLeast(0L)
            QuestionAnswerRecord(
                questionIndex = index,
                selectedIndex = selected,
                isCorrect = isCorrect,
                timeSpentSeconds = timeSpent
            )
        }
        val score = answersRecords.count { it.isCorrect }.toDouble()
        val maxScore = quiz.questions.size.coerceAtLeast(1).toDouble()

        val attemptRef = if (!attemptId.isNullOrBlank()) {
            db.collection("quiz_attempts").document(attemptId)
        } else {
            db.collection("quiz_attempts").document()
        }
        val answerMaps = answersRecords.map { rec ->
            mapOf(
                "questionIndex" to rec.questionIndex,
                "selectedIndex" to rec.selectedIndex,
                "isCorrect" to rec.isCorrect,
                "timeSpentSeconds" to rec.timeSpentSeconds
            )
        }
        val payload = mapOf(
            "quizId" to quiz.id,
            "quizTitle" to quiz.title,
            "classId" to quiz.classId,
            "teacherId" to quiz.teacherId,
            "studentId" to uid,
            "studentName" to studentName,
            "score" to score,
            "maxScore" to maxScore,
            "totalDurationSeconds" to totalDurationSeconds.coerceAtLeast(0L),
            "answers" to answerMaps,
            "completedAt" to FieldValue.serverTimestamp(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            attemptRef.set(payload).await()
            return QuizAttempt(
                id = attemptRef.id,
                quizId = quiz.id,
                quizTitle = quiz.title,
                classId = quiz.classId,
                teacherId = quiz.teacherId,
                studentId = uid,
                studentName = studentName,
                score = score,
                maxScore = maxScore,
                totalDurationSeconds = totalDurationSeconds,
                answers = answersRecords
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, attemptRef.path)
            throw e
        }
    }

    suspend fun syncPendingQuizAttempt(
        attemptId: String,
        quizId: String,
        quizTitle: String,
        classId: String,
        teacherId: String,
        studentName: String,
        score: Double,
        maxScore: Double,
        totalDurationSeconds: Long,
        answers: List<QuestionAnswerRecord>
    ): QuizAttempt {
        val uid = requireUserId()
        val attemptRef = db.collection("quiz_attempts").document(attemptId)
        try {
            val existing = runCatching { attemptRef.get().await() }.getOrNull()
            if (existing != null && existing.exists()) {
                return QuizAttempt(
                    id = attemptId,
                    quizId = quizId,
                    quizTitle = quizTitle,
                    classId = classId,
                    teacherId = teacherId,
                    studentId = uid,
                    studentName = studentName,
                    score = score,
                    maxScore = maxScore,
                    totalDurationSeconds = totalDurationSeconds,
                    answers = answers
                )
            }
            val answerMaps = answers.map { rec ->
                mapOf(
                    "questionIndex" to rec.questionIndex,
                    "selectedIndex" to rec.selectedIndex,
                    "isCorrect" to rec.isCorrect,
                    "timeSpentSeconds" to rec.timeSpentSeconds
                )
            }
            val payload = mapOf(
                "quizId" to quizId,
                "quizTitle" to quizTitle,
                "classId" to classId,
                "teacherId" to teacherId,
                "studentId" to uid,
                "studentName" to studentName,
                "score" to score,
                "maxScore" to maxScore,
                "totalDurationSeconds" to totalDurationSeconds.coerceAtLeast(0L),
                "answers" to answerMaps,
                "completedAt" to FieldValue.serverTimestamp(),
                "createdAt" to FieldValue.serverTimestamp()
            )
            attemptRef.set(payload).await()
            return QuizAttempt(
                id = attemptId,
                quizId = quizId,
                quizTitle = quizTitle,
                classId = classId,
                teacherId = teacherId,
                studentId = uid,
                studentName = studentName,
                score = score,
                maxScore = maxScore,
                totalDurationSeconds = totalDurationSeconds,
                answers = answers
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, attemptRef.path)
            throw e
        }
    }

    // ---------------------------------------------------------
    // 5. Resources & Video Tracking
    // ---------------------------------------------------------
    fun observeTeacherResources(teacherId: String): Flow<List<CourseResource>> = flow {
        db.collection("resources")
            .whereEqualTo("teacherId", teacherId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CourseResource::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .logFirestoreErrors(OperationType.LIST, "resources", teacherId)
            .collect { emit(it) }
    }

    fun observeStudentResources(classId: String): Flow<List<CourseResource>> = flow {
        db.collection("resources")
            .whereEqualTo("classId", classId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CourseResource::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .logFirestoreErrors(OperationType.LIST, "resources", classId)
            .collect { emit(it) }
    }

    fun observeTeacherVideoTracking(teacherId: String): Flow<List<VideoTracking>> = flow {
        db.collection("video_tracking")
            .whereEqualTo("teacherId", teacherId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(VideoTracking::class.java, ServerTimestampBehavior.ESTIMATE)
            }
            .logFirestoreErrors(OperationType.LIST, "video_tracking", teacherId)
            .collect { emit(it) }
    }

    fun observeStudentVideoTracking(studentId: String): Flow<List<VideoTracking>> = flow {
        db.collection("video_tracking")
            .whereEqualTo("studentId", studentId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(VideoTracking::class.java, ServerTimestampBehavior.ESTIMATE)
            }
            .logFirestoreErrors(OperationType.LIST, "video_tracking", studentId)
            .collect { emit(it) }
    }

    suspend fun createResource(
        classId: String,
        title: String,
        description: String,
        type: String,
        fileUrl: String,
        durationSeconds: Long,
        approvedStudentIds: List<String>,
        notifTitle: String,
        notifBody: String,
        isDownloadable: Boolean = false
    ): String {
        val uid = requireUserId()
        val resRef = db.collection("resources").document()
        val payload = mapOf(
            "classId" to classId,
            "teacherId" to uid,
            "title" to title.trim(),
            "description" to description.trim(),
            "type" to type,
            "fileUrl" to fileUrl.trim(),
            "durationSeconds" to durationSeconds.coerceAtLeast(0L),
            "approvedStudentIds" to approvedStudentIds.distinct().take(100),
            "isDownloadable" to isDownloadable,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            val batch = db.batch()
            batch.set(resRef, payload)
            for (studentId in approvedStudentIds.distinct()) {
                val notifRef = db.collection("student_notifications").document()
                batch.set(
                    notifRef,
                    mapOf(
                        "studentId" to studentId,
                        "teacherId" to uid,
                        "type" to "resource",
                        "title" to notifTitle,
                        "body" to notifBody,
                        "isRead" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            return resRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, resRef.path)
            throw e
        }
    }

    suspend fun uploadChunkedResource(
        appContext: Context,
        classId: String,
        title: String,
        description: String,
        type: String,
        contentUriString: String,
        durationSeconds: Long,
        approvedStudentIds: List<String>,
        notifTitle: String,
        notifBody: String,
        isDownloadable: Boolean = false
    ): String {
        val uid = requireUserId()
        val uri = Uri.parse(contentUriString)
        val (fileBytes, ext) = withContext(Dispatchers.IO) {
            val resolver = appContext.contentResolver
            val mime = resolver.getType(uri)
            val fromMime = mime?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            val displayName = runCatching {
                resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                }
            }.getOrNull()
            val extResolved = when {
                !fromMime.isNullOrBlank() -> fromMime.lowercase(Locale.US)
                !displayName.isNullOrBlank() && displayName.contains('.') ->
                    displayName.substringAfterLast('.').lowercase(Locale.US).take(5)
                else -> OfflineStorageManager.inferExtension(contentUriString, type)
            }

            val input = try {
                resolver.openInputStream(uri)
            } catch (e: Exception) {
                throw ResourceReadException("Cannot read selected file", e)
            } ?: throw ResourceReadException("Cannot open selected file")

            val out = ByteArrayOutputStream()
            val buf = ByteArray(16 * 1024)
            var total = 0L
            input.use { stream ->
                while (true) {
                    val read = stream.read(buf)
                    if (read == -1) break
                    total += read
                    if (total > MAX_CHUNKED_UPLOAD_BYTES) {
                        throw ResourceUploadTooLargeException(total, MAX_CHUNKED_UPLOAD_BYTES)
                    }
                    out.write(buf, 0, read)
                }
            }
            val bytes = out.toByteArray()
            if (bytes.isEmpty()) {
                throw ResourceReadException("Selected file is empty")
            }
            bytes to extResolved
        }

        val resRef = db.collection("resources").document()
        val resourceId = resRef.id
        val chunkSize = 600 * 1024
        val chunks = mutableListOf<ByteArray>()
        var offset = 0
        while (offset < fileBytes.size) {
            val end = minOf(offset + chunkSize, fileBytes.size)
            chunks.add(fileBytes.copyOfRange(offset, end))
            offset = end
        }
        val totalChunks = chunks.size
        val createdChunkRefs = mutableListOf<com.google.firebase.firestore.DocumentReference>()

        try {
            // Upload chunks in batches of 8 (each batch <= ~4.8 MiB, well under Firestore 10 MiB write limit)
            chunks.withIndex().chunked(8).forEach { group ->
                val chunkBatch = db.batch()
                group.forEach { (idx, chunkBytes) ->
                    val chunkRef = db.collection("resource_chunks").document("${resourceId}_$idx")
                    createdChunkRefs.add(chunkRef)
                    chunkBatch.set(
                        chunkRef,
                        mapOf(
                            "resourceId" to resourceId,
                            "classId" to classId,
                            "teacherId" to uid,
                            "chunkIndex" to idx,
                            "totalChunks" to totalChunks,
                            "data" to Blob.fromBytes(chunkBytes),
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                chunkBatch.commit().await()
            }

            val chunkSchemeUrl = "${OfflineStorageManager.FIRESTORE_CHUNK_SCHEME}://${OfflineStorageManager.FIRESTORE_CHUNK_HOST}/$resourceId?ext=$ext"
            val payload = mapOf(
                "classId" to classId,
                "teacherId" to uid,
                "title" to title.trim(),
                "description" to description.trim(),
                "type" to type,
                "fileUrl" to chunkSchemeUrl,
                "durationSeconds" to durationSeconds.coerceAtLeast(0L),
                "approvedStudentIds" to approvedStudentIds.distinct().take(100),
                "isDownloadable" to isDownloadable,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val batch = db.batch()
            batch.set(resRef, payload)
            for (studentId in approvedStudentIds.distinct()) {
                val notifRef = db.collection("student_notifications").document()
                batch.set(
                    notifRef,
                    mapOf(
                        "studentId" to studentId,
                        "teacherId" to uid,
                        "type" to "resource",
                        "title" to notifTitle,
                        "body" to notifBody,
                        "isRead" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            return resourceId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            createdChunkRefs.chunked(200).forEach { group ->
                runCatching {
                    val cleanup = db.batch()
                    group.forEach { cleanup.delete(it) }
                    cleanup.commit().await()
                }
            }
            handleFirestoreError(e, OperationType.CREATE, resRef.path)
            throw e
        }
    }

    suspend fun deleteResource(resource: CourseResource) {
        val uid = requireUserId()
        if (resource.id.isBlank()) return
        try {
            val refsToDelete = mutableListOf(
                db.collection("resources").document(resource.id)
            )
            val chunkSnaps = db.collection("resource_chunks")
                .whereEqualTo("teacherId", uid)
                .whereEqualTo("resourceId", resource.id)
                .get()
                .await()
            refsToDelete.addAll(chunkSnaps.documents.map { it.reference })

            val trackingSnaps = db.collection("video_tracking")
                .whereEqualTo("teacherId", uid)
                .whereEqualTo("resourceId", resource.id)
                .get()
                .await()
            refsToDelete.addAll(trackingSnaps.documents.map { it.reference })

            refsToDelete.chunked(400).forEach { group ->
                val batch = db.batch()
                group.forEach { batch.delete(it) }
                batch.commit().await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "resources/${resource.id}")
            throw e
        }
    }

    suspend fun updateResourceDownloadable(resourceId: String, isDownloadable: Boolean) {
        requireUserId()
        val docRef = db.collection("resources").document(resourceId)
        try {
            docRef.update(
                mapOf(
                    "isDownloadable" to isDownloadable,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun upsertVideoTracking(
        resource: CourseResource,
        studentName: String,
        positionSeconds: Long,
        totalDurationSeconds: Long,
        existingTracking: VideoTracking?,
        watchedPercentOverride: Double? = null
    ): VideoTracking = videoTrackingMutex.withLock {
        val uid = requireUserId()
        val trackingId = "${resource.id}_$uid"
        val docRef = db.collection("video_tracking").document(trackingId)
        val safeStudentName = studentName.trim().ifBlank { "Élève" }.take(100)

        try {
            val existingSnap = runCatching { docRef.get().await() }.getOrNull()
            val docExists = existingSnap?.exists() == true
            val remotePrevPos = if (docExists) {
                existingSnap?.getLong("maxPositionSeconds") ?: 0L
            } else 0L
            val remotePrevPct = if (docExists) {
                existingSnap?.getDouble("watchedPercentage") ?: 0.0
            } else 0.0

            val prevMax = maxOf(existingTracking?.maxPositionSeconds ?: 0L, remotePrevPos)
            val prevPct = maxOf(existingTracking?.watchedPercentage ?: 0.0, remotePrevPct)
            val (safeDuration, maxPos, pct, status) = computeVideoTrackingMetrics(
                positionSeconds = positionSeconds,
                totalDurationSeconds = totalDurationSeconds,
                resourceDurationSeconds = resource.durationSeconds,
                prevPositionSeconds = prevMax,
                prevPercentage = prevPct,
                watchedPercentOverride = watchedPercentOverride
            )

            if (!docExists) {
                val createPayload = mapOf(
                    "resourceId" to resource.id,
                    "classId" to resource.classId,
                    "teacherId" to resource.teacherId,
                    "studentId" to uid,
                    "studentName" to safeStudentName,
                    "maxPositionSeconds" to maxPos,
                    "durationSeconds" to safeDuration,
                    "watchedPercentage" to pct,
                    "status" to status,
                    "lastWatchedAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createPayload).await()
            } else {
                val updatePayload = mapOf(
                    "studentName" to safeStudentName,
                    "maxPositionSeconds" to maxPos,
                    "durationSeconds" to safeDuration,
                    "watchedPercentage" to pct,
                    "status" to status,
                    "lastWatchedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            }
            return@withLock VideoTracking(
                id = trackingId,
                resourceId = resource.id,
                classId = resource.classId,
                teacherId = resource.teacherId,
                studentId = uid,
                studentName = safeStudentName,
                maxPositionSeconds = maxPos,
                durationSeconds = safeDuration,
                watchedPercentage = pct,
                status = status
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun recordFileOpenedTracking(
        resource: CourseResource,
        studentName: String
    ): VideoTracking {
        return upsertVideoTracking(
            resource = resource,
            studentName = studentName,
            positionSeconds = 1L,
            totalDurationSeconds = 1L,
            existingTracking = null,
            watchedPercentOverride = 100.0
        )
    }

    // ---------------------------------------------------------
    // 6. Announcements
    // ---------------------------------------------------------
    fun observeTeacherAnnouncements(teacherId: String): Flow<List<Announcement>> = flow {
        db.collection("announcements")
            .whereEqualTo("teacherId", teacherId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Announcement::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .logFirestoreErrors(OperationType.LIST, "announcements", teacherId)
            .collect { emit(it) }
    }

    fun observeStudentAnnouncements(classId: String): Flow<List<Announcement>> = flow {
        db.collection("announcements")
            .whereEqualTo("classId", classId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Announcement::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .logFirestoreErrors(OperationType.LIST, "announcements", classId)
            .collect { emit(it) }
    }

    suspend fun createAnnouncement(
        classId: String,
        title: String,
        content: String,
        approvedStudentIds: List<String>,
        notifTitle: String,
        notifBody: String
    ) {
        val uid = requireUserId()
        val annRef = db.collection("announcements").document()
        val payload = mapOf(
            "classId" to classId,
            "teacherId" to uid,
            "title" to title.trim(),
            "content" to content.trim(),
            "approvedStudentIds" to approvedStudentIds.distinct().take(100),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            val batch = db.batch()
            batch.set(annRef, payload)
            for (studentId in approvedStudentIds.distinct()) {
                val notifRef = db.collection("student_notifications").document()
                batch.set(
                    notifRef,
                    mapOf(
                        "studentId" to studentId,
                        "teacherId" to uid,
                        "type" to "announcement",
                        "title" to notifTitle,
                        "body" to notifBody,
                        "isRead" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, annRef.path)
            throw e
        }
    }

    suspend fun deleteAnnouncement(announcement: Announcement) {
        requireUserId()
        if (announcement.id.isBlank()) return
        val docRef = db.collection("announcements").document(announcement.id)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // ---------------------------------------------------------
    // 7. Private Messaging (Strictly Student <-> Teacher)
    // ---------------------------------------------------------
    fun observeTeacherMessages(teacherId: String): Flow<List<PrivateMessage>> = flow {
        db.collection("private_messages")
            .whereEqualTo("teacherId", teacherId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PrivateMessage::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .logFirestoreErrors(OperationType.LIST, "private_messages", teacherId)
            .collect { emit(it) }
    }

    fun observeStudentMessages(studentId: String): Flow<List<PrivateMessage>> = flow {
        db.collection("private_messages")
            .whereEqualTo("studentId", studentId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(PrivateMessage::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .logFirestoreErrors(OperationType.LIST, "private_messages", studentId)
            .collect { emit(it) }
    }

    suspend fun sendPrivateMessage(
        classId: String,
        teacherId: String,
        studentId: String,
        studentName: String,
        senderRole: String,
        content: String,
        notifTitleForStudent: String? = null
    ) {
        val uid = requireUserId()
        val msgRef = db.collection("private_messages").document()
        val payload = mapOf(
            "classId" to classId,
            "teacherId" to teacherId,
            "studentId" to studentId,
            "studentName" to studentName,
            "senderId" to uid,
            "senderRole" to senderRole,
            "content" to content.trim(),
            "isRead" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            val batch = db.batch()
            batch.set(msgRef, payload)
            if (senderRole == "teacher" && !notifTitleForStudent.isNullOrBlank()) {
                val notifRef = db.collection("student_notifications").document()
                batch.set(
                    notifRef,
                    mapOf(
                        "studentId" to studentId,
                        "teacherId" to teacherId,
                        "type" to "message",
                        "title" to notifTitleForStudent,
                        "body" to content.trim().take(120),
                        "isRead" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            throw e
        }
    }

    suspend fun markMessagesAsRead(messages: List<PrivateMessage>) {
        requireUserId()
        val unread = messages.filter { !it.isRead && it.id.isNotBlank() }
        if (unread.isEmpty()) return
        try {
            val batch = db.batch()
            unread.take(100).forEach { msg ->
                batch.update(db.collection("private_messages").document(msg.id), "isRead", true)
            }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "private_messages")
        }
    }

    // ---------------------------------------------------------
    // 8. Student Notifications (including Badge Unlocks)
    // ---------------------------------------------------------
    fun observeStudentNotifications(studentId: String): Flow<List<StudentNotification>> = flow {
        db.collection("student_notifications")
            .whereEqualTo("studentId", studentId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(StudentNotification::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .logFirestoreErrors(OperationType.LIST, "student_notifications", studentId)
            .collect { emit(it) }
    }

    suspend fun createBadgeNotification(
        studentId: String,
        teacherId: String,
        title: String,
        body: String
    ) {
        requireUserId()
        val notifRef = db.collection("student_notifications").document()
        val payload = mapOf(
            "studentId" to studentId,
            "teacherId" to teacherId,
            "type" to "badge",
            "title" to title,
            "body" to body,
            "isRead" to false,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            notifRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, notifRef.path)
        }
    }

    suspend fun markAllNotificationsRead(notifications: List<StudentNotification>) {
        requireUserId()
        val unread = notifications.filter { !it.isRead && it.id.isNotBlank() }
        if (unread.isEmpty()) return
        try {
            val batch = db.batch()
            unread.take(100).forEach { notif ->
                batch.update(db.collection("student_notifications").document(notif.id), "isRead", true)
            }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "student_notifications")
        }
    }

    // ---------------------------------------------------------
    // 9. Class Rosters (Excel Import & Provisional Passwords)
    // ---------------------------------------------------------
    fun observeTeacherRoster(teacherId: String): Flow<List<RosterStudent>> = flow {
        db.collection("class_rosters")
            .whereEqualTo("teacherId", teacherId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(RosterStudent::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.fullName.lowercase(Locale.getDefault()) }
            }
            .logFirestoreErrors(OperationType.LIST, "class_rosters", teacherId)
            .collect { emit(it) }
    }

    fun observeStudentRoster(studentUid: String): Flow<RosterStudent?> = flow {
        db.collection("class_rosters")
            .whereEqualTo("studentUid", studentUid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(RosterStudent::class.java, ServerTimestampBehavior.ESTIMATE)
                    .firstOrNull()
            }
            .logFirestoreErrors(OperationType.LIST, "class_rosters", studentUid)
            .collect { emit(it) }
    }

    suspend fun saveRosterStudent(roster: RosterStudent): RosterStudent {
        val uid = requireUserId()
        val safeDocId = roster.id.ifBlank { "${roster.classId}_${roster.massarNumber}" }
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .take(120)
        val docRef = db.collection("class_rosters").document(safeDocId)
        try {
            val existing = runCatching { docRef.get().await() }.getOrNull()
            if (existing != null && existing.exists()) {
                val updates = mapOf(
                    "className" to roster.className.trim(),
                    "teacherName" to roster.teacherName.trim(),
                    "qrToken" to roster.qrToken.trim(),
                    "fullName" to roster.fullName.trim(),
                    "massarNumber" to roster.massarNumber.trim(),
                    "provisionalPassword" to roster.provisionalPassword,
                    "studentUid" to roster.studentUid,
                    "qrPayload" to roster.qrPayload,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "classId" to roster.classId,
                    "className" to roster.className.trim(),
                    "teacherId" to uid,
                    "teacherName" to roster.teacherName.trim(),
                    "qrToken" to roster.qrToken.trim(),
                    "fullName" to roster.fullName.trim(),
                    "massarNumber" to roster.massarNumber.trim(),
                    "provisionalPassword" to roster.provisionalPassword,
                    "studentUid" to roster.studentUid,
                    "qrPayload" to roster.qrPayload,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            return roster.copy(id = safeDocId, teacherId = uid)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun deleteRosterStudent(rosterId: String) {
        requireUserId()
        if (rosterId.isBlank()) return
        val docRef = db.collection("class_rosters").document(rosterId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    // ---------------------------------------------------------
    // 10. Teacher-Published App Updates (HTTPS distribution URL)
    // ---------------------------------------------------------
    fun observeTeacherAppUpdates(teacherId: String): Flow<List<AppUpdateRelease>> = flow {
        db.collection("app_updates")
            .whereEqualTo("teacherId", teacherId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AppUpdateRelease::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.versionCode }
            }
            .logFirestoreErrors(OperationType.LIST, "app_updates", teacherId)
            .collect { emit(it) }
    }

    fun observeClassAppUpdates(classId: String): Flow<List<AppUpdateRelease>> = flow {
        db.collection("app_updates")
            .whereEqualTo("classId", classId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AppUpdateRelease::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.versionCode }
            }
            .logFirestoreErrors(OperationType.LIST, "app_updates", classId)
            .collect { emit(it) }
    }

    suspend fun publishAppUpdate(
        classId: String,
        teacherName: String,
        versionCode: Int,
        versionName: String,
        releaseNotes: String,
        downloadUrl: String
    ): String {
        val uid = requireUserId()
        val updateRef = db.collection("app_updates").document()
        val payload = mapOf(
            "classId" to classId,
            "teacherId" to uid,
            "teacherName" to teacherName.trim().ifBlank { "Professeur" },
            "versionCode" to versionCode,
            "versionName" to versionName.trim(),
            "releaseNotes" to releaseNotes.trim(),
            "downloadUrl" to downloadUrl.trim(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            updateRef.set(payload).await()
            return updateRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, updateRef.path)
            throw e
        }
    }

    suspend fun deleteAppUpdate(updateId: String) {
        requireUserId()
        if (updateId.isBlank()) return
        val docRef = db.collection("app_updates").document(updateId)
        try {
            docRef.delete().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    private fun todayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    private fun yesterdayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date(System.currentTimeMillis() - 86_400_000L))
    }

    data class VideoTrackingMetrics(
        val durationSeconds: Long,
        val maxPositionSeconds: Long,
        val watchedPercentage: Double,
        val status: String
    )

    companion object {
        const val MAX_CHUNKED_UPLOAD_BYTES: Long = 20L * 1024L * 1024L

        fun computeVideoTrackingMetrics(
            positionSeconds: Long,
            totalDurationSeconds: Long,
            resourceDurationSeconds: Long,
            prevPositionSeconds: Long = 0L,
            prevPercentage: Double = 0.0,
            watchedPercentOverride: Double? = null
        ): VideoTrackingMetrics {
            val safeDuration = (if (totalDurationSeconds > 0L) totalDurationSeconds else resourceDurationSeconds)
                .coerceIn(1L, 86400L)
            val maxPos = maxOf(prevPositionSeconds, positionSeconds).coerceIn(0L, safeDuration)
            val computedPct = ((maxPos.toDouble() / safeDuration.toDouble()) * 100.0).coerceIn(0.0, 100.0)
            val pct = maxOf(
                prevPercentage.coerceIn(0.0, 100.0),
                computedPct,
                watchedPercentOverride?.coerceIn(0.0, 100.0) ?: 0.0
            ).coerceIn(0.0, 100.0)
            val status = when {
                pct >= 90.0 -> "completed"
                pct > 0.0 -> "partial"
                else -> "not_started"
            }
            return VideoTrackingMetrics(
                durationSeconds = safeDuration,
                maxPositionSeconds = maxPos,
                watchedPercentage = pct,
                status = status
            )
        }
    }
}
