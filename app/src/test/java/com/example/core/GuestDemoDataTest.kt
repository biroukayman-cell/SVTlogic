package com.example.core

import com.example.data.demo.GuestDemoData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GuestDemoDataTest {

    @Test
    fun guestDemoData_providesReadOnlyNonDownloadableSampleContent() {
        assertEquals(GuestDemoData.GUEST_USER_ID, GuestDemoData.profile.userId)
        assertEquals("student", GuestDemoData.profile.role)
        assertTrue(GuestDemoData.profile.acceptedTerms)
        assertEquals(GuestDemoData.GUEST_CLASS_ID, GuestDemoData.enrollment.classId)
        assertEquals("approved", GuestDemoData.enrollment.status)

        assertTrue(GuestDemoData.quizzes.isNotEmpty())
        assertTrue(GuestDemoData.quizzes.all { !it.isDownloadable && it.questions.isNotEmpty() })

        assertTrue(GuestDemoData.resources.isNotEmpty())
        assertTrue(GuestDemoData.resources.all { !it.isDownloadable && it.fileUrl.isBlank() })

        assertTrue(GuestDemoData.announcements.isNotEmpty())
        assertTrue(GuestDemoData.messages.isNotEmpty())
        assertFalse(GuestDemoData.GUEST_USER_ID.isBlank())
    }
}
