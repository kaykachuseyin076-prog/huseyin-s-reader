package com.example.data.gemini

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class GeminiRepository(private val context: Context) {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    private fun getApiKey(): String {
        return GeminiSettingsManager.getEffectiveApiKey(context)
            ?: throw IllegalStateException("Gemini API anahtarınızı Ayarlar bölümünden ekleyin.")
    }

    /**
     * Sends a minimal test request to verify the Gemini API key and network connection.
     */
    suspend fun testConnection(overrideApiKey: String? = null): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = overrideApiKey?.trim()?.ifBlank { null } ?: GeminiSettingsManager.getEffectiveApiKey(context)
        if (apiKey.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("API key boşsa API isteği gönderilemez."))
        }

        try {
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = "Merhaba"))
                    )
                ),
                generationConfig = GeminiGenerationConfig(maxOutputTokens = 10)
            )

            val response = apiService.generateContent(apiKey, request)
            if (response.error != null) {
                Result.failure(Exception("Gemini bağlantısı kurulamadı. API anahtarınızı ve internet bağlantınızı kontrol edin."))
            } else {
                Result.success("Gemini bağlantısı başarılı.")
            }
        } catch (e: Throwable) {
            Result.failure(Exception("Gemini bağlantısı kurulamadı. API anahtarınızı ve internet bağlantınızı kontrol edin."))
        }
    }

    /**
     * Translates the selected text into the target language.
     */
    suspend fun translate(text: String, targetLanguage: String = "Türkçe"): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            val prompt = if (targetLanguage.equals("Türkçe", ignoreCase = true)) {
                """
                Translate the following text into natural Turkish. Preserve the original meaning and terminology. Do not add explanations unless necessary.

                TEXT:
                $text
                """.trimIndent()
            } else {
                """
                Translate the following text into natural $targetLanguage. Preserve the original meaning and terminology. Do not add explanations unless necessary.

                TEXT:
                $text
                """.trimIndent()
            }

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = prompt))
                    )
                ),
                systemInstruction = GeminiContent(
                    parts = listOf(
                        GeminiPart(
                            text = "Sen uzman ve edebî yetkinliği yüksek bir çevirmensin. Verilen metnin tonuna ve üslubuna sadık kalarak en doğal ve doğru çeviriyi üretirsin."
                        )
                    )
                ),
                generationConfig = GeminiGenerationConfig(temperature = 0.3f, maxOutputTokens = 2048)
            )

            val response = apiService.generateContent(apiKey, request)
            parseResponse(response)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    /**
     * Explains the selected text in clear and accurate Turkish.
     */
    suspend fun explain(text: String, contextSnippet: String? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            val prompt = """
                Explain the following passage clearly and accurately. Assume the reader wants to understand the meaning and argument of the passage. Provide the explanation in clear, well-structured Turkish with Markdown headings, bold text, and bullet points where helpful.

                PASSAGE:
                $text
            """.trimIndent()

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(role = "user", parts = listOf(GeminiPart(text = prompt)))
                ),
                systemInstruction = GeminiContent(
                    parts = listOf(
                        GeminiPart(
                            text = "Sen bilgili, sabırlı ve entelektüel bir okuma rehberisin. Okuyucunun metni derinlemesine anlamasına yardımcı olur, gereksiz laf kalabalığı yapmazsın."
                        )
                    )
                ),
                generationConfig = GeminiGenerationConfig(temperature = 0.4f, maxOutputTokens = 2048)
            )

            val response = apiService.generateContent(apiKey, request)
            parseResponse(response)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    /**
     * Summarizes the selected text concisely while preserving its key ideas.
     */
    suspend fun summarize(text: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            val prompt = """
                Summarize the following passage concisely while preserving its key ideas. Provide the summary in clear Turkish using clean Markdown bullet points.

                PASSAGE:
                $text
            """.trimIndent()

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(role = "user", parts = listOf(GeminiPart(text = prompt)))
                ),
                generationConfig = GeminiGenerationConfig(temperature = 0.3f, maxOutputTokens = 2048)
            )

            val response = apiService.generateContent(apiKey, request)
            parseResponse(response)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    /**
     * Simplifies difficult or archaic text into plain language.
     */
    suspend fun simplify(text: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            val prompt = """
                Rewrite the following passage in simpler language while preserving its meaning. Do not add unnecessary explanations; provide the simplified version in natural Turkish.

                PASSAGE:
                $text
            """.trimIndent()

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(role = "user", parts = listOf(GeminiPart(text = prompt)))
                ),
                generationConfig = GeminiGenerationConfig(temperature = 0.3f, maxOutputTokens = 2048)
            )

            val response = apiService.generateContent(apiKey, request)
            parseResponse(response)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    /**
     * Extracts key points and arguments from the selected text.
     */
    suspend fun extractKeyPoints(text: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            val prompt = """
                Extract and list the key points, arguments, and takeaways from the following passage. Present them as numbered or bulleted items in clear Turkish using Markdown.

                PASSAGE:
                $text
            """.trimIndent()

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(role = "user", parts = listOf(GeminiPart(text = prompt)))
                ),
                generationConfig = GeminiGenerationConfig(temperature = 0.3f, maxOutputTokens = 2048)
            )

            val response = apiService.generateContent(apiKey, request)
            parseResponse(response)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    /**
     * Explains the historical, philosophical, or literary context of the passage.
     */
    suspend fun getContextAnalysis(text: String, bookTitle: String? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            val bookInfo = if (!bookTitle.isNullOrBlank()) " ($bookTitle adlı eser)" else ""
            val prompt = """
                Explain the historical, philosophical, or literary context of the following passage$bookInfo. Provide a structured, insightful analysis in Turkish using Markdown formatting with headings and bullet points.

                PASSAGE:
                $text
            """.trimIndent()

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(role = "user", parts = listOf(GeminiPart(text = prompt)))
                ),
                generationConfig = GeminiGenerationConfig(temperature = 0.4f, maxOutputTokens = 2048)
            )

            val response = apiService.generateContent(apiKey, request)
            parseResponse(response)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    /**
     * Answers a custom user question specifically grounded on the selected text.
     */
    suspend fun askCustomQuestion(text: String, question: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            val prompt = """
                Below is a passage selected by the reader:
                PASSAGE:
                $text

                Reader's Question:
                $question

                Please answer the reader's question accurately, thoughtfully, and clearly in Turkish based on the passage, using clean Markdown formatting.
            """.trimIndent()

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(role = "user", parts = listOf(GeminiPart(text = prompt)))
                ),
                generationConfig = GeminiGenerationConfig(temperature = 0.4f, maxOutputTokens = 2048)
            )

            val response = apiService.generateContent(apiKey, request)
            parseResponse(response)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    /**
     * Generates an opening insight and discussion question for the selected passage.
     */
    suspend fun startDiscussion(selectedText: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            val prompt = """
                Aşağıda okuyucunun kitaptan seçtiği bir pasaj yer alıyor:

                PASAJ:
                "$selectedText"

                Okuyucu ile bu pasaj üzerine edebî ve fikrî bir tartışma başlatmak için pasajı kısaca değerlendir ve okuyucuyu düşünmeye sevk edecek ufuk açıcı bir başlangıç tespiti ve sorusu ilet. Yanıtını temiz bir Markdown formatında ver.
            """.trimIndent()

            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(role = "user", parts = listOf(GeminiPart(text = prompt)))
                ),
                systemInstruction = GeminiContent(
                    parts = listOf(
                        GeminiPart(
                            text = "Sen bir okuma asistanısın. Okuyucu ile seçilen metin üzerinde karşılıklı bir edebî/fikrî tartışma yapıyorsun. Okuyucuyu düşünmeye sevk eden, ufuk açıcı, nazik ve özlü cevaplar verirsin."
                        )
                    )
                ),
                generationConfig = GeminiGenerationConfig(temperature = 0.7f, maxOutputTokens = 2048)
            )

            val response = apiService.generateContent(apiKey, request)
            parseResponse(response)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    /**
     * Multi-turn chat / discussion over the selected text.
     * Only passes the conversation history and selected passage, never whole book.
     */
    suspend fun continueDiscussion(
        selectedText: String,
        history: List<Pair<String, String>>, // List of Pair(role: "user"|"assistant", content: String)
        newMessage: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = getApiKey()
            val contents = mutableListOf<GeminiContent>()

            // System instruction sets the context
            val systemText = """
                Sen bir okuma asistanısın. Okuyucu ile şu metin üzerinde karşılıklı bir edebî/fikrî tartışma yapıyorsun:
                "$selectedText"
                
                Okuyucunun sorularını bu metin merkezinde düşünerek yanıtla, ufuk açıcı yorumlar yap, nazik ol ve cevaplarını düzenli Markdown olarak biçimlendir.
            """.trimIndent()

            // If history starts with model/assistant, Gemini requires user first.
            // Add initial user context turn if needed.
            var hasUserFirst = false
            for ((role, _) in history) {
                if (role == "user") {
                    hasUserFirst = true
                    break
                }
            }

            if (!hasUserFirst && history.isNotEmpty()) {
                contents.add(
                    GeminiContent(
                        role = "user",
                        parts = listOf(GeminiPart(text = "Bu pasajı birlikte inceleyip tartışalım:\n\"$selectedText\""))
                    )
                )
            }

            // Build history turns
            for ((role, msg) in history) {
                val apiRole = if (role == "user") "user" else "model"
                contents.add(
                    GeminiContent(
                        role = apiRole,
                        parts = listOf(GeminiPart(text = msg))
                    )
                )
            }

            // Append new user message
            contents.add(
                GeminiContent(
                    role = "user",
                    parts = listOf(GeminiPart(text = newMessage))
                )
            )

            val request = GeminiRequest(
                contents = contents,
                systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemText))),
                generationConfig = GeminiGenerationConfig(temperature = 0.7f, maxOutputTokens = 2048)
            )

            val response = apiService.generateContent(apiKey, request)
            parseResponse(response)
        } catch (e: Exception) {
            Result.failure(mapError(e))
        }
    }

    private fun parseResponse(response: GeminiResponse): Result<String> {
        if (response.error != null) {
            val msg = response.error.message ?: "Hata kodu: ${response.error.code}"
            return Result.failure(Exception("Gemini Hatası: $msg"))
        }

        val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        return if (!text.isNullOrBlank()) {
            Result.success(text.trim())
        } else {
            Result.failure(Exception("Gemini yanıt veremedi. Lütfen tekrar deneyin."))
        }
    }

    private fun mapError(e: Throwable): Throwable {
        val message = e.message ?: ""
        return when {
            e is IllegalStateException && message.contains("Ayarlar") -> e
            e is IOException -> Exception("Gemini ile bağlantı kurulamadı. İnternet bağlantınızı veya API anahtarınızı kontrol edin.")
            message.contains("API_KEY_INVALID", ignoreCase = true) || message.contains("401") || message.contains("403") ->
                Exception("Geçersiz Gemini API anahtarı. Lütfen Ayarlar bölümünden kontrol edin.")
            message.contains("RESOURCE_EXHAUSTED", ignoreCase = true) || message.contains("429") ->
                Exception("Gemini kullanım kotası aşıldı. Lütfen bir süre sonra tekrar deneyin.")
            else -> Exception("Gemini ile bağlantı kurulamadı. İnternet bağlantınızı veya API anahtarınızı kontrol edin.")
        }
    }
}
