package com.example.core

import com.example.data.model.StudentNotification
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationDrawerLogicTest {

    @Test
    fun `priorityForType maps quiz enrollment and message to HIGH`() {
        assertEquals(NotificationPriority.HIGH, NotificationDrawerLogic.priorityForType("quiz"))
        assertEquals(NotificationPriority.HIGH, NotificationDrawerLogic.priorityForType("enrollment"))
        assertEquals(NotificationPriority.HIGH, NotificationDrawerLogic.priorityForType("message"))
        assertEquals(NotificationPriority.HIGH, NotificationDrawerLogic.priorityForType(" QUIZ "))
    }

    @Test
    fun `priorityForType maps announcement and resource to MEDIUM and badge to NORMAL`() {
        assertEquals(NotificationPriority.MEDIUM, NotificationDrawerLogic.priorityForType("announcement"))
        assertEquals(NotificationPriority.MEDIUM, NotificationDrawerLogic.priorityForType("resource"))
        assertEquals(NotificationPriority.NORMAL, NotificationDrawerLogic.priorityForType("badge"))
        assertEquals(NotificationPriority.NORMAL, NotificationDrawerLogic.priorityForType("unknown"))
    }

    @Test
    fun `sortNotifications by DATE orders newest first with id tiebreaker`() {
        val oldest = StudentNotification(
            id = "n1",
            type = "quiz",
            title = "QCM 1",
            createdAt = Timestamp(1_000L, 0)
        )
        val middle = StudentNotification(
            id = "n2",
            type = "badge",
            title = "Badge",
            createdAt = Timestamp(2_000L, 0)
        )
        val newest = StudentNotification(
            id = "n3",
            type = "announcement",
            title = "Annonce",
            createdAt = Timestamp(3_000L, 0)
        )

        val sorted = NotificationDrawerLogic.sortNotifications(
            listOf(oldest, newest, middle),
            NotificationSortMode.DATE
        )

        assertEquals(listOf("n3", "n2", "n1"), sorted.map { it.id })
    }

    @Test
    fun `sortNotifications by PRIORITY orders high then medium then normal and unread first`() {
        val badgeUnread = StudentNotification(
            id = "badge_unread",
            type = "badge",
            isRead = false,
            createdAt = Timestamp(5_000L, 0)
        )
        val announcementUnread = StudentNotification(
            id = "ann_unread",
            type = "announcement",
            isRead = false,
            createdAt = Timestamp(4_000L, 0)
        )
        val quizRead = StudentNotification(
            id = "quiz_read",
            type = "quiz",
            isRead = true,
            createdAt = Timestamp(3_000L, 0)
        )
        val quizUnreadOld = StudentNotification(
            id = "quiz_unread_old",
            type = "quiz",
            isRead = false,
            createdAt = Timestamp(1_000L, 0)
        )
        val messageUnreadNew = StudentNotification(
            id = "msg_unread_new",
            type = "message",
            isRead = false,
            createdAt = Timestamp(2_000L, 0)
        )

        val sorted = NotificationDrawerLogic.sortNotifications(
            listOf(badgeUnread, quizRead, announcementUnread, quizUnreadOld, messageUnreadNew),
            NotificationSortMode.PRIORITY
        )

        assertEquals(
            listOf("msg_unread_new", "quiz_unread_old", "quiz_read", "ann_unread", "badge_unread"),
            sorted.map { it.id }
        )
    }

    @Test
    fun `detectNewlyArrivedNotificationIds ignores initial null baseline and flags new arrivals`() {
        val initialList = listOf(
            StudentNotification(id = "n1"),
            StudentNotification(id = "n2")
        )
        assertTrue(NotificationDrawerLogic.detectNewlyArrivedNotificationIds(null, initialList).isEmpty())

        val knownIds = setOf("n1", "n2")
        val updatedList = listOf(
            StudentNotification(id = "n1"),
            StudentNotification(id = "n2"),
            StudentNotification(id = "n3"),
            StudentNotification(id = "")
        )

        assertEquals(
            setOf("n3"),
            NotificationDrawerLogic.detectNewlyArrivedNotificationIds(knownIds, updatedList)
        )
    }
}
