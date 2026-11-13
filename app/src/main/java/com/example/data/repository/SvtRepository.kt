package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.badges.BadgeEngine
import com.example.data.badges.SvtBadgeDefinition
import com.example.data.model.Announcement
import com.example.data.model.ClassCode
import com.example.data.model.CourseResource
import com.example.data.model.Enrollment
import com.example.data.model.PrivateMessage
import com.example.data.model.QuestionAnswerRecord
import com.example.data.model.Quiz
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizQuestion
import com.example.data.model.StudentNotification
import com.example.data.model.SvtClass
import com.example.data.model.UserProfile
import com.example.data.model.VideoTracking
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class SvtRepository(private val db: FirebaseFirestore) {

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
            .logFirestoreErrors(OperationType.GET, path, userId)
            .collect { emit(it) }
    }

    suspend fun createUserProfile(
        fullName: String,
        phoneNumber: String,
        role: String,
        avatarUrl: String? = null
    ): UserProfile {
        val uid = requireUserId()
        val today = todayDateString()
        val docRef = db.collection("users").document(uid)
        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "fullName" to fullName.trim(),
            "phoneNumber" to phoneNumber.trim(),
            "role" to role,
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
        try {
            docRef.set(payload).await()
            return UserProfile(
                id = uid,
                userId = uid,
                fullName = fullName.trim(),
                phoneNumber = phoneNumber.trim(),
                role = role,
                avatarUrl = avatarUrl,
                xp = 0L,
                loginStreak = 1L,
                lastLoginDate = today,
                unlockedBadgeIds = emptyList()
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateProfileInfo(
        fullName: String,
        phoneNumber: String,
        avatarUrl: String?
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
        try {
            docRef.update(updates).await()
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
                qrToken = qrToken
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, classRef.path)
            throw e
        }
    }

    suspend fun lookupClassByQrToken(qrToken: String): ClassCode? {
        requireUserId()
        val clean = qrToken.trim()
        if (clean.isEmpty()) return null
        val docRef = db.collection("class_codes").document(clean)
        return try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                snap.toObject(ClassCode::class.java, ServerTimestampBehavior.ESTIMATE)
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
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
                "quizzes",
                "quiz_attempts",
                "resources",
                "video_tracking",
                "announcements",
                "private_messages"
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
        studentPhone: String
    ) {
        val uid = requireUserId()
        val docRef = db.collection("enrollments").document(uid)
        val existing = try {
            docRef.get().await()
        } catch (_: Exception) {
            null
        }
        val payload = mapOf(
            "studentId" to uid,
            "classId" to classCode.classId,
            "className" to classCode.className,
            "teacherId" to classCode.teacherId,
            "teacherName" to classCode.teacherName,
            "studentName" to studentName.trim(),
            "studentPhone" to studentPhone.trim(),
            "status" to "pending",
            "requestedAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            if (existing != null && existing.exists()) {
                docRef.delete().await()
            }
            docRef.set(payload).await()
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

    fun observeStudentQuizzes(studentId: String): Flow<List<Quiz>> = flow {
        db.collection("quizzes")
            .whereArrayContains("approvedStudentIds", studentId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Quiz::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .logFirestoreErrors(OperationType.LIST, "quizzes", studentId)
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

    fun observeStudentResources(studentId: String): Flow<List<CourseResource>> = flow {
        db.collection("resources")
            .whereArrayContains("approvedStudentIds", studentId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CourseResource::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .logFirestoreErrors(OperationType.LIST, "resources", studentId)
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
        existingTracking: VideoTracking?
    ): VideoTracking {
        val uid = requireUserId()
        val trackingId = "${resource.id}_$uid"
        val docRef = db.collection("video_tracking").document(trackingId)
        val safeDuration = totalDurationSeconds.coerceAtLeast(resource.durationSeconds).coerceAtLeast(1L)
        val prevMax = existingTracking?.maxPositionSeconds ?: 0L
        val maxPos = maxOf(prevMax, positionSeconds).coerceIn(0L, safeDuration)
        val pct = ((maxPos.toDouble() / safeDuration.toDouble()) * 100.0).coerceIn(0.0, 100.0)
        val status = when {
            pct >= 90.0 -> "completed"
            pct > 0.0 -> "partial"
            else -> "not_started"
        }

        try {
            val existingSnap = docRef.get().await()
            if (!existingSnap.exists()) {
                val createPayload = mapOf(
                    "resourceId" to resource.id,
                    "classId" to resource.classId,
                    "teacherId" to resource.teacherId,
                    "studentId" to uid,
                    "studentName" to studentName,
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
                    "studentName" to studentName,
                    "maxPositionSeconds" to maxPos,
                    "durationSeconds" to safeDuration,
                    "watchedPercentage" to pct,
                    "status" to status,
                    "lastWatchedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updatePayload).await()
            }
            return VideoTracking(
                id = trackingId,
                resourceId = resource.id,
                classId = resource.classId,
                teacherId = resource.teacherId,
                studentId = uid,
                studentName = studentName,
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

    fun observeStudentAnnouncements(studentId: String): Flow<List<Announcement>> = flow {
        db.collection("announcements")
            .whereArrayContains("approvedStudentIds", studentId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Announcement::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .logFirestoreErrors(OperationType.LIST, "announcements", studentId)
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

    private fun todayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    private fun yesterdayDateString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date(System.currentTimeMillis() - 86_400_000L))
    }
}
