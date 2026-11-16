package com.example.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExternalAiQuizHelperTest {

    @Test
    fun buildPrompt_includesChapterLevelCountNotesAndLanguage() {
        val prompt = ExternalAiQuizHelper.buildExternalAiQuizPrompt(
            chapter = "Immunité",
            level = "2ème Bac SVT",
            questionCount = 6,
            sourceNotes = "Exercice sur la phagocytose et les lymphocytes T.",
            language = AppLanguage.FR
        )
        assertTrue(prompt.contains("Immunité"))
        assertTrue(prompt.contains("2ème Bac SVT"))
        assertTrue(prompt.contains("6 questions"))
        assertTrue(prompt.contains("Exercice sur la phagocytose"))
        assertTrue(prompt.contains("en français"))
    }

    @Test
    fun parseResponse_extractsMarkdownFencedJsonWithProse() {
        val raw = """
            Voici le QCM demandé pour votre classe de 2ème Bac SVT :

            ```json
            {
              "title": "QCM Immunité Adaptative",
              "description": "Synthèse du chapitre immunité",
              "chapterSvt": "Immunité",
              "questions": [
                {
                  "questionText": "Quelles cellules produisent les anticorps ?",
                  "options": ["Plasmocytes", "Hématies", "Plaquettes", "Neurones"],
                  "correctIndex": 0,
                  "explanation": "Les plasmocytes sont issus de la différenciation des lymphocytes B."
                },
                {
                  "questionText": "Quel marqueur caractérise les lymphocytes T cytotoxiques ?",
                  "options": ["CD4", "CD8", "IgM", "CMH-II"],
                  "correctIndex": 1,
                  "explanation": "Les LTc expriment la glycoprotéine membranaire CD8."
                }
              ]
            }
            ```

            N'hésitez pas si vous voulez plus de questions !
        """.trimIndent()

        val result = ExternalAiQuizHelper.parseExternalAiQuizResponse(raw, fallbackChapter = "SVT")
        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertEquals("QCM Immunité Adaptative", parsed.draft.title)
        assertEquals("Immunité", parsed.chapterSvt)
        assertEquals(2, parsed.draft.questions.size)
        assertEquals(0, parsed.draft.questions[0].correctIndex)
        assertEquals(1, parsed.draft.questions[1].correctIndex)
        assertEquals("Plasmocytes", parsed.draft.questions[0].options[0])
    }

    @Test
    fun parseResponse_handlesLetterAnswersAndAltKeys() {
        val raw = """
            {
              "title": "Génétique Moléculaire",
              "questions": [
                {
                  "question": "Quel codon initie généralement la traduction ?",
                  "choices": ["UAA", "UAG", "AUG", "UGA"],
                  "answer": "C",
                  "justification": "AUG code pour la méthionine."
                }
              ]
            }
        """.trimIndent()

        val result = ExternalAiQuizHelper.parseExternalAiQuizResponse(raw, fallbackChapter = "Génétique")
        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertEquals("Génétique", parsed.chapterSvt)
        assertEquals(1, parsed.draft.questions.size)
        assertEquals(2, parsed.draft.questions[0].correctIndex)
        assertEquals("AUG code pour la méthionine.", parsed.draft.questions[0].explanation)
    }

    @Test
    fun parseResponse_fallsBackToPlainTextNumberedQcm() {
        val raw = """
            # QCM Reproduction Humaine

            1. Où a lieu normalement la fécondation chez l'espèce humaine ?
            A) Dans l'utérus
            B) Dans le tiers supérieur de la trompe de Fallope
            C) Dans le col de l'utérus
            D) Dans le vagin
            Réponse : B
            Explication : La rencontre des gamètes se produit dans l'ampoule tubaire.

            Q2: Quelle hormone déclenche l'ovulation par son pic ?
            A) Progestérone
            B) LH
            C) Prolactine
            D) Ocytocine
            Réponse : B
        """.trimIndent()

        val result = ExternalAiQuizHelper.parseExternalAiQuizResponse(raw, fallbackChapter = "Reproduction")
        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertEquals("QCM Reproduction Humaine", parsed.draft.title)
        assertEquals(2, parsed.draft.questions.size)
        assertEquals(1, parsed.draft.questions[0].correctIndex)
        assertEquals(
            "La rencontre des gamètes se produit dans l'ampoule tubaire.",
            parsed.draft.questions[0].explanation
        )
        assertEquals(1, parsed.draft.questions[1].correctIndex)
    }

    @Test
    fun parseResponse_clampsMaxQuestionsToFirestoreLimit() {
        val manyQuestions = (1..25).joinToString(",") { i ->
            """{"questionText":"Question $i","options":["Opt A","Opt B","Opt C","Opt D"],"correctIndex":0}"""
        }
        val raw = """{"title":"Grand QCM","questions":[$manyQuestions]}"""

        val result = ExternalAiQuizHelper.parseExternalAiQuizResponse(raw, fallbackChapter = "Géologie")
        assertTrue(result.isSuccess)
        assertEquals(20, result.getOrThrow().draft.questions.size)
    }

    @Test
    fun parseResponse_returnsFailureOnInvalidOrEmptyText() {
        assertTrue(ExternalAiQuizHelper.parseExternalAiQuizResponse("").isFailure)
        assertTrue(ExternalAiQuizHelper.parseExternalAiQuizResponse("Bonjour, voici un texte sans QCM.").isFailure)
    }
}
