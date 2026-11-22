package com.example.core

import com.example.data.model.Announcement
import com.example.data.model.CourseResource
import com.example.data.model.Enrollment
import com.example.data.model.PrivateMessage
import com.example.data.model.QuestionAnswerRecord
import com.example.data.model.Quiz
import com.example.data.model.QuizAttempt
import com.example.data.model.QuizQuestion
import com.example.data.model.RosterStudent
import com.example.data.model.SvtClass
import com.example.data.model.VideoTracking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TeacherAnalyticsAndAuthTest {

    private val classes = listOf(
        SvtClass(id = "c1", teacherId = "t1", name = "2BAC SVT 1"),
        SvtClass(id = "c2", teacherId = "t1", name = "1BAC EX 2")
    )

    private val enrollments = listOf(
        Enrollment(id = "e1", studentId = "s1", classId = "c1", studentName = "Salma", massarNumber = "J1001", qrToken = "QR_TOKEN_1", status = "approved"),
        Enrollment(id = "e2", studentId = "s2", classId = "c1", studentName = "Youssef", massarNumber = "J1002", status = "approved"),
        Enrollment(id = "e3", studentId = "s3", classId = "c2", studentName = "Amina", massarNumber = "K2001", status = "pending")
    )

    private val roster = listOf(
        RosterStudent(id = "r1", classId = "c1", fullName = "Salma", massarNumber = "J1001", studentUid = "s1", qrToken = "QR_TOKEN_1"),
        RosterStudent(id = "r2", classId = "c2", fullName = "Omar", massarNumber = "K2002", studentUid = "", qrToken = "QR_TOKEN_2")
    )

    private val quizzes = listOf(
        Quiz(id = "q1", classId = "c1", teacherId = "t1", title = "QCM Respiration"),
        Quiz(id = "q2", classId = "c1", teacherId = "t1", title = "QCM Génétique"),
        Quiz(id = "q3", classId = "c2", teacherId = "t1", title = "QCM Géologie")
    )

    private val attempts = listOf(
        QuizAttempt(id = "a1", quizId = "q1", quizTitle = "QCM Respiration", classId = "c1", studentId = "s1", studentName = "Salma", score = 8.0, maxScore = 10.0, totalDurationSeconds = 120L),
        QuizAttempt(id = "a2", quizId = "q2", quizTitle = "QCM Génétique", classId = "c1", studentId = "s1", studentName = "Salma", score = 10.0, maxScore = 10.0, totalDurationSeconds = 95L),
        QuizAttempt(id = "a3", quizId = "q1", quizTitle = "QCM Respiration", classId = "c1", studentId = "s2", studentName = "Youssef", score = 5.0, maxScore = 10.0, totalDurationSeconds = 140L)
    )

    @Test
    fun `computeDashboardOverview aggregates classes, enrollments, attempts, videos, and messages`() {
        val resources = listOf(
            CourseResource(id = "res1", classId = "c1", type = "video"),
            CourseResource(id = "res2", classId = "c2", type = "pdf")
        )
        val trackings = listOf(
            VideoTracking(id = "vt1", resourceId = "res1", classId = "c1", studentId = "s1", status = "completed"),
            VideoTracking(id = "vt2", resourceId = "res1", classId = "c1", studentId = "s2", status = "partial")
        )
        val announcements = listOf(
            Announcement(id = "ann1", classId = "c1", title = "Devoir")
        )
        val messages = listOf(
            PrivateMessage(id = "m1", classId = "c1", senderRole = "student", isRead = false),
            PrivateMessage(id = "m2", classId = "c1", senderRole = "student", isRead = true),
            PrivateMessage(id = "m3", classId = "c1", senderRole = "teacher", isRead = false)
        )

        val overview = TeacherAnalyticsLogic.computeDashboardOverview(
            classes = classes,
            enrollments = enrollments,
            quizzes = quizzes,
            attempts = attempts,
            resources = resources,
            videoTrackings = trackings,
            announcements = announcements,
            messages = messages,
            roster = roster
        )

        assertEquals(2, overview.totalClasses)
        assertEquals(2, overview.totalApprovedStudents)
        assertEquals(1, overview.totalPendingEnrollments)
        assertEquals(3, overview.totalQuizzes)
        assertEquals(3, overview.totalAttempts)
        // Attempts: 80%, 100%, 50% -> avg 76.67% -> 77
        assertEquals(77, overview.globalAverageQcmPct)
        assertEquals(2, overview.totalResources)
        assertEquals(1, overview.totalAnnouncements)
        assertEquals(1, overview.unreadStudentMessages)
        assertEquals(50, overview.videoCompletionRatePct)

        val c1Summary = overview.classSummaries.first { it.svtClass.id == "c1" }
        assertEquals(2, c1Summary.approvedStudentsCount)
        assertEquals(2, c1Summary.quizzesCount)
        assertEquals(3, c1Summary.attemptsCount)
        assertEquals(77, c1Summary.classAveragePct)
        // Salma: 18 ok / 2 ko; Youssef: 5 ok / 5 ko -> total 23 ok / 7 ko (77%)
        assertEquals(23, c1Summary.totalSucceededQuestions)
        assertEquals(7, c1Summary.totalFailedQuestions)
        assertEquals(77, c1Summary.questionSuccessRatePct)
    }

    @Test
    fun `computeStudentQcmResults calculates per-student overall percentage and filters by class`() {
        val allResults = TeacherAnalyticsLogic.computeStudentQcmResults(
            classes = classes,
            enrollments = enrollments,
            roster = roster,
            quizzes = quizzes,
            attempts = attempts,
            filterClassId = null
        )

        // Salma (s1 in c1), Youssef (s2 in c1), and Omar (unclaimed roster in c2)
        assertEquals(3, allResults.size)

        val salma = allResults.first { it.studentId == "s1" }
        assertEquals(2, salma.quizzesTakenCount)
        assertEquals(2, salma.totalClassQuizzesCount)
        assertEquals(18.0, salma.totalScore, 0.01)
        assertEquals(20.0, salma.totalMaxScore, 0.01)
        assertEquals(90, salma.overallPercentage)
        assertEquals(215L, salma.totalTimeSeconds)
        assertEquals(18, salma.succeededQuestionsCount)
        assertEquals(2, salma.failedQuestionsCount)
        assertEquals(20, salma.totalQuestionsAnsweredCount)

        val youssef = allResults.first { it.studentId == "s2" }
        assertEquals(1, youssef.quizzesTakenCount)
        assertEquals(2, youssef.totalClassQuizzesCount)
        assertEquals(50, youssef.overallPercentage)
        assertEquals(5, youssef.succeededQuestionsCount)
        assertEquals(5, youssef.failedQuestionsCount)

        val omar = allResults.first { it.massarNumber == "K2002" }
        assertEquals(0, omar.quizzesTakenCount)
        assertEquals(0, omar.overallPercentage)
        assertEquals(0, omar.succeededQuestionsCount)
        assertEquals(0, omar.failedQuestionsCount)

        val c2Only = TeacherAnalyticsLogic.computeStudentQcmResults(
            classes = classes,
            enrollments = enrollments,
            roster = roster,
            quizzes = quizzes,
            attempts = attempts,
            filterClassId = "c2"
        )
        assertEquals(1, c2Only.size)
        assertEquals("c2", c2Only.first().classId)
    }

    @Test
    fun `computeClassGlobalQuestionStats aggregates succeeded and failed questions from answer records`() {
        val detailedAttempts = listOf(
            QuizAttempt(
                id = "a_det_1",
                quizId = "q1",
                classId = "c1",
                studentId = "s1",
                studentName = "Salma",
                score = 2.0,
                maxScore = 3.0,
                answers = listOf(
                    QuestionAnswerRecord(questionIndex = 0, selectedIndex = 0, isCorrect = true),
                    QuestionAnswerRecord(questionIndex = 1, selectedIndex = 1, isCorrect = false),
                    QuestionAnswerRecord(questionIndex = 2, selectedIndex = 2, isCorrect = true)
                )
            ),
            QuizAttempt(
                id = "a_det_2",
                quizId = "q1",
                classId = "c1",
                studentId = "s2",
                studentName = "Youssef",
                score = 1.0,
                maxScore = 3.0,
                answers = listOf(
                    QuestionAnswerRecord(questionIndex = 0, selectedIndex = 2, isCorrect = false),
                    QuestionAnswerRecord(questionIndex = 1, selectedIndex = 0, isCorrect = true),
                    QuestionAnswerRecord(questionIndex = 2, selectedIndex = 1, isCorrect = false)
                )
            )
        )

        val classStats = TeacherAnalyticsLogic.computeClassGlobalQuestionStats(
            classes = classes,
            enrollments = enrollments,
            roster = roster,
            quizzes = quizzes,
            attempts = detailedAttempts,
            filterClassId = "c1"
        )

        assertEquals(1, classStats.size)
        val c1 = classStats.first()
        assertEquals("c1", c1.classId)
        assertEquals(2, c1.totalStudents)
        assertEquals(2, c1.activeStudentsCount)
        assertEquals(3, c1.totalSucceededQuestions)
        assertEquals(3, c1.totalFailedQuestions)
        assertEquals(6, c1.totalQuestionsAnswered)
        assertEquals(50, c1.successRatePct)

        val salmaStats = c1.studentStats.first { it.studentId == "s1" }
        assertEquals(2, salmaStats.succeededQuestions)
        assertEquals(1, salmaStats.failedQuestions)
        assertEquals(67, salmaStats.successRatePct)

        val youssefStats = c1.studentStats.first { it.studentId == "s2" }
        assertEquals(1, youssefStats.succeededQuestions)
        assertEquals(2, youssefStats.failedQuestions)
        assertEquals(33, youssefStats.successRatePct)
    }

    @Test
    fun `defaultStudentTaalimEmail formats massar numbers and skips blank or QR prefixes`() {
        assertEquals("j123456@taalim.ma", AuthSessionDraft.defaultStudentTaalimEmail("J123456"))
        assertEquals("m987654321@taalim.ma", AuthSessionDraft.defaultStudentTaalimEmail("  m987654321 "))
        assertEquals("", AuthSessionDraft.defaultStudentTaalimEmail(""))
        assertEquals("", AuthSessionDraft.defaultStudentTaalimEmail("QR12345"))
    }

    @Test
    fun `QuizQuestion defaults imageUrl to empty and preserves attached image`() {
        val plain = QuizQuestion(questionText = "Question 1", options = listOf("A", "B"))
        assertEquals("", plain.imageUrl)

        val withImage = plain.copy(imageUrl = "data:image/jpeg;base64,/9j/4AAQ")
        assertTrue(withImage.imageUrl.startsWith("data:image/jpeg;base64,"))
    }
}
