package com.example.data.ai

import android.util.Base64
import android.util.Log
import com.example.core.AppLanguage
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

