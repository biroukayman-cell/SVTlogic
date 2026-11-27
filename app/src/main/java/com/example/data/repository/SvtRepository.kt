package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.example.R
import com.example.core.AppUpdateUrlHelper
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.core.isPermissionDeniedError
import com.example.core.isQuotaExceededError
import com.example.data.badges.BadgeEngine
import com.example.data.badges.SvtBadgeDefinition
import com.example.data.model.Announcement
import com.example.data.model.AppUpdateRelease
import com.example.data.model.ClassCode
import com.example.data.model.CourseResource
import com.example.data.model.CustomGlossaryEntry
import com.example.data.model.Enrollment
import com.example.data.model.Establishment
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
import com.google.firebase.firestore.WriteBatch
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
            if (code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                return@retryWhen false
            }
            val isTransient = code == FirebaseFirestoreException.Code.UNAVAILABLE ||
                code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ||
                code == FirebaseFirestoreException.Code.ABORTED ||
                (code == FirebaseFirestoreException.Code.PERMISSION_DENIED && attempt < 1L)
            if (isTransient && attempt < 3L) {
                delay(650L * (attempt + 1L))
                true
            } else {
                false
            }
        }
    }

    private suspend fun commitWriteOpsChunked(ops: List<(WriteBatch) -> Unit>) {
        if (ops.isEmpty()) return
        ops.chunked(FIRESTORE_BATCH_SAFE_LIMIT).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { applyOp -> applyOp(batch) }
            batch.commit().await()
        }
    }

    private suspend fun commitBackfillOpsSafely(
        updates: List<Pair<com.google.firebase.firestore.DocumentReference, Map<String, Any>>>
    ) {
        if (updates.isEmpty()) return
        updates.chunked(FIRESTORE_BATCH_SAFE_LIMIT).forEach { chunk ->
            try {
                val batch = db.batch()
                chunk.forEach { (ref, map) -> batch.update(ref, map) }
                batch.commit().await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (isQuotaExceededError(e)) throw e
                val fsCode = generateSequence<Throwable>(e) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()?.code
                val isBatchValidationOrPerm = isPermissionDeniedError(e) ||
                    fsCode == FirebaseFirestoreException.Code.INVALID_ARGUMENT ||
                    fsCode == FirebaseFirestoreException.Code.NOT_FOUND
                if (!isBatchValidationOrPerm) throw e
                // Legacy invalid docs can reject an entire batch; apply individually so valid docs still update
                for ((ref, map) in chunk) {
                    try {
                        ref.update(map).await()
                    } catch (singleErr: CancellationException) {
                        throw singleErr
                    } catch (singleErr: Exception) {
                        if (isQuotaExceededError(singleErr)) throw singleErr
                    }
                }
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
        establishment: String = "",
        mustChangePassword: Boolean = false,
        boundDeviceId: String = "",
        boundDeviceModel: String = ""
    ): UserProfile {
        val uid = requireUserId()
        val today = todayDateString()
        val docRef = db.collection("users").document(uid)
        val cleanFullName = clampMinMax(fullName, "Utilisateur SVT", 2, 100)
        val cleanPhone = phoneNumber.trim().take(30)
        val cleanMassar = massarNumber.trim().take(40)
        val cleanBirthDate = birthDate.trim().take(30)
        val rawEmail = email.trim()
        val cleanEmail = (if (role == "student" && rawEmail.isBlank()) {
            com.example.core.AuthSessionDraft.defaultStudentTaalimEmail(cleanMassar)
        } else {
            rawEmail
        }).take(150)
        val cleanEstablishment = establishment.trim().take(150)
        val currentAuthUser = auth.currentUser
        val isVerifiedOwnerAdmin = currentAuthUser?.email.equals("biroukayman@gmail.com", ignoreCase = true) &&
            currentAuthUser?.isEmailVerified == true
        val teacherStatus = if (role == "teacher") {
            if (isVerifiedOwnerAdmin) "approved" else "pending"
        } else {
            "approved"
        }
        val acceptedTerms = role != "student"
        val cleanDeviceId = if (role == "student") sanitizeId(boundDeviceId, 128) else ""
        val cleanDeviceModel = if (role == "student") boundDeviceModel.trim().take(100) else ""
        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "fullName" to cleanFullName,
            "phoneNumber" to cleanPhone,
            "role" to role,
            "massarNumber" to cleanMassar,
            "birthDate" to cleanBirthDate,
            "email" to cleanEmail,
            "establishment" to cleanEstablishment,
            "teacherStatus" to teacherStatus,
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
            val existingSnap = runCatching { docRef.get().await() }.getOrNull()
            if (existingSnap != null && existingSnap.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "fullName" to cleanFullName,
                    "phoneNumber" to cleanPhone,
                    "massarNumber" to cleanMassar,
                    "birthDate" to cleanBirthDate,
                    "email" to cleanEmail,
                    "establishment" to cleanEstablishment,
                    "mustChangePassword" to mustChangePassword,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!avatarUrl.isNullOrBlank()) {
                    updates["avatarUrl"] = avatarUrl
                }
                val existingBoundId = existingSnap.getString("boundDeviceId").orEmpty().trim()
                if (existingBoundId.isBlank() && cleanDeviceId.isNotBlank()) {
                    updates["boundDeviceId"] = cleanDeviceId
                    updates["boundDeviceModel"] = cleanDeviceModel
                }
                docRef.update(updates).await()
                val existingObj = existingSnap.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
                return UserProfile(
                    id = uid,
                    userId = uid,
                    fullName = cleanFullName,
                    phoneNumber = cleanPhone,
                    role = existingObj?.role ?: role,
                    massarNumber = cleanMassar,
                    birthDate = cleanBirthDate,
                    email = cleanEmail,
                    establishment = cleanEstablishment,
                    teacherStatus = existingObj?.teacherStatus ?: teacherStatus,
                    mustChangePassword = mustChangePassword,
                    acceptedTerms = existingObj?.acceptedTerms ?: acceptedTerms,
                    avatarUrl = avatarUrl?.takeIf { it.isNotBlank() } ?: existingObj?.avatarUrl,
                    xp = existingObj?.xp ?: 0L,
                    loginStreak = existingObj?.loginStreak ?: 1L,
                    lastLoginDate = existingObj?.lastLoginDate ?: today,
                    unlockedBadgeIds = existingObj?.unlockedBadgeIds ?: emptyList(),
                    boundDeviceId = existingBoundId.ifBlank { cleanDeviceId },
                    boundDeviceModel = if (existingBoundId.isBlank()) cleanDeviceModel else existingObj?.boundDeviceModel.orEmpty()
                )
            }
            docRef.set(payload).await()
            return UserProfile(
                id = uid,
                userId = uid,
                fullName = cleanFullName,
                phoneNumber = cleanPhone,
                role = role,
                massarNumber = cleanMassar,
                birthDate = cleanBirthDate,
                email = cleanEmail,
                establishment = cleanEstablishment,
                teacherStatus = teacherStatus,
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
        val cleanId = sanitizeId(deviceId, 128)
        val cleanModel = deviceModel.trim().take(100)
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
        mustChangePassword: Boolean? = null,
        email: String? = null,
        skipQrLockCheck: Boolean = false
    ) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)

        // Prevent name change if student registered via teacher QR code
        val effectiveName = if (skipQrLockCheck) {
            fullName.trim()
        } else {
            val existingEnrollment = runCatching {
                db.collection("enrollments").document(uid).get().await()
            }.getOrNull()
            val enrolledViaQr = existingEnrollment?.exists() == true &&
                !existingEnrollment.getString("qrToken").isNullOrBlank()
            if (enrolledViaQr) {
                val currentSnap = runCatching { docRef.get().await() }.getOrNull()
                val storedName = currentSnap?.getString("fullName").orEmpty().trim()
                storedName.ifBlank { fullName.trim() }
            } else {
                fullName.trim()
            }
        }

        val clampedName = clampMinMax(effectiveName, "Utilisateur SVT", 2, 100)
        val updates = mutableMapOf<String, Any>(
            "fullName" to clampedName,
            "phoneNumber" to phoneNumber.trim().take(30),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!avatarUrl.isNullOrBlank()) {
            updates["avatarUrl"] = avatarUrl
        }
        if (birthDate != null) {
            updates["birthDate"] = birthDate.trim().take(30)
        }
        if (mustChangePassword != null) {
            updates["mustChangePassword"] = mustChangePassword
        }
        if (email != null) {
            updates["email"] = email.trim().take(150)
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

    suspend fun updateProfileAvatar(avatarUrl: String) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            docRef.update(
                mapOf(
                    "avatarUrl" to avatarUrl,
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

    fun observeAllTeachers(): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        db.collection("users")
            .whereEqualTo("role", "teacher")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedWith(
                        compareBy<UserProfile>(
                            { when (it.teacherStatus) { "pending" -> 0; "rejected" -> 1; else -> 2 } },
                            { -(it.createdAt?.seconds ?: 0L) }
                        )
                    )
            }
            .logFirestoreErrors(OperationType.LIST, "users", uid)
            .collect { emit(it) }
    }

    suspend fun updateTeacherApprovalStatus(teacherUserId: String, status: String) {
        requireUserId()
        require(status in setOf("pending", "approved", "rejected")) { "Invalid teacher status" }
        val docRef = db.collection("users").document(teacherUserId)
        try {
            docRef.update(
                mapOf(
                    "teacherStatus" to status,
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

    suspend fun markPasswordChanged(mustChange: Boolean = false) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        try {
            docRef.update(
                mapOf(
                    "mustChangePassword" to mustChange,
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

    // ---------------------------------------------------------
    // 1b. Establishments (Teacher Schools / Centers)
    // ---------------------------------------------------------
    fun observeTeacherEstablishments(teacherId: String): Flow<List<Establishment>> = flow {
        val path = "establishments"
        db.collection("establishments")
            .whereEqualTo("teacherId", teacherId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Establishment::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.name.lowercase(Locale.getDefault()) }
            }
            .logFirestoreErrors(OperationType.LIST, path, teacherId)
            .collect { emit(it) }
    }

    suspend fun createEstablishment(
        teacherName: String,
        name: String,
        city: String = ""
    ): Establishment {
        val uid = requireUserId()
        val estId = "est_${UUID.randomUUID().toString().take(8)}"
        val cleanTeacher = teacherName.trim().ifBlank { "Professeur" }.take(100)
        val cleanName = name.trim().take(120)
        val cleanCity = city.trim().take(100)
        require(cleanName.length >= 2) { "Establishment name must be at least 2 characters" }
        val docRef = db.collection("establishments").document(estId)
        val payload = mapOf(
            "teacherId" to uid,
            "teacherName" to cleanTeacher,
            "name" to cleanName,
            "city" to cleanCity,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return Establishment(
                id = estId,
                teacherId = uid,
                teacherName = cleanTeacher,
                name = cleanName,
                city = cleanCity
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateEstablishment(
        establishmentId: String,
        name: String,
        city: String = ""
    ) {
        val uid = requireUserId()
        val cleanName = name.trim().take(120)
        val cleanCity = city.trim().take(100)
        require(establishmentId.isNotBlank() && cleanName.length >= 2) {
            "Establishment name must be at least 2 characters"
        }
        val docRef = db.collection("establishments").document(establishmentId)
        try {
            val ops = mutableListOf<(WriteBatch) -> Unit>()
            ops += { batch ->
                batch.update(
                    docRef,
                    mapOf(
                        "name" to cleanName,
                        "city" to cleanCity,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            val linkedClasses = db.collection("classes")
                .whereEqualTo("teacherId", uid)
                .whereEqualTo("establishmentId", establishmentId)
                .get()
                .await()
            for (clsDoc in linkedClasses.documents) {
                ops += { batch ->
                    batch.update(
                        clsDoc.reference,
                        mapOf(
                            "establishmentName" to cleanName,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            commitWriteOpsChunked(ops)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteEstablishment(establishmentId: String) {
        val uid = requireUserId()
        if (establishmentId.isBlank()) return
        val docRef = db.collection("establishments").document(establishmentId)
        try {
            val ops = mutableListOf<(WriteBatch) -> Unit>()
            ops += { batch -> batch.delete(docRef) }
            val linkedClasses = db.collection("classes")
                .whereEqualTo("teacherId", uid)
                .whereEqualTo("establishmentId", establishmentId)
                .get()
                .await()
            for (clsDoc in linkedClasses.documents) {
                ops += { batch ->
                    batch.update(
                        clsDoc.reference,
                        mapOf(
                            "establishmentId" to "",
                            "establishmentName" to "",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            commitWriteOpsChunked(ops)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun createClass(
        teacherName: String,
        name: String,
        level: String,
        academicYear: String,
        establishmentId: String = "",
        establishmentName: String = ""
    ): SvtClass {
        val uid = requireUserId()
        val classId = "cls_${UUID.randomUUID().toString().take(8)}"
        val qrToken = "SVT-${UUID.randomUUID().toString().replace("-", "").take(8).uppercase(Locale.US)}"
        val classRef = db.collection("classes").document(classId)
        val codeRef = db.collection("class_codes").document(qrToken)
        val cleanTeacher = teacherName.trim().ifBlank { "Professeur" }.take(100)
        val cleanName = clampMinMax(name, "Classe SVT", 2, 100)
        val cleanYear = clampMinMax(academicYear, "2025-2026", 2, 30)
        val cleanLevel = level.trim().ifBlank { "SVT" }.take(50)
        val cleanEstId = establishmentId.trim().take(128)
        val cleanEstName = if (cleanEstId.isNotEmpty()) establishmentName.trim().take(120) else ""

        val classPayload = mapOf(
            "teacherId" to uid,
            "teacherName" to cleanTeacher,
            "name" to cleanName,
            "academicYear" to cleanYear,
            "level" to cleanLevel,
            "qrToken" to qrToken,
            "establishmentId" to cleanEstId,
            "establishmentName" to cleanEstName,
            "visibleLabDomains" to SvtClass.DEFAULT_LAB_DOMAINS,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val codePayload = mapOf(
            "classId" to classId,
            "className" to cleanName,
            "academicYear" to cleanYear,
            "teacherId" to uid,
            "teacherName" to cleanTeacher,
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
                teacherName = cleanTeacher,
                name = cleanName,
                academicYear = cleanYear,
                level = cleanLevel,
                qrToken = qrToken,
                establishmentId = cleanEstId,
                establishmentName = cleanEstName,
                visibleLabDomains = SvtClass.DEFAULT_LAB_DOMAINS
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, classRef.path)
            throw e
        }
    }

    suspend fun updateClassDetails(
        svtClass: SvtClass,
        name: String,
        level: String,
        academicYear: String,
        establishmentId: String,
        establishmentName: String
    ) {
        requireUserId()
        val cleanName = name.trim().take(100)
        val cleanLevel = level.trim().ifBlank { "SVT" }.take(50)
        val cleanYear = academicYear.trim().ifBlank { "2025-2026" }.take(30)
        val cleanEstId = establishmentId.trim().take(128)
        val cleanEstName = if (cleanEstId.isNotEmpty()) establishmentName.trim().take(120) else ""
        require(svtClass.id.isNotBlank() && cleanName.length >= 2) {
            "Class name must be at least 2 characters"
        }
        val classRef = db.collection("classes").document(svtClass.id)
        try {
            val needsCodeUpdate = svtClass.qrToken.isNotBlank() &&
                (svtClass.name.trim() != cleanName || svtClass.academicYear.trim() != cleanYear)
            val codeRef = if (needsCodeUpdate) {
                db.collection("class_codes").document(svtClass.qrToken)
            } else {
                null
            }
            val codeExists = codeRef?.let { ref ->
                runCatching { ref.get().await().exists() }.getOrDefault(false)
            } == true

            val batch = db.batch()
            batch.update(
                classRef,
                mapOf(
                    "name" to cleanName,
                    "level" to cleanLevel,
                    "academicYear" to cleanYear,
                    "establishmentId" to cleanEstId,
                    "establishmentName" to cleanEstName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (codeRef != null && codeExists) {
                batch.update(
                    codeRef,
                    mapOf(
                        "className" to cleanName,
                        "academicYear" to cleanYear
                    )
                )
            }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, classRef.path)
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
            val docRefsToDelete = mutableListOf<com.google.firebase.firestore.DocumentReference>()
            val classRef = db.collection("classes").document(classId)
            if (runCatching { classRef.get().await().exists() }.getOrDefault(true)) {
                docRefsToDelete += classRef
            }
            val token = svtClass.qrToken.trim()
            if (token.isNotBlank()) {
                val codeRef = db.collection("class_codes").document(token)
                if (runCatching { codeRef.get().await().exists() }.getOrDefault(false)) {
                    docRefsToDelete += codeRef
                }
            }
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
        val resolvedToken = classCode.id.trim().ifBlank { qrToken.trim() }.take(64)
        val status = if (resolvedToken.isNotBlank()) "approved" else "pending"
        val cleanName = clampMinMax(studentName, "Élève SVT", 2, 100)
        val cleanPhone = studentPhone.trim().take(30)
        val cleanMassar = massarNumber.trim().take(40)
        val cleanClassName = clampMinMax(classCode.className, "Classe SVT", 2, 100)
        val cleanTeacherName = classCode.teacherName.trim().ifBlank { "Professeur" }.take(100)
        val payload = mutableMapOf<String, Any>(
            "studentId" to uid,
            "classId" to classCode.classId,
            "className" to cleanClassName,
            "teacherId" to classCode.teacherId,
            "teacherName" to cleanTeacherName,
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
            try {
                docRef.set(payload).await()
            } catch (e: Exception) {
                if (existing == null && isPermissionDeniedError(e)) {
                    // Handle stale server enrollment doc when cache/get missed it
                    val deleted = runCatching { docRef.delete().await() }.isSuccess
                    if (deleted) {
                        docRef.set(payload).await()
                    } else {
                        throw e
                    }
                } else {
                    throw e
                }
            }
            return Enrollment(
                id = uid,
                studentId = uid,
                classId = classCode.classId,
                className = cleanClassName,
                teacherId = classCode.teacherId,
                teacherName = cleanTeacherName,
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
        approvePendingEnrollmentsBatch(
            enrollments = listOf(enrollment),
            notifTitle = notifTitle,
            notifBody = notifBody
        )
    }

    suspend fun approvePendingEnrollmentsBatch(
        enrollments: List<Enrollment>,
        notifTitle: String,
        notifBody: String
    ) {
        val uid = requireUserId()
        val distinctEnrollments = enrollments
            .filter { it.studentId.isNotBlank() }
            .distinctBy { it.studentId }
        if (distinctEnrollments.isEmpty()) return

        val cleanNotifTitle = notifTitle.trim().ifBlank { "BioTerre SVT" }.take(200)
        val cleanNotifBody = notifBody.trim().ifBlank { cleanNotifTitle }.take(1000)
        val firstPath = "enrollments/${distinctEnrollments.first().studentId}"
        try {
            val criticalOps = mutableListOf<(WriteBatch) -> Unit>()

            // 1. Approve each enrollment and create its welcome notification first
            for (enrollment in distinctEnrollments) {
                val enrollRef = db.collection("enrollments").document(enrollment.studentId)
                criticalOps += { batch ->
                    batch.update(
                        enrollRef,
                        mapOf(
                            "status" to "approved",
                            "validatedAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                val notifRef = db.collection("student_notifications").document()
                criticalOps += { batch ->
                    batch.set(
                        notifRef,
                        mapOf(
                            "studentId" to enrollment.studentId,
                            "teacherId" to uid,
                            "type" to "enrollment",
                            "title" to cleanNotifTitle,
                            "body" to cleanNotifBody,
                            "isRead" to false,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            commitWriteOpsChunked(criticalOps)

            // 2. Backfill approvedStudentIds separately so a legacy invalid doc cannot block approval
            val backfillUpdates = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, Map<String, Any>>>()
            val classScopedCollections = listOf("quizzes", "resources", "announcements")
            val byClass = distinctEnrollments.groupBy { it.classId }
            for ((classId, classEnrollments) in byClass) {
                if (classId.isBlank()) continue
                val studentIds = classEnrollments.map { it.studentId }.distinct()
                for (col in classScopedCollections) {
                    val docs = db.collection(col)
                        .whereEqualTo("teacherId", uid)
                        .whereEqualTo("classId", classId)
                        .get()
                        .await()
                    for (doc in docs.documents) {
                        if (col == "resources" && !isDocSafeForResourceUpdate(doc)) continue
                        val existingIds = (doc.get("approvedStudentIds") as? List<*>)
                            ?.mapNotNull { it as? String }
                            .orEmpty()
                        val remainingSlots = (100 - existingIds.size).coerceAtLeast(0)
                        if (remainingSlots <= 0) continue
                        val toAdd = studentIds.filterNot { it in existingIds }.take(remainingSlots)
                        if (toAdd.isEmpty()) continue
                        backfillUpdates += doc.reference to mapOf(
                            "approvedStudentIds" to FieldValue.arrayUnion(*toAdd.toTypedArray()),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    }
                }
            }

            // 3. Query teacher glossary_entries only ONCE for all approved students
            val allStudentIds = distinctEnrollments.map { it.studentId }.distinct()
            val glossaryDocs = db.collection("glossary_entries")
                .whereEqualTo("teacherId", uid)
                .get()
                .await()
            for (doc in glossaryDocs.documents) {
                val existingIds = (doc.get("approvedStudentIds") as? List<*>)
                    ?.mapNotNull { it as? String }
                    .orEmpty()
                val remainingSlots = (500 - existingIds.size).coerceAtLeast(0)
                if (remainingSlots <= 0) continue
                val toAdd = allStudentIds.filterNot { it in existingIds }.take(remainingSlots)
                if (toAdd.isEmpty()) continue
                backfillUpdates += doc.reference to mapOf(
                    "approvedStudentIds" to FieldValue.arrayUnion(*toAdd.toTypedArray()),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            }

            commitBackfillOpsSafely(backfillUpdates)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, firstPath)
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
            val criticalOps = mutableListOf<(WriteBatch) -> Unit>()
            criticalOps += { batch -> batch.delete(db.collection("enrollments").document(studentId)) }

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
                    val docRef = doc.reference
                    criticalOps += { batch -> batch.delete(docRef) }
                }
            }

            val rosterDocs = db.collection("class_rosters")
                .whereEqualTo("teacherId", uid)
                .whereEqualTo("classId", enrollment.classId)
                .whereEqualTo("studentUid", studentId)
                .get()
                .await()
            for (doc in rosterDocs.documents) {
                val docRef = doc.reference
                criticalOps += { batch -> batch.delete(docRef) }
            }
            commitWriteOpsChunked(criticalOps)

            val backfillUpdates = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, Map<String, Any>>>()
            val classScopedCollections = listOf("quizzes", "resources", "announcements")
            for (col in classScopedCollections) {
                val docs = db.collection(col)
                    .whereEqualTo("teacherId", uid)
                    .whereEqualTo("classId", enrollment.classId)
                    .get()
                    .await()
                for (doc in docs.documents) {
                    if (col == "resources" && !isDocSafeForResourceUpdate(doc)) continue
                    val existingIds = (doc.get("approvedStudentIds") as? List<*>)
                        ?.mapNotNull { it as? String }
                        .orEmpty()
                    if (studentId !in existingIds) continue
                    backfillUpdates += doc.reference to mapOf(
                        "approvedStudentIds" to FieldValue.arrayRemove(studentId),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                }
            }

            val glossaryDocs = db.collection("glossary_entries")
                .whereEqualTo("teacherId", uid)
                .get()
                .await()
            for (doc in glossaryDocs.documents) {
                val existingIds = (doc.get("approvedStudentIds") as? List<*>)
                    ?.mapNotNull { it as? String }
                    .orEmpty()
                if (studentId !in existingIds) continue
                backfillUpdates += doc.reference to mapOf(
                    "approvedStudentIds" to FieldValue.arrayRemove(studentId),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            }
            commitBackfillOpsSafely(backfillUpdates)
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
        val cleanTitle = clampMinMax(title, "QCM SVT", 2, 200)
        val cleanDesc = description.trim().take(1000)
        val cleanChapter = chapterSvt.trim().ifBlank { "SVT" }.take(150)
        val cleanNotifTitle = notifTitle.trim().ifBlank { cleanTitle }.take(200)
        val cleanNotifBody = notifBody.trim().ifBlank { cleanTitle }.take(1000)
        val questionMaps = questions.take(20).map { q ->
            buildMap<String, Any> {
                put("questionText", q.questionText.trim())
                put("options", q.options.map { it.trim() })
                put("correctIndex", q.correctIndex)
                put("explanation", q.explanation.trim())
                val cleanImg = q.imageUrl.trim()
                if (cleanImg.isNotBlank()) {
                    put("imageUrl", cleanImg)
                }
            }
        }
        val payload = mapOf(
            "classId" to classId,
            "teacherId" to uid,
            "title" to cleanTitle,
            "description" to cleanDesc,
            "chapterSvt" to cleanChapter,
            "questions" to questionMaps,
            "approvedStudentIds" to approvedStudentIds.distinct().take(100),
            "isPublished" to true,
            "isDownloadable" to isDownloadable,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            val ops = mutableListOf<(WriteBatch) -> Unit>()
            ops += { batch -> batch.set(quizRef, payload) }
            for (studentId in approvedStudentIds.distinct()) {
                val notifRef = db.collection("student_notifications").document()
                ops += { batch ->
                    batch.set(
                        notifRef,
                        mapOf(
                            "studentId" to studentId,
                            "teacherId" to uid,
                            "type" to "quiz",
                            "title" to cleanNotifTitle,
                            "body" to cleanNotifBody,
                            "isRead" to false,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            commitWriteOpsChunked(ops)
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
        val limitedQuestions = quiz.questions.take(20)
        val answersRecords = limitedQuestions.mapIndexed { index, q ->
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
        val maxScore = limitedQuestions.size.coerceIn(1, 1000).toDouble()
        val score = answersRecords.count { it.isCorrect }.toDouble().coerceIn(0.0, maxScore)
        val safeQuizTitle = quiz.title.trim().ifBlank { "QCM SVT" }.take(200)
        val safeStudentName = studentName.trim().ifBlank { "Élève" }.take(100)
        val safeDuration = totalDurationSeconds.coerceIn(0L, 86400L)

        val attemptRef = if (!attemptId.isNullOrBlank()) {
            db.collection("quiz_attempts").document(sanitizeId(attemptId, 128))
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
            "quizTitle" to safeQuizTitle,
            "classId" to quiz.classId,
            "teacherId" to quiz.teacherId,
            "studentId" to uid,
            "studentName" to safeStudentName,
            "score" to score,
            "maxScore" to maxScore,
            "totalDurationSeconds" to safeDuration,
            "answers" to answerMaps,
            "completedAt" to FieldValue.serverTimestamp(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            attemptRef.set(payload).await()
            return QuizAttempt(
                id = attemptRef.id,
                quizId = quiz.id,
                quizTitle = safeQuizTitle,
                classId = quiz.classId,
                teacherId = quiz.teacherId,
                studentId = uid,
                studentName = safeStudentName,
                score = score,
                maxScore = maxScore,
                totalDurationSeconds = safeDuration,
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
        val safeAttemptId = sanitizeId(attemptId, 128)
        val attemptRef = db.collection("quiz_attempts").document(safeAttemptId)
        val safeQuizTitle = quizTitle.trim().ifBlank { "QCM SVT" }.take(200)
        val safeStudentName = studentName.trim().ifBlank { "Élève" }.take(100)
        val safeMaxScore = maxScore.coerceIn(1.0, 1000.0)
        val safeScore = score.coerceIn(0.0, safeMaxScore)
        val safeDuration = totalDurationSeconds.coerceIn(0L, 86400L)
        val safeAnswers = answers.take(20)
        try {
            val existing = runCatching { attemptRef.get().await() }.getOrNull()
            if (existing != null && existing.exists()) {
                return QuizAttempt(
                    id = safeAttemptId,
                    quizId = quizId,
                    quizTitle = safeQuizTitle,
                    classId = classId,
                    teacherId = teacherId,
                    studentId = uid,
                    studentName = safeStudentName,
                    score = safeScore,
                    maxScore = safeMaxScore,
                    totalDurationSeconds = safeDuration,
                    answers = safeAnswers
                )
            }
            val answerMaps = safeAnswers.map { rec ->
                mapOf(
                    "questionIndex" to rec.questionIndex,
                    "selectedIndex" to rec.selectedIndex,
                    "isCorrect" to rec.isCorrect,
                    "timeSpentSeconds" to rec.timeSpentSeconds
                )
            }
            val payload = mapOf(
                "quizId" to quizId,
                "quizTitle" to safeQuizTitle,
                "classId" to classId,
                "teacherId" to teacherId,
                "studentId" to uid,
                "studentName" to safeStudentName,
                "score" to safeScore,
                "maxScore" to safeMaxScore,
                "totalDurationSeconds" to safeDuration,
                "answers" to answerMaps,
                "completedAt" to FieldValue.serverTimestamp(),
                "createdAt" to FieldValue.serverTimestamp()
            )
            attemptRef.set(payload).await()
            return QuizAttempt(
                id = safeAttemptId,
                quizId = quizId,
                quizTitle = safeQuizTitle,
                classId = classId,
                teacherId = teacherId,
                studentId = uid,
                studentName = safeStudentName,
                score = safeScore,
                maxScore = safeMaxScore,
                totalDurationSeconds = safeDuration,
                answers = safeAnswers
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
        val cleanTitle = clampMinMax(title, "Ressource SVT", 2, 200)
        val cleanDesc = description.trim().take(1000)
        val cleanUrl = fileUrl.trim().take(1000)
        val cleanNotifTitle = notifTitle.trim().ifBlank { cleanTitle }.take(200)
        val cleanNotifBody = notifBody.trim().ifBlank { cleanTitle }.take(1000)
        val payload = mapOf(
            "classId" to classId,
            "teacherId" to uid,
            "title" to cleanTitle,
            "description" to cleanDesc,
            "type" to type,
            "fileUrl" to cleanUrl,
            "durationSeconds" to durationSeconds.coerceIn(0L, 86400L),
            "approvedStudentIds" to approvedStudentIds.distinct().take(100),
            "isDownloadable" to isDownloadable,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            val ops = mutableListOf<(WriteBatch) -> Unit>()
            ops += { batch -> batch.set(resRef, payload) }
            for (studentId in approvedStudentIds.distinct()) {
                val notifRef = db.collection("student_notifications").document()
                ops += { batch ->
                    batch.set(
                        notifRef,
                        mapOf(
                            "studentId" to studentId,
                            "teacherId" to uid,
                            "type" to "resource",
                            "title" to cleanNotifTitle,
                            "body" to cleanNotifBody,
                            "isRead" to false,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            commitWriteOpsChunked(ops)
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
            val cleanTitle = clampMinMax(title, "Ressource SVT", 2, 200)
            val cleanDesc = description.trim().take(1000)
            val cleanNotifTitle = notifTitle.trim().ifBlank { cleanTitle }.take(200)
            val cleanNotifBody = notifBody.trim().ifBlank { cleanTitle }.take(1000)
            val payload = mapOf(
                "classId" to classId,
                "teacherId" to uid,
                "title" to cleanTitle,
                "description" to cleanDesc,
                "type" to type,
                "fileUrl" to chunkSchemeUrl.take(1000),
                "durationSeconds" to durationSeconds.coerceIn(0L, 86400L),
                "approvedStudentIds" to approvedStudentIds.distinct().take(100),
                "isDownloadable" to isDownloadable,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val ops = mutableListOf<(WriteBatch) -> Unit>()
            ops += { batch -> batch.set(resRef, payload) }
            for (studentId in approvedStudentIds.distinct()) {
                val notifRef = db.collection("student_notifications").document()
                ops += { batch ->
                    batch.set(
                        notifRef,
                        mapOf(
                            "studentId" to studentId,
                            "teacherId" to uid,
                            "type" to "resource",
                            "title" to cleanNotifTitle,
                            "body" to cleanNotifBody,
                            "isRead" to false,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            commitWriteOpsChunked(ops)
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
        watchedPercentOverride: Double? = null,
        knownRemoteExists: Boolean = false
    ): VideoTracking = videoTrackingMutex.withLock {
        val uid = requireUserId()
        val trackingId = "${resource.id}_$uid"
        val docRef = db.collection("video_tracking").document(trackingId)
        val safeStudentName = studentName.trim().ifBlank { "Élève" }.take(100)

        try {
            val docExists: Boolean
            val remotePrevPos: Long
            val remotePrevPct: Double
            if (knownRemoteExists && existingTracking != null) {
                docExists = true
                remotePrevPos = existingTracking.maxPositionSeconds
                remotePrevPct = existingTracking.watchedPercentage
            } else {
                val existingSnap = runCatching { docRef.get().await() }.getOrNull()
                docExists = existingSnap?.exists() == true
                remotePrevPos = if (docExists) {
                    existingSnap?.getLong("maxPositionSeconds") ?: 0L
                } else 0L
                remotePrevPct = if (docExists) {
                    existingSnap?.getDouble("watchedPercentage") ?: 0.0
                } else 0.0
            }

            val prevMax = maxOf(existingTracking?.maxPositionSeconds ?: 0L, remotePrevPos)
            val prevPct = maxOf(existingTracking?.watchedPercentage ?: 0.0, remotePrevPct)
            val metrics = computeVideoTrackingMetrics(
                positionSeconds = positionSeconds,
                totalDurationSeconds = totalDurationSeconds,
                resourceDurationSeconds = resource.durationSeconds,
                prevPositionSeconds = prevMax,
                prevPercentage = prevPct,
                watchedPercentOverride = watchedPercentOverride
            )
            val (safeDuration, maxPos, pct, status) = metrics

            if (docExists && shouldSkipVideoTrackingSync(existingTracking, metrics)) {
                return@withLock existingTracking!!.copy(
                    studentName = safeStudentName,
                    maxPositionSeconds = maxPos,
                    durationSeconds = safeDuration,
                    watchedPercentage = pct,
                    status = status
                )
            }

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

            if (!docExists) {
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
                try {
                    docRef.update(updatePayload).await()
                } catch (e: FirebaseFirestoreException) {
                    if (e.code == FirebaseFirestoreException.Code.NOT_FOUND) {
                        docRef.set(createPayload).await()
                    } else {
                        throw e
                    }
                }
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
        studentName: String,
        existingTracking: VideoTracking? = null,
        knownRemoteExists: Boolean = false
    ): VideoTracking {
        return upsertVideoTracking(
            resource = resource,
            studentName = studentName,
            positionSeconds = 1L,
            totalDurationSeconds = 1L,
            existingTracking = existingTracking,
            watchedPercentOverride = 100.0,
            knownRemoteExists = knownRemoteExists
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
        val cleanTitle = clampMinMax(title, "Annonce SVT", 2, 200)
        val cleanContent = content.trim().ifBlank { cleanTitle }.take(4000)
        val cleanNotifTitle = notifTitle.trim().ifBlank { cleanTitle }.take(200)
        val cleanNotifBody = notifBody.trim().ifBlank { cleanContent }.take(1000)
        val payload = mapOf(
            "classId" to classId,
            "teacherId" to uid,
            "title" to cleanTitle,
            "content" to cleanContent,
            "approvedStudentIds" to approvedStudentIds.distinct().take(100),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            val ops = mutableListOf<(WriteBatch) -> Unit>()
            ops += { batch -> batch.set(annRef, payload) }
            for (studentId in approvedStudentIds.distinct()) {
                val notifRef = db.collection("student_notifications").document()
                ops += { batch ->
                    batch.set(
                        notifRef,
                        mapOf(
                            "studentId" to studentId,
                            "teacherId" to uid,
                            "type" to "announcement",
                            "title" to cleanNotifTitle,
                            "body" to cleanNotifBody,
                            "isRead" to false,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            commitWriteOpsChunked(ops)
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
        val cleanStudentName = studentName.trim().ifBlank { "Élève" }.take(100)
        val cleanContent = content.trim().take(2000)
        val payload = mapOf(
            "classId" to classId,
            "teacherId" to teacherId,
            "studentId" to studentId,
            "studentName" to cleanStudentName,
            "senderId" to uid,
            "senderRole" to senderRole,
            "content" to cleanContent,
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
                        "title" to notifTitleForStudent.trim().take(200),
                        "body" to cleanContent.take(1000).take(120),
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
            val ops: List<(WriteBatch) -> Unit> = unread.map { msg ->
                val ref = db.collection("private_messages").document(msg.id)
                return@map { batch: WriteBatch -> batch.update(ref, "isRead", true) }
            }
            commitWriteOpsChunked(ops)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "private_messages")
        }
    }

    suspend fun deletePrivateMessage(messageId: String) {
        requireUserId()
        if (messageId.isBlank()) return
        val docRef = db.collection("private_messages").document(messageId)
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
    // 7b. Teacher Custom Glossary Notions
    // ---------------------------------------------------------
    fun observeTeacherGlossaryEntries(teacherId: String): Flow<List<CustomGlossaryEntry>> = flow {
        db.collection("glossary_entries")
            .whereEqualTo("teacherId", teacherId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CustomGlossaryEntry::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.term.lowercase(Locale.getDefault()) }
            }
            .logFirestoreErrors(OperationType.LIST, "glossary_entries", teacherId)
            .collect { emit(it) }
    }

    fun observeStudentGlossaryEntries(studentId: String): Flow<List<CustomGlossaryEntry>> = flow {
        db.collection("glossary_entries")
            .whereArrayContains("approvedStudentIds", studentId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CustomGlossaryEntry::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.term.lowercase(Locale.getDefault()) }
            }
            .logFirestoreErrors(OperationType.LIST, "glossary_entries", studentId)
            .collect { emit(it) }
    }

    suspend fun createCustomGlossaryEntry(
        teacherName: String,
        domain: String,
        term: String,
        definition: String,
        keyHint: String,
        keywords: List<String>,
        approvedStudentIds: List<String>
    ): CustomGlossaryEntry {
        val uid = requireUserId()
        val entryId = "gloss_${UUID.randomUUID().toString().take(8)}"
        val cleanTeacher = teacherName.trim().ifBlank { "Professeur" }.take(100)
        val cleanDomain = domain.trim().takeIf { it in SvtClass.DEFAULT_LAB_DOMAINS } ?: "NERVOUS_MUSCULAR"
        val cleanTerm = term.trim().take(120)
        val cleanDef = definition.trim().take(1500)
        val cleanHint = keyHint.trim().take(400)
        val cleanKeywords = keywords.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(15)
        val cleanStudents = approvedStudentIds.map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(500)
        require(cleanTerm.length >= 2 && cleanDef.length >= 4) {
            "Term and definition are required"
        }
        val docRef = db.collection("glossary_entries").document(entryId)
        val payload = mapOf(
            "teacherId" to uid,
            "teacherName" to cleanTeacher,
            "domain" to cleanDomain,
            "term" to cleanTerm,
            "definition" to cleanDef,
            "keyHint" to cleanHint,
            "keywords" to cleanKeywords,
            "approvedStudentIds" to cleanStudents,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            return CustomGlossaryEntry(
                id = entryId,
                teacherId = uid,
                teacherName = cleanTeacher,
                domain = cleanDomain,
                term = cleanTerm,
                definition = cleanDef,
                keyHint = cleanHint,
                keywords = cleanKeywords,
                approvedStudentIds = cleanStudents
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun deleteCustomGlossaryEntry(entryId: String) {
        requireUserId()
        if (entryId.isBlank()) return
        val docRef = db.collection("glossary_entries").document(entryId)
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
        val cleanTitle = title.trim().ifBlank { "BioTerre SVT" }.take(200)
        val cleanBody = body.trim().ifBlank { cleanTitle }.take(1000)
        val payload = mapOf(
            "studentId" to studentId,
            "teacherId" to teacherId.trim().take(128),
            "type" to "badge",
            "title" to cleanTitle,
            "body" to cleanBody,
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
            val ops: List<(WriteBatch) -> Unit> = unread.map { notif ->
                val ref = db.collection("student_notifications").document(notif.id)
                return@map { batch: WriteBatch -> batch.update(ref, "isRead", true) }
            }
            commitWriteOpsChunked(ops)
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
        val safeDocId = sanitizeId(roster.id.ifBlank { "${roster.classId}_${roster.massarNumber}" }, 120)
        val docRef = db.collection("class_rosters").document(safeDocId)
        val cleanClassName = clampMinMax(roster.className, "Classe SVT", 2, 100)
        val cleanTeacherName = roster.teacherName.trim().ifBlank { "Professeur" }.take(100)
        val cleanQrToken = clampMinMax(roster.qrToken, "SVT000", 6, 64)
        val cleanFullName = clampMinMax(roster.fullName, "Élève SVT", 2, 100)
        val cleanMassar = clampMinMax(roster.massarNumber, "M0", 2, 40)
        val cleanProvPass = clampMinMax(roster.provisionalPassword, "Svt123", 6, 64)
        val cleanStudentUid = roster.studentUid.trim().take(128)
        val cleanQrPayload = clampMinMax(roster.qrPayload, cleanQrToken, 6, 160)
        try {
            val existing = runCatching { docRef.get().await() }.getOrNull()
            if (existing != null && existing.exists()) {
                val updates = mapOf(
                    "className" to cleanClassName,
                    "teacherName" to cleanTeacherName,
                    "qrToken" to cleanQrToken,
                    "fullName" to cleanFullName,
                    "massarNumber" to cleanMassar,
                    "provisionalPassword" to cleanProvPass,
                    "studentUid" to cleanStudentUid,
                    "qrPayload" to cleanQrPayload,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            } else {
                val payload = mapOf(
                    "classId" to roster.classId,
                    "className" to cleanClassName,
                    "teacherId" to uid,
                    "teacherName" to cleanTeacherName,
                    "qrToken" to cleanQrToken,
                    "fullName" to cleanFullName,
                    "massarNumber" to cleanMassar,
                    "provisionalPassword" to cleanProvPass,
                    "studentUid" to cleanStudentUid,
                    "qrPayload" to cleanQrPayload,
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

    suspend fun updateRosterProvisionalPassword(rosterId: String, newPassword: String) {
        requireUserId()
        require(rosterId.isNotBlank()) { "Roster ID cannot be blank" }
        require(newPassword.length >= 6) { "Password must be at least 6 characters" }
        val docRef = db.collection("class_rosters").document(rosterId)
        try {
            docRef.update(
                mapOf(
                    "provisionalPassword" to newPassword,
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
        downloadUrl: String,
        approvedStudentIds: List<String> = emptyList(),
        notifTitle: String = "",
        notifBody: String = ""
    ): String {
        val uid = requireUserId()
        val updateRef = db.collection("app_updates").document()
        val normalizedUrl = AppUpdateUrlHelper.normalizeDownloadUrl(downloadUrl).take(1000)
        val payload = mapOf(
            "classId" to classId,
            "teacherId" to uid,
            "teacherName" to teacherName.trim().ifBlank { "Professeur" }.take(100),
            "versionCode" to versionCode.coerceIn(1, 1_000_000),
            "versionName" to versionName.trim().ifBlank { "1.0" }.take(40),
            "releaseNotes" to releaseNotes.trim().take(2000),
            "downloadUrl" to normalizedUrl,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            val cleanNotifTitle = notifTitle.trim().take(200)
            val cleanNotifBody = notifBody.trim().take(1000)
            val targets = approvedStudentIds.distinct()
            if (targets.isNotEmpty() && cleanNotifTitle.isNotEmpty() && cleanNotifBody.isNotEmpty()) {
                val ops = mutableListOf<(WriteBatch) -> Unit>()
                ops += { batch -> batch.set(updateRef, payload) }
                for (studentId in targets) {
                    val notifRef = db.collection("student_notifications").document()
                    ops += { batch ->
                        batch.set(
                            notifRef,
                            mapOf(
                                "studentId" to studentId,
                                "teacherId" to uid,
                                "type" to "announcement",
                                "title" to cleanNotifTitle,
                                "body" to cleanNotifBody,
                                "isRead" to false,
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }
                commitWriteOpsChunked(ops)
            } else {
                updateRef.set(payload).await()
            }
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
        private const val FIRESTORE_BATCH_SAFE_LIMIT = 400
        private val VALID_RESOURCE_URL_REGEX =
            Regex("^(https://.+|firestore://resource_chunks/[a-zA-Z0-9_-]+.*)")
        private val INVALID_ID_CHARS_REGEX = Regex("[^a-zA-Z0-9_-]")

        internal fun sanitizeId(raw: String, maxLen: Int = 128): String {
            return raw.trim().replace(INVALID_ID_CHARS_REGEX, "_").take(maxLen)
        }

        internal fun clampMinMax(
            raw: String,
            fallback: String,
            minLen: Int,
            maxLen: Int
        ): String {
            val cleaned = raw.trim()
            val candidate = when {
                cleaned.isBlank() -> fallback
                cleaned.length < minLen -> cleaned.padEnd(minLen, '_')
                else -> cleaned
            }
            return candidate.take(maxLen)
        }

        internal fun isValidResourceUrlForRules(url: String): Boolean {
            val clean = url.trim()
            return clean.length in 1..1000 && VALID_RESOURCE_URL_REGEX.matches(clean)
        }

        private fun isDocSafeForResourceUpdate(
            doc: com.google.firebase.firestore.DocumentSnapshot
        ): Boolean {
            val fileUrl = doc.getString("fileUrl").orEmpty()
            val title = doc.getString("title").orEmpty()
            val type = doc.getString("type").orEmpty()
            val duration = doc.getLong("durationSeconds") ?: -1L
            return isValidResourceUrlForRules(fileUrl) &&
                title.length in 2..200 &&
                type in setOf("pdf", "word", "image", "video") &&
                duration in 0L..86400L
        }

        fun shouldSkipVideoTrackingSync(
            existing: VideoTracking?,
            metrics: VideoTrackingMetrics
        ): Boolean {
            if (existing == null) return false
            if (existing.status == "completed" && existing.watchedPercentage >= 100.0) return true
            if (metrics.status != existing.status) return false
            if (metrics.watchedPercentage >= 100.0 && existing.watchedPercentage < 100.0) return false
            val pctGain = metrics.watchedPercentage - existing.watchedPercentage
            val posGain = metrics.maxPositionSeconds - existing.maxPositionSeconds
            return posGain < 5L && pctGain < 5.0
        }

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
