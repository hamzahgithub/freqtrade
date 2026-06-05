package com.oilsite.analyzer.data.model

data class ValidationResult(
    val parameter: String,
    val parameterAr: String,
    val value: Double,
    val unit: String,
    val status: ReadingStatus,
    val message: String,
    val messageAr: String,
    val recommendation: String? = null,
    val recommendationAr: String? = null
)

enum class ReadingStatus {
    NORMAL,
    WARNING,
    CRITICAL,
    UNDERHEAT,
    UNKNOWN;

    fun colorRes(): Int = when (this) {
        NORMAL -> android.R.color.holo_green_dark
        WARNING -> android.R.color.holo_orange_light
        CRITICAL -> android.R.color.holo_red_light
        UNDERHEAT -> android.R.color.holo_blue_light
        UNKNOWN -> android.R.color.darker_gray
    }

    fun labelEn(): String = when (this) {
        NORMAL -> "NORMAL"
        WARNING -> "WARNING"
        CRITICAL -> "CRITICAL"
        UNDERHEAT -> "UNDERHEAT"
        UNKNOWN -> "UNKNOWN"
    }

    fun labelAr(): String = when (this) {
        NORMAL -> "طبيعي"
        WARNING -> "تحذير"
        CRITICAL -> "حرج"
        UNDERHEAT -> "برودة زائدة"
        UNKNOWN -> "غير معروف"
    }
}
