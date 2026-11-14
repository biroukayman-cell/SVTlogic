package com.example.data.ai

import android.util.Log
import com.example.core.AppLanguage
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

