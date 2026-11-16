package com.example.core

import com.example.data.ai.GeneratedQuizDraft
import com.example.data.model.QuizQuestion
import org.json.JSONArray
import org.json.JSONObject

data class ExternalAiParsedQuiz(
    val draft: GeneratedQuizDraft,
    val chapterSvt: String
)

object ExternalAiQuizHelper {

    private const val MAX_QUESTIONS = 20
    private const val MAX_TITLE_LEN = 200
    private const val MAX_DESC_LEN = 1000
    private const val MAX_CHAPTER_LEN = 150

    fun buildExternalAiQuizPrompt(
        chapter: String,
        level: String,
        questionCount: Int,
        sourceNotes: String,
        language: AppLanguage
    ): String {
        val clampedCount = questionCount.coerceIn(1, MAX_QUESTIONS)
        val cleanChapter = chapter.trim().ifBlank { "SVT" }
        val cleanLevel = level.trim().ifBlank { "2ème Bac SVT" }
        val cleanNotes = sourceNotes.trim()

        val languageDirective = when (language) {
            AppLanguage.FR -> "Rédige toutes les questions, options, consignes et explications uniquement en français."
            AppLanguage.EN -> "Write all questions, options, instructions, and explanations strictly in English."
            AppLanguage.AR -> "اكتب جميع الأسئلة والاختيارات والتعليمات والتفسيرات باللغة العربية الفصحى العلمية."
        }

        val sourceBlock = if (cleanNotes.isNotBlank()) {
            """
            |
            |Texte source / exercice à synthétiser en QCM :
            |\"\"\"
            |$cleanNotes
            |\"\"\"
            """.trimMargin()
        } else {
            ""
        }

        return """
            |Tu es un professeur expert en Sciences de la Vie et de la Terre (SVT).
            |Synthétise un exercice / QCM pédagogique de $clampedCount questions sur le thème suivant :
            |- Chapitre SVT : $cleanChapter
            |- Niveau scolaire : $cleanLevel
            |- Langue : $languageDirective$sourceBlock
            |
            |Consignes strictes de format pour l'import dans BioTerre SVT :
            |Réponds UNIQUEMENT avec un bloc JSON valide respectant exactement cette structure :
            |{
            |  "title": "Titre concis du QCM",
            |  "description": "Consigne courte ou résumé de l'exercice",
            |  "chapterSvt": "$cleanChapter",
            |  "questions": [
            |    {
            |      "questionText": "Énoncé clair de la question SVT",
            |      "options": ["Choix A", "Choix B", "Choix C", "Choix D"],
            |      "correctIndex": 0,
            |      "explanation": "Brève justification scientifique SVT"
            |    }
            |  ]
            |}
            |Règles :
            |- Exactement $clampedCount questions (maximum 20).
            |- Chaque question comporte 4 choix plausibles et un seul "correctIndex" (entier 0, 1, 2 ou 3).
        """.trimMargin()
    }

    fun parseExternalAiQuizResponse(
        rawText: String,
        fallbackChapter: String = "SVT"
    ): Result<ExternalAiParsedQuiz> {
        val trimmed = rawText.trim()
        if (trimmed.isBlank()) {
            return Result.failure(IllegalArgumentException("Empty response text"))
        }

        val cleanFallbackChapter = fallbackChapter.trim().ifBlank { "SVT" }.take(MAX_CHAPTER_LEN)

        // 1. Attempt JSON extraction (fenced ```json or raw {...} object embedded in prose)
        val jsonCandidate = extractFirstJsonObject(trimmed)
        if (jsonCandidate != null) {
            runCatching { parseFromJson(jsonCandidate, cleanFallbackChapter) }
                .getOrNull()
                ?.let { parsed ->
                    if (parsed.draft.questions.isNotEmpty()) {
                        return Result.success(parsed)
                    }
                }
        }

        // 2. Fallback: tolerant plain-text / Markdown numbered QCM parsing
        val textParsed = parseFromPlainText(trimmed, cleanFallbackChapter)
        if (textParsed != null && textParsed.draft.questions.isNotEmpty()) {
            return Result.success(textParsed)
        }

        return Result.failure(
            IllegalArgumentException("Could not find valid QCM questions in the pasted text")
        )
    }

    internal fun extractFirstJsonObject(raw: String): String? {
        val fencedRegex = Regex("```(?:json|JSON)?\\s*([\\s\\S]*?)```")
        val fencedMatch = fencedRegex.find(raw)
        val source = fencedMatch?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() } ?: raw

        val start = source.indexOf('{')
        if (start < 0) return null

        var depth = 0
        var inString = false
        var escaped = false

        for (i in start until source.length) {
            val ch = source[i]
            if (escaped) {
                escaped = false
                continue
            }
            if (ch == '\\' && inString) {
                escaped = true
                continue
            }
            if (ch == '"') {
                inString = !inString
                continue
            }
            if (!inString) {
                if (ch == '{') depth++
                else if (ch == '}') {
                    depth--
                    if (depth == 0) {
                        return source.substring(start, i + 1)
                    }
                }
            }
        }
        return null
    }

    private fun parseFromJson(
        jsonString: String,
        fallbackChapter: String
    ): ExternalAiParsedQuiz {
        val root = JSONObject(jsonString)
        val rawChapter = root.optString("chapterSvt")
            .ifBlank { root.optString("chapter") }
            .ifBlank { root.optString("theme") }
            .trim()
            .ifBlank { fallbackChapter }
            .take(MAX_CHAPTER_LEN)

        val rawTitle = root.optString("title")
            .ifBlank { root.optString("titre") }
            .trim()
        val safeTitle = when {
            rawTitle.length >= 2 -> rawTitle.take(MAX_TITLE_LEN)
            rawChapter.length >= 2 -> "QCM : $rawChapter".take(MAX_TITLE_LEN)
            else -> "QCM SVT"
        }

        val rawDescription = root.optString("description")
            .ifBlank { root.optString("consigne") }
            .trim()
            .take(MAX_DESC_LEN)

        val questionsArray = root.optJSONArray("questions")
            ?: root.optJSONArray("items")
            ?: root.optJSONArray("qcm")
            ?: JSONArray()

        val questions = mutableListOf<QuizQuestion>()
        for (i in 0 until questionsArray.length()) {
            if (questions.size >= MAX_QUESTIONS) break
            val qObj = questionsArray.optJSONObject(i) ?: continue
            val qText = qObj.optString("questionText")
                .ifBlank { qObj.optString("question") }
                .ifBlank { qObj.optString("enonce") }
                .trim()
            if (qText.isBlank()) continue

            val optionsJson = qObj.optJSONArray("options")
                ?: qObj.optJSONArray("choices")
                ?: qObj.optJSONArray("reponses")
                ?: continue

            val options = mutableListOf<String>()
            for (oIdx in 0 until optionsJson.length()) {
                val optVal = when (val item = optionsJson.opt(oIdx)) {
                    is JSONObject -> item.optString("text").ifBlank { item.optString("label") }
                    else -> item?.toString().orEmpty()
                }.trim()
                if (optVal.isNotEmpty()) {
                    options.add(optVal)
                }
            }
            if (options.size < 2) continue

            val correctIdx = resolveCorrectIndex(qObj, options)
            val explanation = qObj.optString("explanation")
                .ifBlank { qObj.optString("explication") }
                .ifBlank { qObj.optString("justification") }
                .trim()

            questions.add(
                QuizQuestion(
                    questionText = qText,
                    options = options.take(5),
                    correctIndex = correctIdx.coerceIn(0, (options.take(5).size - 1).coerceAtLeast(0)),
                    explanation = explanation
                )
            )
        }

        return ExternalAiParsedQuiz(
            draft = GeneratedQuizDraft(
                title = safeTitle,
                description = rawDescription,
                questions = questions
            ),
            chapterSvt = rawChapter
        )
    }

    private fun resolveCorrectIndex(qObj: JSONObject, options: List<String>): Int {
        if (qObj.has("correctIndex")) {
            val raw = qObj.opt("correctIndex")
            if (raw is Number) {
                val idx = raw.toInt()
                if (idx in options.indices) return idx
            }
            if (raw is String) {
                parseAnswerToken(raw, options)?.let { return it }
            }
        }
        val altKeys = listOf("correct_index", "answerIndex", "correctAnswer", "correct", "answer", "reponse")
        for (key in altKeys) {
            if (qObj.has(key)) {
                val raw = qObj.opt(key)
                if (raw is Number) {
                    val v = raw.toInt()
                    return if (key == "correctIndex" || key == "correct_index" || key == "answerIndex") {
                        v.coerceIn(0, options.lastIndex)
                    } else {
                        if (v in 1..options.size) v - 1 else v.coerceIn(0, options.lastIndex)
                    }
                }
                if (raw is String) {
                    parseAnswerToken(raw, options)?.let { return it }
                }
            }
        }
        return 0
    }

    private fun parseAnswerToken(token: String, options: List<String>): Int? {
        val clean = token.trim().removePrefix("*").removeSuffix("*").trim()
        if (clean.isEmpty()) return null

        val intVal = clean.toIntOrNull()
        if (intVal != null) {
            return when {
                intVal in options.indices -> intVal
                intVal in 1..options.size -> intVal - 1
                else -> null
            }
        }

        val firstLetter = clean.firstOrNull()?.uppercaseChar()
        val letterIdx = when (firstLetter) {
            'A', 'أ' -> 0
            'B', 'ب' -> 1
            'C', 'ج' -> 2
            'D', 'د' -> 3
            'E' -> 4
            else -> -1
        }
        if (letterIdx in options.indices && (clean.length == 1 || clean[1] in listOf(')', '.', ':', '-', ' '))) {
            return letterIdx
        }

        val matchedIdx = options.indexOfFirst { it.equals(clean, ignoreCase = true) }
        if (matchedIdx >= 0) return matchedIdx

        return null
    }

    private fun parseFromPlainText(
        raw: String,
        fallbackChapter: String
    ): ExternalAiParsedQuiz? {
        val lines = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return null

        val titleRegex = Regex("^(?:#+\\s*|Titre\\s*[:\\-]\\s*|Title\\s*[:\\-]\\s*)(.+)$", RegexOption.IGNORE_CASE)
        val questionHeaderRegex = Regex("^(?:\\*\\*)?(?:Q(?:uestion)?\\s*\\d+[:.\\)\\-]|\\d+[.)])\\s*(.+?)(?:\\*\\*)?$", RegexOption.IGNORE_CASE)
        val optionRegex = Regex("^(?:[-*]\\s*)?(?:\\*\\*)?([A-Ea-eأبجد])[).:\\-]\\s*(.+?)(?:\\*\\*)?$")
        val answerRegex = Regex("^(?:\\*\\*)?(?:R[ée]ponse(?:\\s+correcte)?|Bonne\\s+r[ée]ponse|Answer|Correct(?:\\s+answer)?|الإجابة(?:\\s+الصحيحة)?)\\s*[:\\-]\\s*(.+)$", RegexOption.IGNORE_CASE)
        val explanationRegex = Regex("^(?:\\*\\*)?(?:Explication|Justification|Explanation|التفسير)\\s*[:\\-]\\s*(.+)$", RegexOption.IGNORE_CASE)

        var parsedTitle = ""
        val questions = mutableListOf<QuizQuestion>()

        var currentQ = ""
        val currentOptions = mutableListOf<String>()
        var currentAnswerRaw = ""
        var currentExplanation = ""

        fun flushCurrentQuestion() {
            if (currentQ.isNotBlank() && currentOptions.size >= 2 && questions.size < MAX_QUESTIONS) {
                val resolvedIdx = parseAnswerToken(currentAnswerRaw, currentOptions) ?: 0
                questions.add(
                    QuizQuestion(
                        questionText = currentQ.trim(),
                        options = currentOptions.take(5),
                        correctIndex = resolvedIdx.coerceIn(0, (currentOptions.take(5).size - 1).coerceAtLeast(0)),
                        explanation = currentExplanation.trim()
                    )
                )
            }
            currentQ = ""
            currentOptions.clear()
            currentAnswerRaw = ""
            currentExplanation = ""
        }

        for (line in lines) {
            val cleanLine = line.replace("**", "").trim()
            if (cleanLine.isEmpty()) continue

            val ansMatch = answerRegex.matchEntire(cleanLine)
            if (ansMatch != null && currentQ.isNotBlank()) {
                currentAnswerRaw = ansMatch.groupValues[1].trim()
                continue
            }

            val expMatch = explanationRegex.matchEntire(cleanLine)
            if (expMatch != null && currentQ.isNotBlank()) {
                currentExplanation = expMatch.groupValues[1].trim()
                continue
            }

            val optMatch = optionRegex.matchEntire(cleanLine)
            if (optMatch != null && currentQ.isNotBlank()) {
                val optText = optMatch.groupValues[2].trim()
                if (optText.isNotEmpty()) currentOptions.add(optText)
                continue
            }

            val qMatch = questionHeaderRegex.matchEntire(cleanLine)
            if (qMatch != null) {
                flushCurrentQuestion()
                currentQ = qMatch.groupValues[1].trim()
                continue
            }

            if (currentQ.isBlank() && parsedTitle.isBlank()) {
                val tMatch = titleRegex.matchEntire(cleanLine)
                if (tMatch != null) {
                    parsedTitle = tMatch.groupValues[1].trim()
                    continue
                }
            }

            if (currentQ.isNotBlank() && currentOptions.isEmpty()) {
                currentQ = "$currentQ $cleanLine"
            } else if (currentExplanation.isNotBlank()) {
                currentExplanation = "$currentExplanation $cleanLine"
            }
        }
        flushCurrentQuestion()

        if (questions.isEmpty()) return null

        val safeTitle = when {
            parsedTitle.length >= 2 -> parsedTitle.take(MAX_TITLE_LEN)
            fallbackChapter.length >= 2 -> "QCM : $fallbackChapter".take(MAX_TITLE_LEN)
            else -> "QCM SVT"
        }

        return ExternalAiParsedQuiz(
            draft = GeneratedQuizDraft(
                title = safeTitle,
                description = "",
                questions = questions
            ),
            chapterSvt = fallbackChapter
        )
    }
}
