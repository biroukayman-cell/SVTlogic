package com.example.data.ai

import android.util.Log
import com.example.core.AppLanguage
import com.example.core.ClassAiAnalysisInput
import com.example.data.model.QuizQuestion
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
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
            return generateWithModel(primaryModel, prompt)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Primary AI model timed out", e)
            throw IllegalStateException("AI request timed out after 60s. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (primaryError: Exception) {
            Log.w(TAG, "Primary model ($PRIMARY_MODEL) failed, trying fallback ($FALLBACK_MODEL)", primaryError)
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
                    throw AiAppCheckException(fallbackError)
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
            return lookupDefinitionWithModel(definitionPrimaryModel, prompt)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Primary definition model timed out", e)
            throw IllegalStateException("Search timed out after 60s. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (primaryError: Exception) {
            Log.w(TAG, "Primary definition model failed, trying fallback", primaryError)
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
                    throw AiAppCheckException(fallbackError)
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
            return analyzeClassWithModel(classAnalysisPrimaryModel, prompt)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Primary class analysis model timed out", e)
            throw IllegalStateException("AI request timed out after 60s. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (primaryError: Exception) {
            Log.w(TAG, "Primary class analysis model failed, trying fallback", primaryError)
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
                    throw AiAppCheckException(fallbackError)
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

    companion object {
        private const val TAG = "SvtAiRepository"
        private const val PRIMARY_MODEL = "gemini-3.8-flash"
        private const val FALLBACK_MODEL = "gemini-3.5-flash-lite"

        fun isAppCheckOrTokenError(error: Throwable): Boolean {
            return generateSequence(error) { it.cause }.any { cause ->
                val cls = cause::class.java.name
                val msg = cause.message.orEmpty()
                cls.contains("AppCheck", ignoreCase = true) ||
                    msg.contains("app check", ignoreCase = true) ||
                    msg.contains("appcheck", ignoreCase = true) ||
                    msg.contains("attestation", ignoreCase = true) ||
                    msg.contains("app token", ignoreCase = true) ||
                    msg.contains("firebase token", ignoreCase = true) ||
                    msg.contains("Too many attempts", ignoreCase = true) ||
                    msg.contains("403", ignoreCase = true)
            }
        }
    }
}

