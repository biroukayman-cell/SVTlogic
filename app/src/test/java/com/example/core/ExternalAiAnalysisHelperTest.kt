package com.example.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalAiAnalysisHelperTest {

    private fun sampleClassInput(
        classId: String = "c1",
        className: String = "2 Bac sciences physiques 3",
        averagePct: Int = 64
    ): ClassAiAnalysisInput = ClassAiAnalysisInput(
        classId = classId,
        className = className,
        level = "2ème Bac",
        academicYear = "2026-2027",
        totalStudentsCount = 12,
        activeQcmStudentsCount = 10,
        classAveragePct = averagePct,
        totalSucceededQuestions = 42,
        totalFailedQuestions = 18,
        questionSuccessRatePct = 70,
        strongStudentsCount = 5,
        averageStudentsCount = 4,
        strugglingStudentsCount = 1,
        inactiveStudentsCount = 2,
        quizMetrics = listOf(
            QuizClassPerformanceMetric(
                quizTitle = "QCM Respiration cellulaire",
                chapterSvt = "Consommation de la matière organique",
                attemptsCount = 10,
                averagePct = averagePct,
                succeededQuestions = 42,
                failedQuestions = 18
            )
        ),
        strongestQuestions = listOf(
            QuizQuestionPerformanceMetric(
                quizTitle = "QCM Respiration cellulaire",
                chapterSvt = "Respiration",
                questionText = "Où se déroule la glycolyse ?",
                succeededCount = 9,
                failedCount = 1,
                successRatePct = 90
            )
        ),
        weakestQuestions = listOf(
            QuizQuestionPerformanceMetric(
                quizTitle = "QCM Respiration cellulaire",
                chapterSvt = "Cycle de Krebs",
                questionText = "Quel est le bilan net en ATP de la glycolyse ?",
                succeededCount = 3,
                failedCount = 7,
                successRatePct = 30
            )
        ),
        totalResourcesCount = 4,
        videoCompletionRatePct = 75
    )

    @Test
    fun `buildClassAnalysisPrompt includes metrics, questions, and respects language instructions`() {
        val input = sampleClassInput()

        val frPrompt = ExternalAiAnalysisHelper.buildClassAnalysisPrompt(input, AppLanguage.FR)
        assertTrue(frPrompt.contains("2 Bac sciences physiques 3"))
        assertTrue(frPrompt.contains("64%"))
        assertTrue(frPrompt.contains("42 réussies / 18 échouées"))
        assertTrue(frPrompt.contains("Où se déroule la glycolyse ?"))
        assertTrue(frPrompt.contains("Quel est le bilan net en ATP de la glycolyse ?"))
        assertTrue(frPrompt.contains("français"))

        val arPrompt = ExternalAiAnalysisHelper.buildClassAnalysisPrompt(input, AppLanguage.AR)
        assertTrue(arPrompt.contains("العربية"))
        assertTrue(arPrompt.contains("2 Bac sciences physiques 3"))

        val enPrompt = ExternalAiAnalysisHelper.buildClassAnalysisPrompt(input, AppLanguage.EN)
        assertTrue(enPrompt.contains("in English"))
        assertTrue(enPrompt.contains("2 Bac sciences physiques 3"))
    }

    @Test
    fun `buildAllClassesAnalysisPrompt aggregates multiple classes and delegates single class`() {
        val first = sampleClassInput(classId = "c1", className = "2 Bac SP 1", averagePct = 80)
        val second = sampleClassInput(classId = "c2", className = "2 Bac SP 2", averagePct = 50)

        val singlePrompt = ExternalAiAnalysisHelper.buildAllClassesAnalysisPrompt(listOf(first), AppLanguage.FR)
        assertFalse(singlePrompt.contains("RÉSUMÉ GLOBAL"))
        assertTrue(singlePrompt.contains("2 Bac SP 1"))

        val multiPrompt = ExternalAiAnalysisHelper.buildAllClassesAnalysisPrompt(listOf(first, second), AppLanguage.FR)
        assertTrue(multiPrompt.contains("RÉSUMÉ GLOBAL (2 classes)"))
        assertTrue(multiPrompt.contains("CLASSE 1/2"))
        assertTrue(multiPrompt.contains("CLASSE 2/2"))
        assertTrue(multiPrompt.contains("2 Bac SP 1"))
        assertTrue(multiPrompt.contains("2 Bac SP 2"))
        assertTrue(multiPrompt.contains("Moyenne QCM inter-classes : 65%"))
        assertTrue(multiPrompt.contains("84 réussies / 36 échouées"))
    }

    @Test
    fun `buildQuizFrequentErrorsPrompt formats error breakdown and distractor frequencies`() {
        val quizInput = QuizErrorSummaryInput(
            quizId = "q1",
            quizTitle = "QCM Génétique humaine",
            chapterSvt = "Hérédité humaine",
            classId = "c1",
            className = "2 Bac SVT 1",
            level = "2ème Bac",
            totalQuestions = 5,
            attemptsCount = 15,
            averagePct = 58,
            totalSucceededAnswers = 43,
            totalFailedAnswers = 32,
            averageDurationSeconds = 110,
            questionErrorMetrics = listOf(
                QuizFrequentErrorQuestionMetric(
                    questionNumber = 3,
                    questionText = "Comment reconnaît-on un allèle récessif sur un arbre généalogique ?",
                    correctOptionText = "Des parents sains ont un enfant atteint",
                    explanation = "Un enfant malade né de deux parents sains prouve la récessivité.",
                    totalResponses = 15,
                    correctCount = 4,
                    incorrectCount = 11,
                    errorRatePct = 73,
                    commonWrongOptions = listOf(
                        QuizWrongOptionFrequency("Tout individu atteint a un parent atteint", 8),
                        QuizWrongOptionFrequency("Seuls les garçons sont touchés", 3)
                    ),
                    avgTimeSpentSeconds = 26
                )
            )
        )

        val prompt = ExternalAiAnalysisHelper.buildQuizFrequentErrorsPrompt(quizInput, AppLanguage.FR)
        assertTrue(prompt.contains("QCM Génétique humaine"))
        assertTrue(prompt.contains("Hérédité humaine"))
        assertTrue(prompt.contains("2 Bac SVT 1"))
        assertTrue(prompt.contains("Q3 (73% d'erreur : 11 fausses / 15 réponses"))
        assertTrue(prompt.contains("Des parents sains ont un enfant atteint"))
        assertTrue(prompt.contains("\"Tout individu atteint a un parent atteint\" (8x)"))
        assertTrue(prompt.contains("Un enfant malade né de deux parents sains"))
    }
}
