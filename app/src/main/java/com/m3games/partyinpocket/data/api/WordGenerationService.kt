package com.m3games.partyinpocket.data.api

import android.util.Log
import com.m3games.partyinpocket.data.api.models.ErrorDetails
import com.m3games.partyinpocket.data.api.models.JsonSchema
import com.m3games.partyinpocket.data.api.models.Message
import com.m3games.partyinpocket.data.api.models.OpenRouterError
import com.m3games.partyinpocket.data.api.models.OpenRouterRequest
import com.m3games.partyinpocket.data.api.models.OpenRouterResponse
import com.m3games.partyinpocket.data.api.models.ResponseFormat
import com.m3games.partyinpocket.data.api.models.WordListResponse
import com.m3games.partyinpocket.domain.model.AiSettings
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class WordGenerationService {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true
            })
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    Log.d("WordGeneration", message)
                }
            }
            level = LogLevel.BODY
        }
    }

    private val systemPrompt = """
        Ты помощник для генерации слов для игры "Шляпа" (Hat Game).

        Правила игры:
        - Игроки объясняют слова тремя способами: словами (1 раунд), жестами (2 раунд), одним словом-ассоциацией (3 раунд)
        - Слова должны быть существительными в именительном падеже единственного числа
        - Слова должны быть понятными для объяснения всеми тремя способами
        - Избегай абстрактных понятий, которые сложно показать жестами
        - Избегай слов, для которых сложно придумать ассоциацию
        - Слова должны быть разнообразными и интересными

        Требования к словам:
        - Только существительные в именительном падеже единственного числа
        - Русский язык
        - Нарицательные существительные (кроме случаев, когда тема явно про имена собственные)
        - Слова должны быть уникальными (без повторений)
        - Длина слова: от 3 до 25 символов
        - Слова должны соответствовать заданной теме

        Верни JSON объект со списком слов в формате: {"words": ["слово1", "слово2", ...]}
    """.trimIndent()

    suspend fun generateWords(
        theme: String,
        targetCount: Int,
        settings: AiSettings,
        existingWords: Set<String> = emptySet()
    ): Result<List<String>> {
        return try {
            val userPrompt = if (existingWords.isEmpty()) {
                "Сгенерируй $targetCount уникальных слов на тему: $theme"
            } else {
                val remaining = targetCount - existingWords.size
                "Сгенерируй еще $remaining уникальных слов на тему: $theme. Уже сгенерированные слова (не используй их повторно): ${existingWords.joinToString(", ")}"
            }

            val jsonSchema = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("words") {
                        put("type", "array")
                        putJsonObject("items") {
                            put("type", "string")
                        }
                    }
                }
                putJsonArray("required") {
                    add(JsonPrimitive("words"))
                }
            }

            val request = OpenRouterRequest(
                model = settings.model,
                messages = listOf(
                    Message(role = "system", content = systemPrompt),
                    Message(role = "user", content = userPrompt)
                ),
                response_format = ResponseFormat(
                    type = "json_schema",
                    json_schema = JsonSchema(
                        name = "word_list",
                        schema = jsonSchema
                    )
                )
            )

            Log.d("WordGeneration", "Sending request to ${settings.baseUrl}")
            Log.d("WordGeneration", "Request: $request")

            val response = client.post(settings.baseUrl) {
                contentType(ContentType.Application.Json)
                headers {
                    append("Authorization", "Bearer ${settings.token}")
                }
                setBody(request)
            }

            Log.d("WordGeneration", "Response status: ${response.status}")

            // Check if response is an error
            if (!response.status.value.toString().startsWith("2")) {
                val errorResponse: OpenRouterError = response.body()
                Log.e("WordGeneration", "API Error: ${errorResponse.error.message}")

                val errorMessage = when (errorResponse.error.code) {
                    401 -> "Неверный API токен. Проверьте настройки."
                    403 -> "Превышен лимит API ключа. Проверьте лимиты на https://openrouter.ai/settings/keys"
                    429 -> "Слишком много запросов. Попробуйте позже."
                    else -> "Ошибка API: ${errorResponse.error.message}"
                }

                return Result.failure(Exception(errorMessage))
            }

            val openRouterResponse: OpenRouterResponse = response.body()
            Log.d("WordGeneration", "Response: $openRouterResponse")

            val content = openRouterResponse.choices.firstOrNull()?.message?.content
                ?: return Result.failure(Exception("Пустой ответ от API"))

            val wordList = Json.decodeFromString<WordListResponse>(content)
            val newWords = wordList.words
                .map { it.trim() }
                .filter { it.isNotEmpty() && it.length in 3..25 }
                .distinct()
                .filterNot { it in existingWords }

            Log.d("WordGeneration", "Generated ${newWords.size} new words")

            Result.success(newWords)
        } catch (e: Exception) {
            Log.e("WordGeneration", "Error generating words", e)
            Result.failure(e)
        }
    }

    suspend fun generateWordsWithRetry(
        theme: String,
        targetCount: Int,
        settings: AiSettings,
        maxAttempts: Int = 3,
        onProgress: (attempt: Int, currentCount: Int) -> Unit
    ): Result<Pair<List<String>, Boolean>> {
        val allWords = mutableSetOf<String>()
        var attempt = 0

        while (attempt < maxAttempts && allWords.size < targetCount) {
            attempt++
            onProgress(attempt, allWords.size)

            val result = generateWords(theme, targetCount, settings, allWords)

            if (result.isSuccess) {
                val newWords = result.getOrNull() ?: emptyList()
                allWords.addAll(newWords)
            } else {
                // On error, return what we have so far
                return if (allWords.isNotEmpty()) {
                    Result.success(allWords.toList() to false)
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Неизвестная ошибка"))
                }
            }
        }

        val isComplete = allWords.size >= targetCount
        return Result.success(allWords.toList() to isComplete)
    }

    fun close() {
        client.close()
    }
}
