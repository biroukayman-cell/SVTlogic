package com.example.data.ai

import android.util.Base64
import android.util.Log
import com.example.core.AppLanguage
import com.example.core.ClassAiAnalysisInput
import com.example.core.QuizErrorSummaryInput
import com.example.core.clearAppCheckTokenStore
import com.example.data.model.QuizQuestion
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.appcheck.FirebaseAppCheck
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class GeneratedQuizDraft(
    val title: String,
    val description: String,
    val questions: List<QuizQuestion>
)

data class SvtTermLookupResult(
    val isSvtTerm: Boolean,
    val term: String,
    val definition: String
)

data class ClassAiAnalysisReport(
    val textualSummary: String,
    val strengths: List<String>,
    val gaps: List<String>,
    val recommendations: List<String>
)

data class QuizErrorsAiReport(
    val overallSynthesis: String,
    val frequentErrors: List<String>,
    val misconceptionCauses: List<String>,
    val remediationActions: List<String>
)

class AiAppCheckException(cause: Throwable) : IllegalStateException(
    cause.message ?: "Firebase App Check token missing",
    cause
)

class SvtAiRepository {

    private val quizSchema = Schema.obj(
        properties = mapOf(
            "title" to Schema.string(description = "Concise title of the SVT quiz"),
            "description" to Schema.string(description = "Short instructions for the students"),
            "questions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "questionText" to Schema.string(description = "The SVT question text"),
                        "options" to Schema.array(
                            items = Schema.string(),
                            description = "Exactly 4 answer choices"
                        ),
                        "correctIndex" to Schema.integer(description = "0-based index (0..3) of the correct option"),
                        "explanation" to Schema.string(description = "Brief scientific SVT explanation of the correct answer")
                    )
                ),
                description = "List of generated multiple-choice questions"
            )
        )
    )

    private val definitionSchema = Schema.obj(
        properties = mapOf(
            "isSvtTerm" to Schema.boolean(description = "True ONLY if the query belongs strictly to Life and Earth Sciences (SVT: biology, genetics, immunity, reproduction, physiology, ecology, geology). False for any other topic."),
            "term" to Schema.string(description = "Canonical SVT scientific term name in the requested language, or empty string if isSvtTerm is false."),
            "definition" to Schema.string(description = "ONLY the concise scientific SVT definition (1 to 3 sentences, no tips, no extra commentary), or empty string if isSvtTerm is false.")
        )
    )

    private val classAnalysisSchema = Schema.obj(
        properties = mapOf(
            "textualSummary" to Schema.string(description = "A clear, well-written pedagogical paragraph (3 to 5 sentences) summarizing the overall performance, strengths, and gaps of the entire class."),
            "strengths" to Schema.array(
                items = Schema.string(),
                description = "2 to 5 concise bullet points describing the class's identified strengths (well-mastered chapters, concepts, or engagement)."
            ),
            "gaps" to Schema.array(
                items = Schema.string(),
                description = "2 to 5 concise bullet points describing the class's identified learning gaps, weaknesses, or difficult SVT concepts."
            ),
            "recommendations" to Schema.array(
                items = Schema.string(),
                description = "2 to 4 concrete pedagogical remediation actions the SVT teacher can apply with the class."
            )
        )
    )

    private val quizErrorsSchema = Schema.obj(
        properties = mapOf(
            "overallSynthesis" to Schema.string(description = "A concise 2 to 4 sentence synthesis of the quiz results and the main recurring errors observed across students."),
            "frequentErrors" to Schema.array(
                items = Schema.string(),
                description = "2 to 6 concrete bullet points citing the specific questions, wrong options frequently chosen, and SVT concepts where students failed most."
            ),
            "misconceptionCauses" to Schema.array(
                items = Schema.string(),
                description = "2 to 4 pedagogical diagnoses explaining the likely scientific misconceptions or confusions behind these frequent errors."
            ),
            "remediationActions" to Schema.array(
                items = Schema.string(),
                description = "2 to 4 targeted classroom remediation steps or follow-up explanations the SVT teacher can present."
            )
        )
    )

    private val primaryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = PRIMARY_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = quizSchema
            }
        )
    }

    private val fallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FALLBACK_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = quizSchema
            }
        )
    }

    private val definitionPrimaryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = PRIMARY_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = definitionSchema
            }
        )
    }

    private val definitionFallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FALLBACK_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = definitionSchema
            }
        )
    }

    private val classAnalysisPrimaryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = PRIMARY_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = classAnalysisSchema
            }
        )
    }

    private val classAnalysisFallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FALLBACK_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = classAnalysisSchema
            }
        )
    }

    private val quizErrorsPrimaryModel by lazy {
        Firebase.ai.generativeModel(
            modelName = PRIMARY_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = quizErrorsSchema
            }
        )
    }

    private val quizErrorsFallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FALLBACK_MODEL,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = quizErrorsSchema
            }
        )
    }

    suspend fun generateSvtQuiz(
        chapter: String,
        level: String,
        questionCount: Int,
        language: AppLanguage
    ): GeneratedQuizDraft {
        val clampedCount = questionCount.coerceIn(1, 10)
        val langInstruction = when (language) {
            AppLanguage.FR -> "Write all text strictly in French."
            AppLanguage.AR -> "Write all text strictly in Modern Standard Arabic (العربية الفصحى العلمية)."
            AppLanguage.EN -> "Write all text strictly in English."
        }
        val prompt = """
            You are an expert Life and Earth Sciences (SVT - Sciences de la Vie et de la Terre) teacher.
            Generate a pedagogically accurate multiple-choice quiz (QCM) with $clampedCount questions.
            Level: $level
            SVT Chapter/Topic: $chapter
            Requirements:
            - $langInstruction
            - Each question must have exactly 4 plausible options and 1 clearly correct answer (correctIndex between 0 and 3).
            - Include a concise, accurate scientific explanation for each question.
        """.trimIndent()

        try {
            ensureAppCheckTokenReady(forceRefresh = false)
            return generateWithModel(primaryModel, prompt)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Primary AI model timed out", e)
            throw IllegalStateException("AI request timed out after 60s. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (primaryError: Exception) {
            Log.w(TAG, "Primary model ($PRIMARY_MODEL) failed, checking App Check & fallback ($FALLBACK_MODEL)", primaryError)
            if (isAppCheckOrTokenError(primaryError)) {
                try {
                    ensureAppCheckTokenReady(forceRefresh = true)
                    return generateWithModel(primaryModel, prompt)
                } catch (e: TimeoutCancellationException) {
                    Log.e(TAG, "Refreshed primary AI model timed out", e)
                    throw IllegalStateException("AI request timed out after 60s. Please retry.", e)
                } catch (e: CancellationException) {
                    throw e
                } catch (refreshError: Exception) {
                    if (isAppCheckOrTokenError(refreshError)) {
                        throw (refreshError as? AiAppCheckException) ?: AiAppCheckException(refreshError)
                    }
                }
            }
            return try {
                generateWithModel(fallbackModel, prompt)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Fallback AI model timed out", e)
                throw IllegalStateException("AI request timed out after 60s. Please retry.", e)
            } catch (e: CancellationException) {
                throw e
            } catch (fallbackError: Exception) {
                Log.e(TAG, "Fallback AI model failed", fallbackError)
                if (isAppCheckOrTokenError(fallbackError) || isAppCheckOrTokenError(primaryError)) {
                    val cause = (fallbackError as? AiAppCheckException)
                        ?: (primaryError as? AiAppCheckException)
                        ?: AiAppCheckException(fallbackError)
                    throw cause
                }
                throw fallbackError
            }
        }
    }

    suspend fun lookupSvtTermDefinition(
        query: String,
        language: AppLanguage
    ): SvtTermLookupResult {
        val cleanQuery = query.trim()
        val langInstruction = when (language) {
            AppLanguage.FR -> "Respond in French."
            AppLanguage.AR -> "Respond in Modern Standard Arabic (العربية الفصحى العلمية)."
            AppLanguage.EN -> "Respond in English."
        }
        val prompt = """
            You are a strict dictionary exclusively for Life and Earth Sciences (SVT - Sciences de la Vie et de la Terre): biology, cell biology, genetics, immunology, human/plant reproduction, physiology, neurobiology, anatomy, microbiology, biochemistry, ecology, geology, plate tectonics, petrology, seismology, and paleontology.
            User query: "$cleanQuery"
            Rules:
            1. Decide whether the query is genuinely a term, concept, organ, organism, molecule, or phenomenon related to SVT.
            2. If the query is NOT related to SVT (e.g. sports, history, politics, cooking, software/coding, celebrities, random chat, non-SVT math/physics/general words, or nonsense), set isSvtTerm=false, term="", and definition="".
            3. If isSvtTerm=true:
               - $langInstruction
               - Set "term" to the clean canonical SVT term name.
               - Set "definition" to ONLY a clear, precise scientific definition (1 to 3 sentences). Do NOT add examples, tips, study advice, greetings, or anything other than the definition.
        """.trimIndent()

        try {
            ensureAppCheckTokenReady(forceRefresh = false)
            return lookupDefinitionWithModel(definitionPrimaryModel, prompt)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Primary definition model timed out", e)
            throw IllegalStateException("Search timed out after 60s. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (primaryError: Exception) {
            Log.w(TAG, "Primary definition model failed, checking App Check & fallback", primaryError)
            if (isAppCheckOrTokenError(primaryError)) {
                try {
                    ensureAppCheckTokenReady(forceRefresh = true)
                    return lookupDefinitionWithModel(definitionPrimaryModel, prompt)
                } catch (e: TimeoutCancellationException) {
                    Log.e(TAG, "Refreshed definition model timed out", e)
                    throw IllegalStateException("Search timed out after 60s. Please retry.", e)
                } catch (e: CancellationException) {
                    throw e
                } catch (refreshError: Exception) {
                    if (isAppCheckOrTokenError(refreshError)) {
                        throw (refreshError as? AiAppCheckException) ?: AiAppCheckException(refreshError)
                    }
                }
            }
            return try {
                lookupDefinitionWithModel(definitionFallbackModel, prompt)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Fallback definition model timed out", e)
                throw IllegalStateException("Search timed out after 60s. Please retry.", e)
            } catch (e: CancellationException) {
                throw e
            } catch (fallbackError: Exception) {
                Log.e(TAG, "Fallback definition model failed", fallbackError)
                if (isAppCheckOrTokenError(fallbackError) || isAppCheckOrTokenError(primaryError)) {
                    val cause = (fallbackError as? AiAppCheckException)
                        ?: (primaryError as? AiAppCheckException)
                        ?: AiAppCheckException(fallbackError)
                    throw cause
                }
                throw fallbackError
            }
        }
    }

    private suspend fun lookupDefinitionWithModel(
        model: com.google.firebase.ai.GenerativeModel,
        prompt: String
    ): SvtTermLookupResult {
        val response = withTimeout(60_000L) {
            model.generateContent(prompt)
        }
        val jsonText = response.text?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Empty response from Gemini.")

        val root = JSONObject(jsonText)
        val isSvt = root.optBoolean("isSvtTerm", false)
        if (!isSvt) {
            return SvtTermLookupResult(isSvtTerm = false, term = "", definition = "")
        }
        val term = root.optString("term", "").trim()
        val definition = root.optString("definition", "").trim()
        if (definition.isBlank()) {
            return SvtTermLookupResult(isSvtTerm = false, term = "", definition = "")
        }
        return SvtTermLookupResult(isSvtTerm = true, term = term, definition = definition)
    }

    private suspend fun generateWithModel(
        model: com.google.firebase.ai.GenerativeModel,
        prompt: String
    ): GeneratedQuizDraft {
        val response = withTimeout(60_000L) {
            model.generateContent(prompt)
        }
        val jsonText = response.text?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Empty response from Gemini.")

        val root = JSONObject(jsonText)
        val title = root.optString("title", "").ifBlank { "QCM SVT" }
        val description = root.optString("description", "")
        val questionsJson = root.getJSONArray("questions")

        val questions = (0 until questionsJson.length()).map { idx ->
            val qObj = questionsJson.getJSONObject(idx)
            val optsJson = qObj.getJSONArray("options")
            val options = (0 until optsJson.length()).map { oIdx -> optsJson.getString(oIdx) }
            val correctIdx = qObj.optInt("correctIndex", 0).coerceIn(0, (options.size - 1).coerceAtLeast(0))
            QuizQuestion(
                questionText = qObj.getString("questionText"),
                options = options,
                correctIndex = correctIdx,
                explanation = qObj.optString("explanation", "")
            )
        }
        return GeneratedQuizDraft(
            title = title,
            description = description,
            questions = questions
        )
    }

    suspend fun analyzeClassPerformance(
        input: ClassAiAnalysisInput,
        language: AppLanguage
    ): ClassAiAnalysisReport {
        val langInstruction = when (language) {
            AppLanguage.FR -> "Write the entire analysis strictly in clear, professional pedagogical French."
            AppLanguage.AR -> "Write the entire analysis strictly in clear, professional pedagogical Modern Standard Arabic (العربية الفصحى التربوية)."
            AppLanguage.EN -> "Write the entire analysis strictly in clear, professional pedagogical English."
        }

        val quizLines = if (input.quizMetrics.isEmpty()) {
            "- No quizzes published yet."
        } else {
            input.quizMetrics.joinToString("\n") { q ->
                val ch = q.chapterSvt.ifBlank { "General SVT" }
                "- Quiz '${q.quizTitle}' (Chapter: $ch): ${q.attemptsCount} copies, average ${q.averagePct}%, ${q.succeededQuestions} correct answers, ${q.failedQuestions} failed answers"
            }
        }

        val strongLines = if (input.strongestQuestions.isEmpty()) {
            "- No specific high-success question data recorded yet."
        } else {
            input.strongestQuestions.joinToString("\n") { q ->
                "- [${q.chapterSvt.ifBlank { q.quizTitle }}] \"${q.questionText.take(120)}\" -> ${q.successRatePct}% success (${q.succeededCount} ok / ${q.failedCount} failed)"
            }
        }

        val weakLines = if (input.weakestQuestions.isEmpty()) {
            "- No specific failed question data recorded yet."
        } else {
            input.weakestQuestions.joinToString("\n") { q ->
                "- [${q.chapterSvt.ifBlank { q.quizTitle }}] \"${q.questionText.take(120)}\" -> ${q.successRatePct}% success (${q.succeededCount} ok / ${q.failedCount} failed)"
            }
        }

        val prompt = """
            You are an expert Life and Earth Sciences (SVT) pedagogical advisor assisting a teacher.
            Analyze the overall performance of this entire class and produce a clear, actionable synthesis of its strengths and learning gaps.

            Class Profile:
            - Class: ${input.className} (Level: ${input.level}, Year: ${input.academicYear})
            - Total students tracked: ${input.totalStudentsCount} (Active in QCM: ${input.activeQcmStudentsCount}, Not yet active in QCM: ${input.inactiveStudentsCount})
            - Class overall QCM average: ${input.classAveragePct}%
            - Global question performance: ${input.totalSucceededQuestions} succeeded vs ${input.totalFailedQuestions} failed (${input.questionSuccessRatePct}% success rate)
            - Student score distribution: >=70%: ${input.strongStudentsCount}, 50-69%: ${input.averageStudentsCount}, <50%: ${input.strugglingStudentsCount}
            - Course resources: ${input.totalResourcesCount} resources, video completion rate: ${input.videoCompletionRatePct}%

            Per-Quiz & Chapter Results:
            $quizLines

            Best-Mastered Questions:
            $strongLines

            Most Missed Questions:
            $weakLines

            Instructions:
            - $langInstruction
            - Ground every point strictly in the provided class metrics and SVT chapters/questions (do not invent scores or student names).
            - Summarize the class as a whole in "textualSummary" (highlighting overall level, key strengths, main gaps, and participation).
            - Provide specific, concrete "strengths", "gaps", and "recommendations" lists tailored to the SVT chapters and metrics above.
        """.trimIndent()

        try {
            ensureAppCheckTokenReady(forceRefresh = false)
            return analyzeClassWithModel(classAnalysisPrimaryModel, prompt)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Primary class analysis model timed out", e)
            throw IllegalStateException("AI request timed out after 60s. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (primaryError: Exception) {
            Log.w(TAG, "Primary class analysis model failed, checking App Check & fallback", primaryError)
            if (isAppCheckOrTokenError(primaryError)) {
                try {
                    ensureAppCheckTokenReady(forceRefresh = true)
                    return analyzeClassWithModel(classAnalysisPrimaryModel, prompt)
                } catch (e: TimeoutCancellationException) {
                    Log.e(TAG, "Refreshed class analysis model timed out", e)
                    throw IllegalStateException("AI request timed out after 60s. Please retry.", e)
                } catch (e: CancellationException) {
                    throw e
                } catch (refreshError: Exception) {
                    if (isAppCheckOrTokenError(refreshError)) {
                        throw (refreshError as? AiAppCheckException) ?: AiAppCheckException(refreshError)
                    }
                }
            }
            return try {
                analyzeClassWithModel(classAnalysisFallbackModel, prompt)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Fallback class analysis model timed out", e)
                throw IllegalStateException("AI request timed out after 60s. Please retry.", e)
            } catch (e: CancellationException) {
                throw e
            } catch (fallbackError: Exception) {
                Log.e(TAG, "Fallback class analysis model failed", fallbackError)
                if (isAppCheckOrTokenError(fallbackError) || isAppCheckOrTokenError(primaryError)) {
                    val cause = (fallbackError as? AiAppCheckException)
                        ?: (primaryError as? AiAppCheckException)
                        ?: AiAppCheckException(fallbackError)
                    throw cause
                }
                throw fallbackError
            }
        }
    }

    private suspend fun analyzeClassWithModel(
        model: com.google.firebase.ai.GenerativeModel,
        prompt: String
    ): ClassAiAnalysisReport {
        val response = withTimeout(60_000L) {
            model.generateContent(prompt)
        }
        val jsonText = response.text?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Empty response from Gemini.")

        val root = JSONObject(jsonText)
        val textualSummary = root.optString("textualSummary", "").trim()
        val strengthsArr = root.optJSONArray("strengths")
        val gapsArr = root.optJSONArray("gaps")
        val recArr = root.optJSONArray("recommendations")

        val strengths = (0 until (strengthsArr?.length() ?: 0))
            .mapNotNull { strengthsArr?.optString(it)?.trim()?.takeIf { s -> s.isNotEmpty() } }
        val gaps = (0 until (gapsArr?.length() ?: 0))
            .mapNotNull { gapsArr?.optString(it)?.trim()?.takeIf { s -> s.isNotEmpty() } }
        val recommendations = (0 until (recArr?.length() ?: 0))
            .mapNotNull { recArr?.optString(it)?.trim()?.takeIf { s -> s.isNotEmpty() } }

        if (textualSummary.isBlank() && strengths.isEmpty() && gaps.isEmpty()) {
            throw IllegalStateException("Incomplete AI analysis response from Gemini.")
        }

        return ClassAiAnalysisReport(
            textualSummary = textualSummary,
            strengths = strengths,
            gaps = gaps,
            recommendations = recommendations
        )
    }

    suspend fun analyzeQuizFrequentErrors(
        input: QuizErrorSummaryInput,
        language: AppLanguage
    ): QuizErrorsAiReport {
        val langInstruction = when (language) {
            AppLanguage.FR -> "Write the entire synthesis strictly in clear, professional pedagogical French."
            AppLanguage.AR -> "Write the entire synthesis strictly in clear, professional pedagogical Modern Standard Arabic (العربية الفصحى التربوية)."
            AppLanguage.EN -> "Write the entire synthesis strictly in clear, professional pedagogical English."
        }

        val questionBreakdown = if (input.questionErrorMetrics.isEmpty()) {
            "- No question-level breakdown available."
        } else {
            input.questionErrorMetrics.joinToString("\n") { q ->
                val wrongSummary = if (q.commonWrongOptions.isEmpty()) {
                    "No incorrect selections"
                } else {
                    q.commonWrongOptions.take(3).joinToString("; ") { opt ->
                        "\"${opt.optionText.take(90)}\" (${opt.count}x)"
                    }
                }
                buildString {
                    append("- Q${q.questionNumber}: \"${q.questionText.take(140)}\" | ")
                    append("Error rate: ${q.errorRatePct}% (${q.incorrectCount} wrong / ${q.totalResponses} answers, avg ${q.avgTimeSpentSeconds}s) | ")
                    append("Correct answer: \"${q.correctOptionText.take(90)}\" | ")
                    append("Frequent wrong choices: $wrongSummary")
                    if (q.explanation.isNotBlank()) {
                        append(" | Expected concept: ${q.explanation.take(120)}")
                    }
                }
            }
        }

        val prompt = """
            You are an expert Life and Earth Sciences (SVT) pedagogical advisor helping a teacher diagnose student mistakes on a specific QCM quiz.
            Synthesize the most frequent errors made by students and explain the underlying scientific misconceptions and remediation steps.

            Quiz Context:
            - Title: ${input.quizTitle}
            - SVT Chapter: ${input.chapterSvt.ifBlank { "General SVT" }}
            - Class: ${input.className.ifBlank { "Class" }} (Level: ${input.level.ifBlank { "SVT" }})
            - Total questions: ${input.totalQuestions}
            - Student attempts: ${input.attemptsCount}
            - Quiz average score: ${input.averagePct}%
            - Total answers: ${input.totalSucceededAnswers} correct vs ${input.totalFailedAnswers} failed
            - Average completion duration: ${input.averageDurationSeconds}s

            Per-Question Error Breakdown (sorted by highest error rate):
            $questionBreakdown

            Instructions:
            - $langInstruction
            - Ground every point strictly in the provided quiz questions, error rates, and frequent wrong choices (do not invent student names or extra metrics).
            - In "overallSynthesis", provide a clear summary of how students performed on this QCM and where errors concentrate.
            - In "frequentErrors", list the specific questions/concepts and common wrong choices students picked most often.
            - In "misconceptionCauses", explain why students likely fell for those distractors or confused those SVT mechanisms.
            - In "remediationActions", suggest concrete classroom review exercises or explanations to fix those errors.
        """.trimIndent()

        try {
            ensureAppCheckTokenReady(forceRefresh = false)
            return analyzeQuizErrorsWithModel(quizErrorsPrimaryModel, prompt)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Primary quiz errors model timed out", e)
            throw IllegalStateException("AI request timed out after 60s. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (primaryError: Exception) {
            Log.w(TAG, "Primary quiz errors model failed, checking App Check & fallback", primaryError)
            if (isAppCheckOrTokenError(primaryError)) {
                try {
                    ensureAppCheckTokenReady(forceRefresh = true)
                    return analyzeQuizErrorsWithModel(quizErrorsPrimaryModel, prompt)
                } catch (e: TimeoutCancellationException) {
                    Log.e(TAG, "Refreshed quiz errors model timed out", e)
                    throw IllegalStateException("AI request timed out after 60s. Please retry.", e)
                } catch (e: CancellationException) {
                    throw e
                } catch (refreshError: Exception) {
                    if (isAppCheckOrTokenError(refreshError)) {
                        throw (refreshError as? AiAppCheckException) ?: AiAppCheckException(refreshError)
                    }
                }
            }
            return try {
                analyzeQuizErrorsWithModel(quizErrorsFallbackModel, prompt)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Fallback quiz errors model timed out", e)
                throw IllegalStateException("AI request timed out after 60s. Please retry.", e)
            } catch (e: CancellationException) {
                throw e
            } catch (fallbackError: Exception) {
                Log.e(TAG, "Fallback quiz errors model failed", fallbackError)
                if (isAppCheckOrTokenError(fallbackError) || isAppCheckOrTokenError(primaryError)) {
                    val cause = (fallbackError as? AiAppCheckException)
                        ?: (primaryError as? AiAppCheckException)
                        ?: AiAppCheckException(fallbackError)
                    throw cause
                }
                throw fallbackError
            }
        }
    }

    private suspend fun analyzeQuizErrorsWithModel(
        model: com.google.firebase.ai.GenerativeModel,
        prompt: String
    ): QuizErrorsAiReport {
        val response = withTimeout(60_000L) {
            model.generateContent(prompt)
        }
        val jsonText = response.text?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Empty response from Gemini.")

        val root = JSONObject(jsonText)
        val overallSynthesis = root.optString("overallSynthesis", "").trim()
        val freqArr = root.optJSONArray("frequentErrors")
        val causesArr = root.optJSONArray("misconceptionCauses")
        val remArr = root.optJSONArray("remediationActions")

        val frequentErrors = (0 until (freqArr?.length() ?: 0))
            .mapNotNull { freqArr?.optString(it)?.trim()?.takeIf { s -> s.isNotEmpty() } }
        val misconceptionCauses = (0 until (causesArr?.length() ?: 0))
            .mapNotNull { causesArr?.optString(it)?.trim()?.takeIf { s -> s.isNotEmpty() } }
        val remediationActions = (0 until (remArr?.length() ?: 0))
            .mapNotNull { remArr?.optString(it)?.trim()?.takeIf { s -> s.isNotEmpty() } }

        if (overallSynthesis.isBlank() && frequentErrors.isEmpty() && misconceptionCauses.isEmpty()) {
            throw IllegalStateException("Incomplete AI quiz errors response from Gemini.")
        }

        return QuizErrorsAiReport(
            overallSynthesis = overallSynthesis,
            frequentErrors = frequentErrors,
            misconceptionCauses = misconceptionCauses,
            remediationActions = remediationActions
        )
    }

    private suspend fun ensureAppCheckTokenReady(forceRefresh: Boolean) {
        val firebaseApp = try {
            FirebaseApp.getInstance()
        } catch (_: Exception) {
            null
        } ?: return

        if (forceRefresh) {
            clearAppCheckTokenStore(firebaseApp.applicationContext, firebaseApp.persistenceKey)
        }

        val appCheck = try {
            FirebaseAppCheck.getInstance()
        } catch (e: Exception) {
            throw AiAppCheckException(e)
        }

        val tokenResult = try {
            withTimeout(12_000L) {
                appCheck.getAppCheckToken(forceRefresh).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw AiAppCheckException(e)
        }

        val rawToken = tokenResult.token
        if (rawToken.isBlank()) {
            throw AiAppCheckException(IllegalStateException("App Check token is empty"))
        }
        extractDummyTokenError(rawToken)?.let { embeddedError ->
            throw AiAppCheckException(
                IllegalStateException("App Check / reCAPTCHA verification failed: $embeddedError")
            )
        }
    }

    companion object {
        private const val TAG = "SvtAiRepository"
        private const val PRIMARY_MODEL = "gemini-3.8-flash"
        private const val FALLBACK_MODEL = "gemini-3.5-flash-lite"

        internal fun extractDummyTokenError(token: String): String? {
            val trimmed = token.trim()
            if (trimmed.isEmpty() || trimmed.count { it == '.' } >= 2) return null
            return runCatching {
                val decoded = Base64.decode(trimmed, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
                    .toString(Charsets.UTF_8)
                val obj = JSONObject(decoded)
                obj.optString("error", "").trim().takeIf { it.isNotEmpty() }
            }.getOrNull()
        }

        fun isAppCheckOrTokenError(error: Throwable): Boolean {
            return generateSequence(error) { it.cause }.any { cause ->
                val cls = cause::class.java.name
                val msg = cause.message.orEmpty()
                cls.contains("AppCheck", ignoreCase = true) ||
                    cls.contains("Recaptcha", ignoreCase = true) ||
                    msg.contains("app check", ignoreCase = true) ||
                    msg.contains("appcheck", ignoreCase = true) ||
                    msg.contains("attestation", ignoreCase = true) ||
                    msg.contains("app token", ignoreCase = true) ||
                    msg.contains("firebase token", ignoreCase = true) ||
                    msg.contains("recaptcha", ignoreCase = true) ||
                    msg.contains("Too many attempts", ignoreCase = true)
            }
        }
    }
}

