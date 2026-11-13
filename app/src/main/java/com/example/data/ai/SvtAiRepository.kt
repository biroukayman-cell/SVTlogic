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

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
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
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            val jsonText = response.text?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Empty response from Gemini.")

            val root = JSONObject(jsonText)
            val title = root.optString("title", chapter).ifBlank { chapter }
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
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI quiz generation timed out", e)
            throw IllegalStateException("AI request timed out after 60s. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI quiz generation failed", e)
            throw e
        }
    }

    private companion object {
        const val TAG = "SvtAiRepository"
    }
}
