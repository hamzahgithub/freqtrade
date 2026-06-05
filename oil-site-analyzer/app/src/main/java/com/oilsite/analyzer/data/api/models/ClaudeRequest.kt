package com.oilsite.analyzer.data.api.models

import com.google.gson.annotations.SerializedName

data class ClaudeRequest(
    val model: String = "claude-opus-4-8",
    @SerializedName("max_tokens") val maxTokens: Int = 2048,
    val messages: List<ClaudeMessage>
)

data class ClaudeMessage(
    val role: String = "user",
    val content: List<ContentBlock>
)

data class ContentBlock(
    val type: String,
    val text: String? = null,
    val source: ImageSource? = null
)

data class ImageSource(
    val type: String = "base64",
    @SerializedName("media_type") val mediaType: String,
    val data: String
)
