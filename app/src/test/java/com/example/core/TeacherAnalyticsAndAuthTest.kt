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

    @Test
    fun `resolveOfficialStudentName and computeStudentQcmResults always prefer official PDF roster name even if student renamed`() {
        val renamedEnrollments = listOf(
            Enrollment(
                id = "e1",
                studentId = "s1",
                classId = "c1",
                studentName = "Salma Modified",
                massarNumber = "J1001",
                status = "approved"
            ),
            Enrollment(
                id = "e_massar_only",
                studentId = "s_massar_uid",
                classId = "c2",
                studentName = "Fake Nickname",
                massarNumber = "k2002",
                status = "approved"
            )
        )
        val renamedAttempts = listOf(
            QuizAttempt(
                id = "a_ren_1",
                quizId = "q1",
                classId = "c1",
                studentId = "s1",
                studentName = "Salma Pseudo",
                score = 7.0,
                maxScore = 10.0
            ),
            QuizAttempt(
                id = "a_ren_2",
                quizId = "q3",
                classId = "c2",
                studentId = "s_massar_uid",
                studentName = "Other Pseudo",
                score = 9.0,
                maxScore = 10.0
            )
        )

        val resolvedByUid = TeacherAnalyticsLogic.resolveOfficialStudentName(
            studentId = "s1",
            classId = "c1",
            fallbackName = "Salma Pseudo",
            roster = roster,
            enrollments = renamedEnrollments
        )
        assertEquals("Salma", resolvedByUid)

        val resolvedByMassar = TeacherAnalyticsLogic.resolveOfficialStudentName(
            studentId = "s_massar_uid",
            classId = "c2",
            fallbackName = "Other Pseudo",
            roster = roster,
            enrollments = renamedEnrollments
        )
        assertEquals("Omar", resolvedByMassar)

        val results = TeacherAnalyticsLogic.computeStudentQcmResults(
            classes = classes,
            enrollments = renamedEnrollments,
            roster = roster,
            quizzes = quizzes,
            attempts = renamedAttempts
        )
        assertEquals("Salma", results.first { it.studentId == "s1" }.studentName)
        assertEquals("Omar", results.first { it.studentId == "s_massar_uid" }.studentName)
    }

    @Test
    fun `buildClassAiAnalysisInput summarizes class strengths, gaps, score tiers, and video tracking`() {
        val c1Quizzes = listOf(
            Quiz(
                id = "q_ai_1",
                classId = "c1",
                teacherId = "t1",
                title = "Respiration cellulaire",
                chapterSvt = "Consommation de la matière organique",
                questions = listOf(
                    QuizQuestion(questionText = "Rôle des mitochondries"),
                    QuizQuestion(questionText = "Bilan ATP de la fermentation")
                )
            )
        )
        val c1Attempts = listOf(
            QuizAttempt(
                id = "att_1",
                quizId = "q_ai_1",
                classId = "c1",
                studentId = "s1",
                studentName = "Salma",
                score = 2.0,
                maxScore = 2.0,
                answers = listOf(
                    QuestionAnswerRecord(questionIndex = 0, selectedIndex = 0, isCorrect = true),
                    QuestionAnswerRecord(questionIndex = 1, selectedIndex = 0, isCorrect = true)
                )
            ),
            QuizAttempt(
                id = "att_2",
                quizId = "q_ai_1",
                classId = "c1",
                studentId = "s2",
                studentName = "Youssef",
                score = 1.0,
                maxScore = 2.0,
                answers = listOf(
                    QuestionAnswerRecord(questionIndex = 0, selectedIndex = 0, isCorrect = true),
                    QuestionAnswerRecord(questionIndex = 1, selectedIndex = 1, isCorrect = false)
                )
            )
        )
        val resources = listOf(
            CourseResource(id = "r_vid", classId = "c1", type = "video"),
            CourseResource(id = "r_pdf", classId = "c1", type = "pdf")
        )
        val trackings = listOf(
            VideoTracking(id = "vt_1", resourceId = "r_vid", classId = "c1", studentId = "s1", status = "completed"),
            VideoTracking(id = "vt_2", resourceId = "r_vid", classId = "c1", studentId = "s2", status = "partial")
        )

        val input = TeacherAnalyticsLogic.buildClassAiAnalysisInput(
            svtClass = classes.first(),
            enrollments = enrollments,
            roster = roster,
            quizzes = c1Quizzes,
            attempts = c1Attempts,
            resources = resources,
            videoTrackings = trackings
        )

        assertEquals("2BAC SVT 1", input.className)
        assertEquals(2, input.totalStudentsCount)
        assertEquals(2, input.activeQcmStudentsCount)
        assertEquals(75, input.classAveragePct)
        assertEquals(3, input.totalSucceededQuestions)
        assertEquals(1, input.totalFailedQuestions)
        assertEquals(75, input.questionSuccessRatePct)
        assertEquals(1, input.strongStudentsCount)
        assertEquals(1, input.averageStudentsCount)
        assertEquals(0, input.strugglingStudentsCount)
        assertEquals(2, input.totalResourcesCount)
        assertEquals(50, input.videoCompletionRatePct)
        assertEquals("Rôle des mitochondries", input.strongestQuestions.first().questionText)
        assertEquals("Bilan ATP de la fermentation", input.weakestQuestions.first().questionText)
    }

    @Test
    fun `computeTeacherQuizDashboardSummaries and buildQuizFrequentErrorsInput rank most frequent wrong answers`() {
        val quiz = Quiz(
            id = "q_err_1",
            classId = "c1",
            teacherId = "t1",
            title = "QCM Glycémie",
            chapterSvt = "Régulation hormonale",
            questions = listOf(
                QuizQuestion(
                    questionText = "Hormone hypoglycémiante ?",
                    options = listOf("Glucagon", "Insuline", "Adrénaline", "Cortisol"),
                    correctIndex = 1,
                    explanation = "L'insuline abaisse la glycémie."
                ),
                QuizQuestion(
                    questionText = "Cellules sécrétrices d'insuline ?",
                    options = listOf("Alpha", "Bêta", "Hépatocytes", "Neurones"),
                    correctIndex = 1,
                    explanation = "Cellules bêta des îlots de Langerhans."
                )
            )
        )
        val qAttempts = listOf(
            QuizAttempt(
                id = "qa1",
                quizId = "q_err_1",
                classId = "c1",
                studentId = "s1",
                score = 1.0,
                maxScore = 2.0,
                totalDurationSeconds = 50L,
                answers = listOf(
                    QuestionAnswerRecord(questionIndex = 0, selectedIndex = 0, isCorrect = false, timeSpentSeconds = 20L),
                    QuestionAnswerRecord(questionIndex = 1, selectedIndex = 1, isCorrect = true, timeSpentSeconds = 30L)
                )
            ),
            QuizAttempt(
                id = "qa2",
                quizId = "q_err_1",
                classId = "c1",
                studentId = "s2",
                score = 0.0,
                maxScore = 2.0,
                totalDurationSeconds = 70L,
                answers = listOf(
                    QuestionAnswerRecord(questionIndex = 0, selectedIndex = 0, isCorrect = false, timeSpentSeconds = 30L),
                    QuestionAnswerRecord(questionIndex = 1, selectedIndex = 2, isCorrect = false, timeSpentSeconds = 40L)
                )
            )
        )

        val summaries = TeacherAnalyticsLogic.computeTeacherQuizDashboardSummaries(
            classes = classes,
            quizzes = listOf(quiz, quizzes.last()),
            attempts = qAttempts,
            filterClassId = "c1"
        )
        assertEquals(1, summaries.size)
        val summary = summaries.first()
        assertEquals(2, summary.attemptsCount)
        assertEquals(25, summary.averagePct)
        assertEquals(1, summary.totalSucceededAnswers)
        assertEquals(3, summary.totalFailedAnswers)
        assertEquals(75, summary.errorRatePct)

        val input = TeacherAnalyticsLogic.buildQuizFrequentErrorsInput(
            quiz = quiz,
            svtClass = classes.first(),
            attempts = qAttempts
        )
        assertEquals(2, input.attemptsCount)
        assertEquals(60, input.averageDurationSeconds)
        assertEquals(2, input.questionErrorMetrics.size)
        val hardest = input.questionErrorMetrics.first()
        assertEquals(1, hardest.questionNumber)
        assertEquals(100, hardest.errorRatePct)
        assertEquals("Glucagon", hardest.commonWrongOptions.first().optionText)
        assertEquals(2, hardest.commonWrongOptions.first().count)
    }

    @Test
    fun `filterClassesByEstablishment supports all, unassigned, and specific establishment IDs`() {
        val mixedClasses = listOf(
            SvtClass(id = "c1", teacherId = "t1", name = "2BAC A", establishmentId = "est_1", establishmentName = "Lycée Ibn Sina"),
            SvtClass(id = "c2", teacherId = "t1", name = "2BAC B", establishmentId = "est_2", establishmentName = "Lycée Al Khawarizmi"),
            SvtClass(id = "c3", teacherId = "t1", name = "1BAC C", establishmentId = "")
        )

        assertEquals(3, TeacherAnalyticsLogic.filterClassesByEstablishment(mixedClasses, null).size)
        assertEquals(3, TeacherAnalyticsLogic.filterClassesByEstablishment(mixedClasses, "  ").size)

        val est1 = TeacherAnalyticsLogic.filterClassesByEstablishment(mixedClasses, "est_1")
        assertEquals(listOf("c1"), est1.map { it.id })

        val unassigned = TeacherAnalyticsLogic.filterClassesByEstablishment(
            mixedClasses,
            TeacherAnalyticsLogic.UNASSIGNED_ESTABLISHMENT_FILTER
        )
        assertEquals(listOf("c3"), unassigned.map { it.id })
    }

    @Test
    fun `computeAdvancedMetrics computes participation rate, average quiz duration, struggling and inactive students`() {
        val advEnrollments = listOf(
            Enrollment(id = "e1", studentId = "s1", classId = "c1", studentName = "Salma", massarNumber = "J1001", status = "approved"),
            Enrollment(id = "e2", studentId = "s2", classId = "c1", studentName = "Youssef", massarNumber = "J1002", status = "approved"),
            Enrollment(id = "e3", studentId = "s3", classId = "c1", studentName = "Karim", massarNumber = "J1003", status = "approved")
        )
        val advAttempts = listOf(
            QuizAttempt(id = "a1", quizId = "q1", classId = "c1", studentId = "s1", studentName = "Salma", score = 9.0, maxScore = 10.0, totalDurationSeconds = 100L),
            QuizAttempt(id = "a2", quizId = "q1", classId = "c1", studentId = "s2", studentName = "Youssef", score = 3.0, maxScore = 10.0, totalDurationSeconds = 200L)
        )

        val metrics = TeacherAnalyticsLogic.computeAdvancedMetrics(
            classes = classes,
            enrollments = advEnrollments,
            roster = emptyList(),
            quizzes = quizzes,
            attempts = advAttempts,
            filterClassId = "c1"
        )

        // 2 active out of 3 tracked students -> 67%
        assertEquals(3, metrics.totalTrackedStudents)
        assertEquals(2, metrics.activeStudentsCount)
        assertEquals(67, metrics.participationRatePct)
        assertEquals(150, metrics.averageQuizDurationSeconds)
        assertEquals(1, metrics.strongStudentsCount)
        assertEquals(listOf("s2"), metrics.strugglingStudents.map { it.studentId })
        assertEquals(listOf("s3"), metrics.inactiveStudents.map { it.studentId })
        assertTrue(metrics.inactiveStudents.first().isInactive)
    }
}

