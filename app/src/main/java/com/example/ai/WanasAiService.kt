package com.example.ai

/**
 * Safe AI facade.
 *
 * The core application must never depend on an optional AI provider during startup.
 * AI features can be wired to a backend later without making app initialization
 * depend on a provider SDK.
 */
object WanasAiService {
    private const val UNAVAILABLE = "ميزة الذكاء الاصطناعي غير متاحة حاليًا."

    suspend fun generate(prompt: String): Result<String> =
        Result.success(UNAVAILABLE)

    suspend fun assistant(userName: String, message: String, history: String) =
        generate("assistant")

    suspend fun roomAssistant(roomTitle: String, transcript: String) =
        generate("room")

    suspend fun moderate(text: String) =
        generate("moderate")

    suspend fun makeGame(topic: String) =
        generate("game")

    suspend fun buildProfile(interests: String) =
        generate("profile")
}
