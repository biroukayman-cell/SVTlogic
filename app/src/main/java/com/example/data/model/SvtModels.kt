package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val role: String = "student", // "teacher" or "student"
    val avatarUrl: String? = null,
    val xp: Long = 0L,
    val loginStreak: Long = 1L,
    val lastLoginDate: String = "",
    val unlockedBadgeIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SvtClass(
    @DocumentId val id: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val name: String = "",
    val academicYear: String = "",
    val level: String = "",
    val qrToken: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ClassCode(
    @DocumentId val id: String = "",
    val classId: String = "",
    val className: String = "",
    val academicYear: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Enrollment(
    @DocumentId val id: String = "",
    val studentId: String = "",
    val classId: String = "",
    val className: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val studentName: String = "",
    val studentPhone: String = "",
    val status: String = "pending", // "pending", "approved", "rejected"
    @ServerTimestamp val requestedAt: Timestamp? = null,
    @ServerTimestamp val validatedAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class QuizQuestion(
    val questionText: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0,
    val explanation: String = ""
)

@IgnoreExtraProperties
data class Quiz(
    @DocumentId val id: String = "",
    val classId: String = "",
    val teacherId: String = "",
    val title: String = "",
    val description: String = "",
    val chapterSvt: String = "",
    val questions: List<QuizQuestion> = emptyList(),
    val approvedStudentIds: List<String> = emptyList(),
    @get:PropertyName("isPublished") @field:PropertyName("isPublished")
    val isPublished: Boolean = true,
    @get:PropertyName("isDownloadable") @field:PropertyName("isDownloadable")
    val isDownloadable: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class QuestionAnswerRecord(
    val questionIndex: Int = 0,
    val selectedIndex: Int = -1,
    @get:PropertyName("isCorrect") @field:PropertyName("isCorrect")
    val isCorrect: Boolean = false,
    val timeSpentSeconds: Long = 0L
)

@IgnoreExtraProperties
data class QuizAttempt(
    @DocumentId val id: String = "",
    val quizId: String = "",
    val quizTitle: String = "",
    val classId: String = "",
    val teacherId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val score: Double = 0.0,
    val maxScore: Double = 1.0,
    val totalDurationSeconds: Long = 0L,
    val answers: List<QuestionAnswerRecord> = emptyList(),
    @ServerTimestamp val completedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CourseResource(
    @DocumentId val id: String = "",
    val classId: String = "",
    val teacherId: String = "",
    val title: String = "",
    val description: String = "",
    val type: String = "pdf", // "pdf", "word", "image", "video"
    val fileUrl: String = "",
    val durationSeconds: Long = 0L,
    val approvedStudentIds: List<String> = emptyList(),
    @get:PropertyName("isDownloadable") @field:PropertyName("isDownloadable")
    val isDownloadable: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class VideoTracking(
    @DocumentId val id: String = "",
    val resourceId: String = "",
    val classId: String = "",
    val teacherId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val maxPositionSeconds: Long = 0L,
    val durationSeconds: Long = 0L,
    val watchedPercentage: Double = 0.0,
    val status: String = "not_started", // "not_started", "partial", "completed"
    @ServerTimestamp val lastWatchedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val classId: String = "",
    val teacherId: String = "",
    val title: String = "",
    val content: String = "",
    val attachmentUrl: String? = null,
    val approvedStudentIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PrivateMessage(
    @DocumentId val id: String = "",
    val classId: String = "",
    val teacherId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val senderId: String = "",
    val senderRole: String = "student", // "student" or "teacher"
    val content: String = "",
    @get:PropertyName("isRead") @field:PropertyName("isRead")
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StudentNotification(
    @DocumentId val id: String = "",
    val studentId: String = "",
    val teacherId: String = "",
    val type: String = "announcement", // "quiz", "resource", "announcement", "badge", "message", "enrollment"
    val title: String = "",
    val body: String = "",
    @get:PropertyName("isRead") @field:PropertyName("isRead")
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)
