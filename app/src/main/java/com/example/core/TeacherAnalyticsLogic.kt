package com.example.core

import com.example.data.model.Announcement
import com.example.data.model.CourseResource
import com.example.data.model.Enrollment
import com.example.data.model.PrivateMessage
import com.example.data.model.Quiz
import com.example.data.model.QuizAttempt
import com.example.data.model.RosterStudent
import com.example.data.model.SvtClass
import com.example.data.model.VideoTracking
import kotlin.math.roundToInt

enum class QuestionAnswerFilter {
    ALL,
    CORRECT,
    INCORRECT
}

data class StudentClassQuestionStats(
    val studentId: String,
    val studentName: String,
    val massarNumber: String,
    val succeededQuestions: Int,
    val failedQuestions: Int,
    val totalQuestionsAnswered: Int,
    val successRatePct: Int
)

data class ClassGlobalQuestionStats(
    val classId: String,
    val className: String,
    val totalStudents: Int,
    val activeStudentsCount: Int,
    val totalSucceededQuestions: Int,
    val totalFailedQuestions: Int,
    val totalQuestionsAnswered: Int,
    val successRatePct: Int,
    val studentStats: List<StudentClassQuestionStats>
)

data class TeacherClassSummary(
    val svtClass: SvtClass,
    val approvedStudentsCount: Int,
    val pendingEnrollmentsCount: Int,
    val quizzesCount: Int,
    val attemptsCount: Int,
    val classAveragePct: Int,
    val resourcesCount: Int,
    val totalSucceededQuestions: Int = 0,
    val totalFailedQuestions: Int = 0,
    val questionSuccessRatePct: Int = 0,
    val studentQuestionStats: List<StudentClassQuestionStats> = emptyList()
)

data class TeacherDashboardOverview(
    val totalClasses: Int,
    val totalApprovedStudents: Int,
    val totalPendingEnrollments: Int,
    val totalQuizzes: Int,
    val totalAttempts: Int,
    val globalAverageQcmPct: Int,
    val totalResources: Int,
    val totalAnnouncements: Int,
    val unreadStudentMessages: Int,
    val videoCompletionRatePct: Int,
    val classSummaries: List<TeacherClassSummary>
)

data class StudentSingleQuizResult(
    val quiz: Quiz,
    val attempt: QuizAttempt?,
    val percentage: Int?,
    val succeededQuestionsCount: Int = 0,
    val failedQuestionsCount: Int = 0
)

data class StudentQcmResultSummary(
    val studentId: String,
    val studentName: String,
    val massarNumber: String,
    val classId: String,
    val className: String,
    val quizzesTakenCount: Int,
    val totalClassQuizzesCount: Int,
    val totalScore: Double,
    val totalMaxScore: Double,
    val overallPercentage: Int,
    val totalTimeSeconds: Long,
    val perQuizResults: List<StudentSingleQuizResult>,
    val succeededQuestionsCount: Int = 0,
    val failedQuestionsCount: Int = 0,
    val totalQuestionsAnsweredCount: Int = 0
)

object TeacherAnalyticsLogic {

    fun extractAttemptQuestionCounts(
        attempt: QuizAttempt,
        quiz: Quiz? = null
    ): Pair<Int, Int> {
        if (attempt.answers.isNotEmpty()) {
            val succeeded = attempt.answers.count { it.isCorrect }
            val failed = attempt.answers.count { !it.isCorrect }
            return succeeded to failed
        }
        val totalQuestions = quiz?.questions?.size?.takeIf { it > 0 }
            ?: attempt.maxScore.roundToInt().coerceAtLeast(0)
        if (totalQuestions <= 0) return 0 to 0
        val succeeded = attempt.score.roundToInt().coerceIn(0, totalQuestions)
        val failed = (totalQuestions - succeeded).coerceAtLeast(0)
        return succeeded to failed
    }

    fun computeDashboardOverview(
        classes: List<SvtClass>,
        enrollments: List<Enrollment>,
        quizzes: List<Quiz>,
        attempts: List<QuizAttempt>,
        resources: List<CourseResource>,
        videoTrackings: List<VideoTracking>,
        announcements: List<Announcement>,
        messages: List<PrivateMessage>,
        roster: List<RosterStudent> = emptyList()
    ): TeacherDashboardOverview {
        val approvedEnrollments = enrollments.filter { it.status == "approved" }
        val pendingEnrollments = enrollments.filter { it.status == "pending" }
        val unreadStudentMessages = messages.count { it.senderRole == "student" && !it.isRead }

        val validAttemptPcts = attempts.mapNotNull { att ->
            if (att.maxScore > 0.0) ((att.score / att.maxScore) * 100.0).coerceIn(0.0, 100.0) else null
        }
        val globalAvg = if (validAttemptPcts.isEmpty()) 0 else validAttemptPcts.average().roundToInt()

        val completedTrackings = videoTrackings.count { it.status == "completed" }
        val videoCompletionPct = if (videoTrackings.isEmpty()) {
            0
        } else {
            ((completedTrackings.toDouble() / videoTrackings.size.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
        }

        val classQuestionStatsById = computeClassGlobalQuestionStats(
            classes = classes,
            enrollments = enrollments,
            roster = roster,
            quizzes = quizzes,
            attempts = attempts
        ).associateBy { it.classId }

        val summaries = classes.map { cls ->
            val clsApproved = approvedEnrollments.count { it.classId == cls.id }
            val clsPending = pendingEnrollments.count { it.classId == cls.id }
            val clsQuizzes = quizzes.count { it.classId == cls.id }
            val clsAttempts = attempts.filter { it.classId == cls.id }
            val clsPcts = clsAttempts.mapNotNull { att ->
                if (att.maxScore > 0.0) ((att.score / att.maxScore) * 100.0).coerceIn(0.0, 100.0) else null
            }
            val clsAvg = if (clsPcts.isEmpty()) 0 else clsPcts.average().roundToInt()
            val clsResources = resources.count { it.classId == cls.id }
            val clsQStats = classQuestionStatsById[cls.id]
            TeacherClassSummary(
                svtClass = cls,
                approvedStudentsCount = clsApproved,
                pendingEnrollmentsCount = clsPending,
                quizzesCount = clsQuizzes,
                attemptsCount = clsAttempts.size,
                classAveragePct = clsAvg,
                resourcesCount = clsResources,
                totalSucceededQuestions = clsQStats?.totalSucceededQuestions ?: 0,
                totalFailedQuestions = clsQStats?.totalFailedQuestions ?: 0,
                questionSuccessRatePct = clsQStats?.successRatePct ?: 0,
                studentQuestionStats = clsQStats?.studentStats ?: emptyList()
            )
        }

        return TeacherDashboardOverview(
            totalClasses = classes.size,
            totalApprovedStudents = approvedEnrollments.size,
            totalPendingEnrollments = pendingEnrollments.size,
            totalQuizzes = quizzes.size,
            totalAttempts = attempts.size,
            globalAverageQcmPct = globalAvg,
            totalResources = resources.size,
            totalAnnouncements = announcements.size,
            unreadStudentMessages = unreadStudentMessages,
            videoCompletionRatePct = videoCompletionPct,
            classSummaries = summaries
        )
    }

    fun computeClassGlobalQuestionStats(
        classes: List<SvtClass>,
        enrollments: List<Enrollment>,
        roster: List<RosterStudent>,
        quizzes: List<Quiz>,
        attempts: List<QuizAttempt>,
        filterClassId: String? = null
    ): List<ClassGlobalQuestionStats> {
        val targetClasses = if (filterClassId.isNullOrBlank()) {
            classes
        } else {
            classes.filter { it.id == filterClassId }
        }
        val studentResultsByClass = computeStudentQcmResults(
            classes = targetClasses,
            enrollments = enrollments,
            roster = roster,
            quizzes = quizzes,
            attempts = attempts,
            filterClassId = filterClassId
        ).groupBy { it.classId }

        return targetClasses.map { cls ->
            val classStudents = studentResultsByClass[cls.id].orEmpty()
            val studentStats = classStudents.map { st ->
                val answered = st.totalQuestionsAnsweredCount
                val rate = if (answered > 0) {
                    ((st.succeededQuestionsCount.toDouble() / answered.toDouble()) * 100.0)
                        .roundToInt()
                        .coerceIn(0, 100)
                } else {
                    0
                }
                StudentClassQuestionStats(
                    studentId = st.studentId,
                    studentName = st.studentName,
                    massarNumber = st.massarNumber,
                    succeededQuestions = st.succeededQuestionsCount,
                    failedQuestions = st.failedQuestionsCount,
                    totalQuestionsAnswered = answered,
                    successRatePct = rate
                )
            }.sortedWith(
                compareByDescending<StudentClassQuestionStats> { it.succeededQuestions }
                    .thenBy { it.failedQuestions }
                    .thenBy { it.studentName.lowercase() }
            )

            val totalOk = studentStats.sumOf { it.succeededQuestions }
            val totalKo = studentStats.sumOf { it.failedQuestions }
            val totalAnswered = totalOk + totalKo
            val classRate = if (totalAnswered > 0) {
                ((totalOk.toDouble() / totalAnswered.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
            } else {
                0
            }

            ClassGlobalQuestionStats(
                classId = cls.id,
                className = cls.name,
                totalStudents = studentStats.size,
                activeStudentsCount = studentStats.count { it.totalQuestionsAnswered > 0 },
                totalSucceededQuestions = totalOk,
                totalFailedQuestions = totalKo,
                totalQuestionsAnswered = totalAnswered,
                successRatePct = classRate,
                studentStats = studentStats
            )
        }
    }

    fun computeStudentQcmResults(
        classes: List<SvtClass>,
        enrollments: List<Enrollment>,
        roster: List<RosterStudent>,
        quizzes: List<Quiz>,
        attempts: List<QuizAttempt>,
        filterClassId: String? = null
    ): List<StudentQcmResultSummary> {
        val targetClasses = if (filterClassId.isNullOrBlank()) {
            classes
        } else {
            classes.filter { it.id == filterClassId }
        }

        val results = mutableListOf<StudentQcmResultSummary>()

        for (cls in targetClasses) {
            val classQuizzes = quizzes.filter { it.classId == cls.id }
            val classQuizIds = classQuizzes.map { it.id }.toSet()
            val classEnrollments = enrollments.filter { it.classId == cls.id && it.status == "approved" }
            val classRoster = roster.filter { it.classId == cls.id }
            val classAttempts = attempts.filter { it.classId == cls.id && it.quizId in classQuizIds }

            val rosterByUid = classRoster.filter { it.studentUid.isNotBlank() }.associateBy { it.studentUid }

            // Collect all known student identities in this class
            data class StudentSeed(
                val studentId: String,
                val name: String,
                val massar: String
            )

            val seeds = linkedMapOf<String, StudentSeed>()

            classEnrollments.forEach { enr ->
                val massar = enr.massarNumber.ifBlank { rosterByUid[enr.studentId]?.massarNumber.orEmpty() }
                seeds[enr.studentId] = StudentSeed(
                    studentId = enr.studentId,
                    name = enr.studentName.ifBlank { "Élève" },
                    massar = massar
                )
            }

            classAttempts.forEach { att ->
                if (!seeds.containsKey(att.studentId)) {
                    val rosterMatch = rosterByUid[att.studentId]
                    seeds[att.studentId] = StudentSeed(
                        studentId = att.studentId,
                        name = att.studentName.ifBlank { rosterMatch?.fullName ?: "Élève" },
                        massar = rosterMatch?.massarNumber.orEmpty()
                    )
                }
            }

            // Include roster students not yet claimed in enrollments under synthetic key so teacher sees whole list
            classRoster.forEach { r ->
                val uid = r.studentUid
                val alreadyExists = if (uid.isNotBlank()) {
                    seeds.containsKey(uid)
                } else {
                    seeds.values.any {
                        it.massar.isNotBlank() && it.massar.equals(r.massarNumber, ignoreCase = true)
                    }
                }
                if (!alreadyExists) {
                    val key = uid.ifBlank { "roster_${cls.id}_${r.massarNumber}" }
                    seeds[key] = StudentSeed(
                        studentId = key,
                        name = r.fullName,
                        massar = r.massarNumber
                    )
                }
            }

            for (seed in seeds.values) {
                val studentAttemptsByQuiz = classAttempts
                    .filter { it.studentId == seed.studentId }
                    .groupBy { it.quizId }
                    .mapValues { (_, list) ->
                        // Keep latest or best attempt per quiz
                        list.maxByOrNull { it.score / it.maxScore.coerceAtLeast(1.0) }
                    }

                val perQuiz = classQuizzes.map { q ->
                    val att = studentAttemptsByQuiz[q.id]
                    val pct = att?.let {
                        if (it.maxScore > 0.0) ((it.score / it.maxScore) * 100.0).roundToInt().coerceIn(0, 100)
                        else 0
                    }
                    val (qOk, qKo) = att?.let { extractAttemptQuestionCounts(it, q) } ?: (0 to 0)
                    StudentSingleQuizResult(
                        quiz = q,
                        attempt = att,
                        percentage = pct,
                        succeededQuestionsCount = qOk,
                        failedQuestionsCount = qKo
                    )
                }

                val completedAttempts = perQuiz.mapNotNull { it.attempt }
                val totalScore = completedAttempts.sumOf { it.score }
                val totalMax = completedAttempts.sumOf { it.maxScore }
                val totalTime = completedAttempts.sumOf { it.totalDurationSeconds }
                val overallPct = if (totalMax > 0.0) {
                    ((totalScore / totalMax) * 100.0).roundToInt().coerceIn(0, 100)
                } else {
                    0
                }
                val totalSucceededQuestions = perQuiz.sumOf { it.succeededQuestionsCount }
                val totalFailedQuestions = perQuiz.sumOf { it.failedQuestionsCount }

                results.add(
                    StudentQcmResultSummary(
                        studentId = seed.studentId,
                        studentName = seed.name,
                        massarNumber = seed.massar,
                        classId = cls.id,
                        className = cls.name,
                        quizzesTakenCount = completedAttempts.size,
                        totalClassQuizzesCount = classQuizzes.size,
                        totalScore = totalScore,
                        totalMaxScore = totalMax,
                        overallPercentage = overallPct,
                        totalTimeSeconds = totalTime,
                        perQuizResults = perQuiz,
                        succeededQuestionsCount = totalSucceededQuestions,
                        failedQuestionsCount = totalFailedQuestions,
                        totalQuestionsAnsweredCount = totalSucceededQuestions + totalFailedQuestions
                    )
                )
            }
        }

        return results.sortedWith(
            compareBy<StudentQcmResultSummary>(
                { it.className.lowercase() },
                { -it.overallPercentage },
                { it.studentName.lowercase() }
            )
        )
    }
}
