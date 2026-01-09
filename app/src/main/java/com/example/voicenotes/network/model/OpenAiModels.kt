package com.example.voicenotes.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OpenAI API Request/Response DTOs.
 */

@Serializable
data class OpenAiRequest(
    val model: String = "gpt-4o-mini",
    val messages: List<OpenAiMessage>,
    @SerialName("max_tokens")
    val maxTokens: Int = 4096
)

@Serializable
data class OpenAiMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenAiResponse(
    val id: String? = null,
    val choices: List<OpenAiChoice>? = null,
    val error: OpenAiError? = null
)

@Serializable
data class OpenAiChoice(
    val index: Int = 0,
    val message: OpenAiMessage? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class OpenAiError(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null
)
