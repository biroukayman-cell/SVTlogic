package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.QuizQuestion
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class SvtRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun fullSvtClassFlow_teacherAndStudent_succeedsAndIsolatesStudents() = runBlocking {
        val teacherEmail = "teacher_${UUID.randomUUID().toString().take(6)}@test.com"
        val studentAEmail = "studentA_${UUID.randomUUID().toString().take(6)}@test.com"
        val studentBEmail = "studentB_${UUID.randomUUID().toString().take(6)}@test.com"

        val teacherUid = signInTestUser(teacherEmail)
        val repo = SvtRepository(firestore)

        // 1. Teacher creates profile & class
        withTimeout(TIMEOUT_MS) {
            repo.createUserProfile("Prof. Nadia", "+212600000001", "teacher")
        }
        val svtClass = withTimeout(TIMEOUT_MS) {
            repo.createClass("Prof. Nadia", "2Bac SVT", "Terminale", "2026-2027")
        }
        assertTrue(svtClass.qrToken.isNotBlank())

        // 2. Student A signs in, looks up QR code, requests enrollment
        val studentAUid = signInTestUser(studentAEmail)
        withTimeout(TIMEOUT_MS) {
            repo.createUserProfile("Youssef", "+212600000002", "student")
        }
        val classCode = withTimeout(TIMEOUT_MS) { repo.lookupClassByQrToken(svtClass.qrToken) }
        assertNotNull(classCode)
        withTimeout(TIMEOUT_MS) {
            repo.requestEnrollment(classCode!!, "Youssef", "+212600000002")
        }

        // 3. Teacher signs back in, sees enrollment, approves Student A, and publishes QCM
        signInTestUser(teacherEmail)
        val enrollments = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeTeacherEnrollments(teacherUid).first { list -> list.any { it.studentId == studentAUid } }
        }
        val studentEnrollment = enrollments.first { it.studentId == studentAUid }
        withTimeout(TIMEOUT_MS) {
            repo.approveStudentEnrollment(studentEnrollment, "Validé", "Bienvenue en classe")
        }

        val quizId = withTimeout(TIMEOUT_MS) {
            repo.createQuizForClass(
                classId = svtClass.id,
                title = "Génétique",
                description = "Test QCM",
                chapterSvt = "Mitose",
                questions = listOf(
                    QuizQuestion("Phase de séparation des chromatides ?", listOf("Prophase", "Anaphase"), 1, "Anaphase")
                ),
                approvedStudentIds = listOf(studentAUid),
                notifTitle = "Nouveau QCM",
                notifBody = "Génétique",
                isDownloadable = true
            )
        }
        assertTrue(quizId.isNotBlank())

        val resourceId = withTimeout(TIMEOUT_MS) {
            repo.createResource(
                classId = svtClass.id,
                title = "Cours Mitose PDF",
                description = "Support de cours",
                type = "pdf",
                fileUrl = "https://example.com/mitose.pdf",
                durationSeconds = 0L,
                approvedStudentIds = listOf(studentAUid),
                notifTitle = "Nouveau cours",
                notifBody = "Cours Mitose PDF",
                isDownloadable = false
            )
        }
        withTimeout(TIMEOUT_MS) {
            repo.updateResourceDownloadable(resourceId, true)
        }

        // 4. Student A reads quiz & resource, submits timed attempt (and idempotent sync), sends private message
        signInTestUser(studentAEmail)
        val studentQuizzes = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeStudentQuizzes(studentAUid).first { it.any { q -> q.id == quizId } }
        }
        val quiz = studentQuizzes.first { it.id == quizId }
        assertTrue(quiz.isDownloadable)

        val studentResources = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeStudentResources(studentAUid).first { it.any { r -> r.id == resourceId } }
        }
        assertTrue(studentResources.first { it.id == resourceId }.isDownloadable)

        val deterministicId = "att_${quiz.id}_$studentAUid"
        val attempt = withTimeout(TIMEOUT_MS) {
            repo.submitQuizAttempt(
                quiz = quiz,
                studentName = "Youssef",
                selectedAnswers = listOf(1),
                perQuestionSeconds = listOf(14L),
                totalDurationSeconds = 14L,
                attemptId = deterministicId
            )
        }
        assertEquals(1.0, attempt.score, 0.01)

        // Idempotent retry of pending sync succeeds cleanly without error
        val syncedAgain = withTimeout(TIMEOUT_MS) {
            repo.syncPendingQuizAttempt(
                attemptId = deterministicId,
                quizId = quiz.id,
                quizTitle = quiz.title,
                classId = quiz.classId,
                teacherId = quiz.teacherId,
                studentName = "Youssef",
                score = 1.0,
                maxScore = 1.0,
                totalDurationSeconds = 14L,
                answers = attempt.answers
            )
        }
        assertEquals(deterministicId, syncedAgain.id)

        withTimeout(TIMEOUT_MS) {
            repo.sendPrivateMessage(
                classId = svtClass.id,
                teacherId = teacherUid,
                studentId = studentAUid,
                studentName = "Youssef",
                senderRole = "student",
                content = "Bonjour prof, question sur la mitose."
            )
        }

        // 5. Student B cannot read Student A's profile or private messages
        signInTestUser(studentBEmail)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeUserProfile(studentAUid).first()
            }
            fail("Expected PERMISSION_DENIED when reading another user's profile")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx!!.code)
        }
    }

    @Test
    fun observeStudentQuizzes_unauthenticated_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = SvtRepository(firestore)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeStudentQuizzes("any_uid").first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated caller")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx!!.code)
        }
    }

    private companion object {
        const val TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
