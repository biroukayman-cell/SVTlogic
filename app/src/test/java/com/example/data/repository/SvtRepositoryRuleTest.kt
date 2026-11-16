package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.QuizQuestion
import com.example.data.model.RosterStudent
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

        // 2. Student A signs in, creates profile (acceptedTerms == false), accepts terms, looks up QR code, requests enrollment
        val studentAUid = signInTestUser(studentAEmail)
        val createdStudentProfile = withTimeout(TIMEOUT_MS) {
            repo.createUserProfile(
                fullName = "Youssef",
                phoneNumber = "+212600000002",
                role = "student",
                massarNumber = "D135792468",
                birthDate = "15/09/2008"
            )
        }
        assertFalse(createdStudentProfile.acceptedTerms)
        withTimeout(TIMEOUT_MS) {
            repo.acceptTermsOfUse()
        }
        val acceptedProfile = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserProfile(studentAUid).first { it?.acceptedTerms == true }
        }
        assertTrue(acceptedProfile?.acceptedTerms == true)
        val classCode = withTimeout(TIMEOUT_MS) { repo.lookupClassByQrToken(svtClass.qrToken) }
        assertNotNull(classCode)
        withTimeout(TIMEOUT_MS) {
            repo.requestEnrollment(classCode!!, "Youssef", "+212600000002", "D135792468")
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
            repo.observeStudentQuizzes(svtClass.id).first { it.any { q -> q.id == quizId } }
        }
        val quiz = studentQuizzes.first { it.id == quizId }
        assertTrue(quiz.isDownloadable)

        val studentResources = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeStudentResources(svtClass.id).first { it.any { r -> r.id == resourceId } }
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
                repo.observeStudentQuizzes("any_class_id").first()
            }
            fail("Expected PERMISSION_DENIED for unauthenticated caller")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx!!.code)
        }
    }

    @Test
    fun classRoster_teacherProvisioningAndStudentIsolation_enforcesRules() = runBlocking {
        val teacherEmail = "roster_teacher_${UUID.randomUUID().toString().take(6)}@test.com"
        val studentAEmail = "roster_a_${UUID.randomUUID().toString().take(6)}@test.com"
        val studentBEmail = "roster_b_${UUID.randomUUID().toString().take(6)}@test.com"

        val studentAUid = signInTestUser(studentAEmail)
        val studentBUid = signInTestUser(studentBEmail)
        val teacherUid = signInTestUser(teacherEmail)
        val repo = SvtRepository(firestore)

        withTimeout(TIMEOUT_MS) {
            repo.createUserProfile(
                fullName = "Prof. Salma",
                phoneNumber = "",
                role = "teacher",
                email = teacherEmail
            )
        }
        val svtClass = withTimeout(TIMEOUT_MS) {
            repo.createClass("Prof. Salma", "1Bac SE", "1Bac", "2026-2027")
        }

        val savedRoster = withTimeout(TIMEOUT_MS) {
            repo.saveRosterStudent(
                RosterStudent(
                    id = "${svtClass.id}_K112233445",
                    classId = svtClass.id,
                    className = svtClass.name,
                    teacherId = teacherUid,
                    teacherName = "Prof. Salma",
                    qrToken = svtClass.qrToken,
                    fullName = "Amine El Idrissi",
                    massarNumber = "K112233445",
                    provisionalPassword = "Svt7x9k2",
                    studentUid = studentAUid,
                    qrPayload = "BIOTERRE:${svtClass.qrToken}|K112233445"
                )
            )
        }

        val teacherRosterList = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeTeacherRoster(teacherUid).first { it.any { r -> r.id == savedRoster.id } }
        }
        assertEquals(1, teacherRosterList.count { it.id == savedRoster.id })

        // Student A can read own assigned roster entry
        signInTestUser(studentAEmail)
        val ownRoster = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeStudentRoster(studentAUid).first { it != null }
        }
        assertEquals("K112233445", ownRoster?.massarNumber)

        // Student B cannot read Student A's roster entry
        signInTestUser(studentBEmail)
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeStudentRoster(studentAUid).first()
            }
            fail("Expected PERMISSION_DENIED when student B reads student A roster")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx!!.code)
        }

        // Student A cannot delete roster entry
        signInTestUser(studentAEmail)
        try {
            withTimeout(TIMEOUT_MS) {
                repo.deleteRosterStudent(savedRoster.id)
            }
            fail("Expected PERMISSION_DENIED when student tries to delete roster entry")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertNotNull("Expected FirebaseFirestoreException, got $e", fsEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx!!.code)
        }

        // Teacher can delete roster entry
        signInTestUser(teacherEmail)
        withTimeout(TIMEOUT_MS) {
            repo.deleteRosterStudent(savedRoster.id)
        }
    }

    @Test
    fun deviceLock_and_classLabThemes_enforcedViaRepository() = runBlocking {
        val teacherEmail = "lock_teacher_${UUID.randomUUID().toString().take(6)}@test.com"
        val studentEmail = "lock_student_${UUID.randomUUID().toString().take(6)}@test.com"

        val teacherUid = signInTestUser(teacherEmail)
        val repo = SvtRepository(firestore)

        withTimeout(TIMEOUT_MS) {
            repo.createUserProfile("Prof. Nadia", "+212600000001", "teacher")
        }
        val svtClass = withTimeout(TIMEOUT_MS) {
            repo.createClass("Prof. Nadia", "2Bac Bio", "2Bac", "2026-2027")
        }
        assertEquals(7, svtClass.visibleLabDomains.size)

        withTimeout(TIMEOUT_MS) {
            repo.updateClassLabDomains(svtClass.id, listOf("IMMUNITY", "REPRODUCTION", "NERVOUS_MUSCULAR"))
        }

        // Student signs in, joins via QR auto-approval, binds device, and reads class themes
        val studentUid = signInTestUser(studentEmail)
        val studentProfile = withTimeout(TIMEOUT_MS) {
            repo.createUserProfile(
                fullName = "Salma",
                phoneNumber = "+212600000009",
                role = "student",
                boundDeviceId = "phone_device_1",
                boundDeviceModel = "Samsung A55"
            )
        }
        assertEquals("phone_device_1", studentProfile.boundDeviceId)

        // Matching device returns true; second device is rejected while bound
        assertTrue(withTimeout(TIMEOUT_MS) { repo.bindStudentDeviceIfUnbound("phone_device_1", "Samsung A55") })
        assertFalse(withTimeout(TIMEOUT_MS) { repo.bindStudentDeviceIfUnbound("phone_device_2", "Pixel 8") })

        val classCode = withTimeout(TIMEOUT_MS) { repo.lookupClassByQrToken(svtClass.qrToken) }!!
        withTimeout(TIMEOUT_MS) {
            repo.requestEnrollment(
                classCode = classCode,
                studentName = "Salma",
                studentPhone = "+212600000009",
                qrToken = svtClass.qrToken
            )
        }

        val observedClass = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeClassById(svtClass.id).first { it != null && it.visibleLabDomains.size == 3 }
        }
        assertEquals(listOf("IMMUNITY", "REPRODUCTION", "NERVOUS_MUSCULAR"), observedClass?.visibleLabDomains)

        // Teacher unlinks student's phone, then student claims new phone successfully
        signInTestUser(teacherEmail)
        assertTrue(teacherUid.isNotBlank())
        withTimeout(TIMEOUT_MS) {
            repo.resetStudentBoundDevice(studentUid)
        }

        signInTestUser(studentEmail)
        val afterReset = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserProfile(studentUid).first { it?.boundDeviceId.isNullOrEmpty() }
        }
        assertEquals("", afterReset?.boundDeviceId)
        assertTrue(withTimeout(TIMEOUT_MS) { repo.bindStudentDeviceIfUnbound("phone_device_2", "Pixel 8") })
        val rebound = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeUserProfile(studentUid).first { it?.boundDeviceId == "phone_device_2" }
        }
        assertEquals("Pixel 8", rebound?.boundDeviceModel)
    }

    private companion object {
        const val TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
