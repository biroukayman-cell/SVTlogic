package com.example.core

import com.example.data.model.StudentNotification
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class NotificationSortMode {
    DATE,
    PRIORITY
}

enum class NotificationPriority(val rank: Int) {
    HIGH(3),
    MEDIUM(2),
    NORMAL(1)
}

object NotificationDrawerLogic {

    fun priorityForType(type: String): NotificationPriority {
        return when (type.trim().lowercase(Locale.US)) {
            "quiz", "enrollment", "message" -> NotificationPriority.HIGH
            "announcement", "resource" -> NotificationPriority.MEDIUM
            "badge" -> NotificationPriority.NORMAL
            else -> NotificationPriority.NORMAL
        }
    }

    fun sortNotifications(
        notifications: List<StudentNotification>,
        sortMode: NotificationSortMode
    ): List<StudentNotification> {
        return when (sortMode) {
            NotificationSortMode.DATE -> {
                notifications.sortedWith(
                    compareByDescending<StudentNotification> { timestampNanos(it.createdAt) }
                        .thenByDescending { it.id }
                )
            }

            NotificationSortMode.PRIORITY -> {
                notifications.sortedWith(
                    compareByDescending<StudentNotification> { priorityForType(it.type).rank }
                        .thenBy { if (it.isRead) 1 else 0 }
                        .thenByDescending { timestampNanos(it.createdAt) }
                        .thenByDescending { it.id }
                )
            }
        }
    }

    fun detectNewlyArrivedNotificationIds(
        knownIds: Set<String>?,
        currentNotifications: List<StudentNotification>
    ): Set<String> {
        if (knownIds == null) return emptySet()
        return currentNotifications.asSequence()
            .map { it.id }
            .filter { it.isNotBlank() && it !in knownIds }
            .toSet()
    }

    fun formatTimestamp(timestamp: Timestamp?): String {
        val date = timestamp?.toDate() ?: return ""
        return SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(date)
    }

    private fun timestampNanos(timestamp: Timestamp?): Long {
        if (timestamp == null) return 0L
        return timestamp.seconds * 1_000_000_000L + timestamp.nanoseconds.toLong()
    }
}
