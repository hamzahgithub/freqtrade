package com.oilsite.analyzer.data.model

import com.google.gson.annotations.SerializedName

data class ClaudeAnalysisData(
    @SerializedName("readings") val readings: List<OilReading> = emptyList(),
    @SerializedName("equipment_type") val equipmentType: String = "general",
    @SerializedName("equipment_details") val equipmentDetails: String = "",
    @SerializedName("image_quality") val imageQuality: String = "good",
    @SerializedName("notes") val notes: String = ""
)

data class AnalysisReport(
    val rawReadings: List<OilReading>,
    val validationResults: List<ValidationResult>,
    val equipmentType: String,
    val equipmentDetails: String,
    val imageQuality: String,
    val notes: String,
    val imagePath: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val normalCount get() = validationResults.count { it.status == ReadingStatus.NORMAL }
    val warningCount get() = validationResults.count { it.status == ReadingStatus.WARNING }
    val criticalCount get() = validationResults.count { it.status == ReadingStatus.CRITICAL }
    val underHeatCount get() = validationResults.count { it.status == ReadingStatus.UNDERHEAT }

    val overallStatus: ReadingStatus
        get() = when {
            criticalCount > 0 -> ReadingStatus.CRITICAL
            warningCount > 0 || underHeatCount > 0 -> ReadingStatus.WARNING
            normalCount > 0 -> ReadingStatus.NORMAL
            else -> ReadingStatus.UNKNOWN
        }
}
