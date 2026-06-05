package com.oilsite.analyzer.data.model

import com.google.gson.annotations.SerializedName

data class OilReading(
    @SerializedName("type") val type: String,
    @SerializedName("value") val value: Double,
    @SerializedName("unit") val unit: String,
    @SerializedName("label") val label: String,
    @SerializedName("location") val location: String,
    @SerializedName("instrument_type") val instrumentType: String,
    @SerializedName("confidence") val confidence: Float
) {
    fun readingType(): ReadingType = when (type.lowercase()) {
        "temperature" -> ReadingType.TEMPERATURE
        "pressure" -> ReadingType.PRESSURE
        "flow" -> ReadingType.FLOW
        "level" -> ReadingType.LEVEL
        else -> ReadingType.OTHER
    }

    fun locationType(): LocationType = when (location.lowercase()) {
        "inlet", "in", "دخول", "داخل" -> LocationType.INLET
        "outlet", "out", "خروج", "خارج" -> LocationType.OUTLET
        "bath", "باث", "حوض" -> LocationType.BATH
        "suction" -> LocationType.SUCTION
        "discharge" -> LocationType.DISCHARGE
        else -> LocationType.GENERAL
    }
}

enum class ReadingType {
    TEMPERATURE, PRESSURE, FLOW, LEVEL, OTHER
}

enum class LocationType {
    INLET, OUTLET, BATH, SUCTION, DISCHARGE, GENERAL
}
