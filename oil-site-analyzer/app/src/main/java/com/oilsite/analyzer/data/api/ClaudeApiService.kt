package com.oilsite.analyzer.data.api

import com.oilsite.analyzer.data.api.models.ClaudeRequest
import com.oilsite.analyzer.data.api.models.ClaudeResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ClaudeApiService {

    @POST("v1/messages")
    suspend fun analyzeImage(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: ClaudeRequest
    ): ClaudeResponse
}
