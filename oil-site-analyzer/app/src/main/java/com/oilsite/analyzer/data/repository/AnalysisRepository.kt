package com.oilsite.analyzer.data.repository

import android.content.Context
import android.net.Uri
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.oilsite.analyzer.data.model.AnalysisReport
import com.oilsite.analyzer.domain.usecase.AnalyzeImageUseCase

class AnalysisRepository(private val context: Context) {

    private val useCase = AnalyzeImageUseCase(context)

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "oil_site_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveApiKey(apiKey: String) {
        encryptedPrefs.edit().putString(KEY_API, apiKey).apply()
    }

    fun getApiKey(): String? = encryptedPrefs.getString(KEY_API, null)

    fun hasApiKey(): Boolean = !getApiKey().isNullOrBlank()

    suspend fun analyzeImage(imageUri: Uri): Result<AnalysisReport> {
        val apiKey = getApiKey()
            ?: return Result.failure(Exception("No API key configured. Please set your Claude API key in Settings."))
        return useCase.execute(imageUri, apiKey)
    }

    companion object {
        private const val KEY_API = "claude_api_key"
    }
}
