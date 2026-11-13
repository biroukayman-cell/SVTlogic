package com.example.data.offline

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CourseResource
import com.example.data.model.QuestionAnswerRecord
import com.example.data.model.Quiz
import com.example.data.model.QuizQuestion
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class OfflineStorageManagerTest {

    @Test
    fun offlineDownloadsAndPendingAttempts_persistAcrossInstances() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val userId = "student_offline_test_${System.currentTimeMillis()}"

        val sampleFile = File(context.cacheDir, "sample_course.pdf").apply {
            writeText("SVT-PDF-CONTENT")
        }

        val manager1 = OfflineStorageManager(context, userId)
        val resource = CourseResource(
            id = "res_pdf_1",
            classId = "cls_1",
            teacherId = "teacher_1",
            title = "La méiose",
            description = "Cours complet",
            type = "pdf",
            fileUrl = Uri.fromFile(sampleFile).toString(),
            durationSeconds = 0L,
            isDownloadable = true
        )
        val downloaded = manager1.downloadResource(resource)
        assertTrue(File(downloaded.localFilePath).exists())

        val quiz = Quiz(
            id = "quiz_offline_1",
            classId = "cls_1",
            teacherId = "teacher_1",
            title = "QCM Génétique",
            description = "Test",
            chapterSvt = "Génétique",
            questions = listOf(
                QuizQuestion("Q1", listOf("A", "B"), 1, "B est correct")
            ),
            isDownloadable = true
        )
        manager1.saveQuizOffline(quiz)

        val pending = PendingQuizAttempt(
            attemptId = "att_quiz_offline_1_$userId",
            quizId = quiz.id,
            quizTitle = quiz.title,
            classId = quiz.classId,
            teacherId = quiz.teacherId,
            studentId = userId,
            studentName = "Salma",
            score = 1.0,
            maxScore = 1.0,
            totalDurationSeconds = 22L,
            answers = listOf(QuestionAnswerRecord(0, 1, true, 22L)),
            createdAtMillis = System.currentTimeMillis()
        )
        manager1.enqueuePendingAttempt(pending)

        // Re-create manager to verify disk persistence
        val manager2 = OfflineStorageManager(context, userId)
        assertTrue(manager2.downloadedResources.value.containsKey("res_pdf_1"))
        assertTrue(manager2.downloadedQuizzes.value.containsKey("quiz_offline_1"))
        assertEquals(1, manager2.pendingAttempts.value.size)
        assertEquals("att_quiz_offline_1_$userId", manager2.pendingAttempts.value.first().attemptId)

        // Remove after sync
        manager2.removePendingAttempt("att_quiz_offline_1_$userId")
        manager2.removeOfflineQuiz("quiz_offline_1")
        manager2.removeDownloadedResource("res_pdf_1")

        assertTrue(manager2.pendingAttempts.value.isEmpty())
        assertFalse(manager2.downloadedQuizzes.value.containsKey("quiz_offline_1"))
        assertFalse(manager2.downloadedResources.value.containsKey("res_pdf_1"))
    }
}
