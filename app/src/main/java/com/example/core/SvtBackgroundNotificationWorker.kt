package com.example.core

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class SvtBackgroundNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context) ?: return Result.success()
        }
        val user = Firebase.auth.currentUser ?: return Result.success()
        val uid = user.uid
        if (uid.isBlank()) return Result.success()

        return try {
            val db = FirebaseFirestore.getInstance(context.getString(R.string.firestore_database_id))
            val userDoc = db.collection("users").document(uid).get().await()
            if (!userDoc.exists()) return Result.success()

            val role = userDoc.getString("role") ?: return Result.success()
            val teacherStatus = userDoc.getString("teacherStatus") ?: "approved"
            val seenStore = SeenBackgroundNotifStore(context, uid)

            if (role == "student") {
                val snap = db.collection("student_notifications")
                    .whereEqualTo("studentId", uid)
                    .whereEqualTo("isRead", false)
                    .get()
                    .await()
                val unseen = snap.documents.filter { !seenStore.hasSeen("stu_${it.id}") }
                if (unseen.isNotEmpty()) {
                    unseen.take(3).forEachIndexed { index, doc ->
                        val title = doc.getString("title").orEmpty()
                            .ifBlank { context.getString(R.string.app_name) }
                        val body = doc.getString("body").orEmpty()
                            .ifBlank { context.getString(R.string.notifications_title) }
                        NotificationHelper.showBadgeOrAlertNotification(
                            context = context,
                            title = title,
                            body = body,
                            notificationId = ("stu_${doc.id}".hashCode() and 0x7FFFFFFF) + index
                        )
                    }
                    if (unseen.size > 3) {
                        NotificationHelper.showBadgeOrAlertNotification(
                            context = context,
                            title = context.getString(R.string.app_name),
                            body = context.getString(R.string.bg_notif_more_alerts, unseen.size - 3),
                            notificationId = 90041
                        )
                    }
                    seenStore.markSeen(unseen.map { "stu_${it.id}" })
                }
            } else if (role == "teacher") {
                val isVerifiedAdmin = user.email?.equals(OWNER_ADMIN_EMAIL, ignoreCase = true) == true &&
                    user.isEmailVerified
                if (teacherStatus == "approved" || isVerifiedAdmin) {
                    val unreadMsgsSnap = db.collection("private_messages")
                        .whereEqualTo("teacherId", uid)
                        .whereEqualTo("isRead", false)
                        .get()
                        .await()
                    val unseenStudentMsgs = unreadMsgsSnap.documents.filter { doc ->
                        doc.getString("senderRole") == "student" && !seenStore.hasSeen("msg_${doc.id}")
                    }
                    if (unseenStudentMsgs.isNotEmpty()) {
                        val latest = unseenStudentMsgs.last()
                        val studentName = latest.getString("studentName").orEmpty().ifBlank { "Élève" }
                        val summary = if (unseenStudentMsgs.size == 1) {
                            latest.getString("content").orEmpty()
                        } else {
                            context.getString(R.string.bg_notif_teacher_messages_count, unseenStudentMsgs.size)
                        }
                        NotificationHelper.showBadgeOrAlertNotification(
                            context = context,
                            title = context.getString(R.string.bg_notif_teacher_message_title, studentName),
                            body = summary,
                            notificationId = 90101
                        )
                        seenStore.markSeen(unseenStudentMsgs.map { "msg_${it.id}" })
                    }

                    val pendingEnrollSnap = db.collection("enrollments")
                        .whereEqualTo("teacherId", uid)
                        .whereEqualTo("status", "pending")
                        .get()
                        .await()
                    val unseenEnrolls = pendingEnrollSnap.documents.filter { !seenStore.hasSeen("enr_${it.id}") }
                    if (unseenEnrolls.isNotEmpty()) {
                        NotificationHelper.showBadgeOrAlertNotification(
                            context = context,
                            title = context.getString(R.string.app_name),
                            body = context.getString(R.string.bg_notif_pending_enrollments, unseenEnrolls.size),
                            notificationId = 90102
                        )
                        seenStore.markSeen(unseenEnrolls.map { "enr_${it.id}" })
                    }
                }

                if (isVerifiedAdmin) {
                    val pendingTeachersSnap = db.collection("users")
                        .whereEqualTo("role", "teacher")
                        .whereEqualTo("teacherStatus", "pending")
                        .get()
                        .await()
                    val unseenTeachers = pendingTeachersSnap.documents.filter { !seenStore.hasSeen("tch_${it.id}") }
                    if (unseenTeachers.isNotEmpty()) {
                        NotificationHelper.showBadgeOrAlertNotification(
                            context = context,
                            title = context.getString(R.string.admin_teacher_approvals_title),
                            body = context.getString(R.string.bg_notif_pending_teachers, unseenTeachers.size),
                            notificationId = 90103
                        )
                        seenStore.markSeen(unseenTeachers.map { "tch_${it.id}" })
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Background notification check skipped: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "SvtBgNotifWorker"
        const val OWNER_ADMIN_EMAIL = "biroukayman@gmail.com"
    }
}

internal class SeenBackgroundNotifStore(context: Context, uid: String) {
    private val prefs = context.applicationContext.getSharedPreferences(
        "bioterre_bg_seen_$uid",
        Context.MODE_PRIVATE
    )

    fun hasSeen(key: String): Boolean {
        val current = prefs.getStringSet(KEY_IDS, emptySet()) ?: emptySet()
        return key in current
    }

    fun markSeen(keys: List<String>) {
        if (keys.isEmpty()) return
        val existing = prefs.getStringSet(KEY_IDS, emptySet())?.toMutableList() ?: mutableListOf()
        keys.forEach { k ->
            if (k !in existing) existing.add(k)
        }
        val trimmed = if (existing.size > 300) existing.takeLast(300) else existing
        prefs.edit().putStringSet(KEY_IDS, trimmed.toSet()).apply()
    }

    companion object {
        private const val KEY_IDS = "seen_ids"
    }
}

object NotificationSyncScheduler {
    private const val PERIODIC_WORK_NAME = "bioterre_bg_notifications_periodic"
    private const val ONESHOT_WORK_NAME = "bioterre_bg_notifications_immediate"

    fun schedulePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<SvtBackgroundNotificationWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun scheduleBackgroundCheckOnLeave(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<SvtBackgroundNotificationWorker>()
            .setConstraints(constraints)
            .setInitialDelay(20, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            ONESHOT_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelAll(context: Context) {
        val wm = WorkManager.getInstance(context.applicationContext)
        wm.cancelUniqueWork(PERIODIC_WORK_NAME)
        wm.cancelUniqueWork(ONESHOT_WORK_NAME)
    }
}
