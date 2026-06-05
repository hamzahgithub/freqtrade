package com.oilsite.analyzer.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import com.google.gson.Gson
import com.oilsite.analyzer.data.api.NetworkModule
import com.oilsite.analyzer.data.api.models.ClaudeMessage
import com.oilsite.analyzer.data.api.models.ClaudeRequest
import com.oilsite.analyzer.data.api.models.ContentBlock
import com.oilsite.analyzer.data.api.models.ImageSource
import com.oilsite.analyzer.data.model.AnalysisReport
import com.oilsite.analyzer.data.model.ClaudeAnalysisData
import com.oilsite.analyzer.data.model.LocationType
import com.oilsite.analyzer.data.model.OilReading
import com.oilsite.analyzer.data.model.ReadingType
import com.oilsite.analyzer.data.model.ValidationResult
import com.oilsite.analyzer.domain.engineering.FlowValidator
import com.oilsite.analyzer.domain.engineering.HeaterValidator
import com.oilsite.analyzer.domain.engineering.PressureValidator
import com.oilsite.analyzer.domain.engineering.TankLevelValidator
import java.io.ByteArrayOutputStream

class AnalyzeImageUseCase(private val context: Context) {

    private val gson = Gson()

    suspend fun execute(imageUri: Uri, apiKey: String): Result<AnalysisReport> {
        return try {
            val base64Image = encodeImageToBase64(imageUri)
                ?: return Result.failure(Exception("Failed to encode image"))

            val prompt = buildEngineeringPrompt()

            val request = ClaudeRequest(
                messages = listOf(
                    ClaudeMessage(
                        content = listOf(
                            ContentBlock(
                                type = "image",
                                source = ImageSource(
                                    mediaType = "image/jpeg",
                                    data = base64Image
                                )
                            ),
                            ContentBlock(
                                type = "text",
                                text = prompt
                            )
                        )
                    )
                )
            )

            val response = NetworkModule.claudeApiService.analyzeImage(
                apiKey = apiKey,
                request = request
            )

            val rawText = response.textContent()
            val analysisData = parseClaudeResponse(rawText)
            val validationResults = applyEngineeringValidation(analysisData.readings, analysisData.equipmentType)

            Result.success(
                AnalysisReport(
                    rawReadings = analysisData.readings,
                    validationResults = validationResults,
                    equipmentType = analysisData.equipmentType,
                    equipmentDetails = analysisData.equipmentDetails,
                    imageQuality = analysisData.imageQuality,
                    notes = analysisData.notes,
                    imagePath = imageUri.toString()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun encodeImageToBase64(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bytes = inputStream.readBytes()
            inputStream.close()

            // Compress if too large (max ~4MB for API)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            val outputStream = ByteArrayOutputStream()

            if (bytes.size > 3_000_000) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
            } else {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            }

            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    private fun buildEngineeringPrompt(): String = """
You are a certified petroleum/oil & gas field engineer with expertise in surface production facilities.

Analyze this image and extract ALL visible instrument readings with maximum engineering precision.

EXTRACTION RULES:
1. Read every gauge, display, meter, indicator, sight glass, or level gauge visible
2. Extract the EXACT numerical value — do not estimate, round only if necessary
3. Identify the unit precisely as shown on the instrument
4. If a gauge has multiple scales, use the one that matches the pointer/reading
5. For analog gauges: read the needle position relative to the scale arc carefully
6. For digital displays: read ALL digits shown

FOR HEATER EQUIPMENT (indirect fired / line heaters / bath heaters):
- "bath" = water/glycol bath temperature (حرارة الباث) - the outer vessel temperature
- "inlet" = process fluid entering the heater coil (حرارة الدخول)
- "outlet" = process fluid exiting the heater coil (حرارة الخروج)

FOR SEPARATORS: look for operating pressure, liquid levels, temperatures
FOR TANKS: look for level gauges (%, meters, feet), temperature probes
FOR COMPRESSORS: look for suction/discharge pressure and temperature

Respond with ONLY valid JSON — no markdown, no explanation, no text outside the JSON:
{
  "readings": [
    {
      "type": "temperature|pressure|flow|level|other",
      "value": 0.0,
      "unit": "string (e.g. °C, °F, barg, psi, bar, m³/h, MMSCFD, bbl/d, %, m)",
      "label": "text label on instrument or nearest tag (empty string if none)",
      "location": "inlet|outlet|bath|suction|discharge|general",
      "instrument_type": "dial_gauge|digital_display|sight_glass|level_indicator|chart_recorder|other",
      "confidence": 0.0
    }
  ],
  "equipment_type": "heater|separator|tank|compressor|pipeline|control_panel|general",
  "equipment_details": "brief engineering description of what is visible",
  "image_quality": "good|fair|poor",
  "notes": "any important engineering observations (hydrate risk, abnormal readings, equipment condition)"
}
    """.trimIndent()

    private fun parseClaudeResponse(rawText: String): ClaudeAnalysisData {
        // Extract JSON from response (may have surrounding text)
        val jsonText = extractJson(rawText) ?: return ClaudeAnalysisData(
            notes = "Failed to parse response: $rawText"
        )

        return try {
            gson.fromJson(jsonText, ClaudeAnalysisData::class.java)
        } catch (e: Exception) {
            ClaudeAnalysisData(notes = "JSON parse error: ${e.message}")
        }
    }

    private fun extractJson(text: String): String? {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        return if (start >= 0 && end > start) text.substring(start, end + 1) else null
    }

    private fun applyEngineeringValidation(
        readings: List<OilReading>,
        equipmentType: String
    ): List<ValidationResult> {
        val results = mutableListOf<ValidationResult>()

        when (equipmentType.lowercase()) {
            "heater" -> results.addAll(validateHeaterReadings(readings))
            "separator" -> results.addAll(validateSeparatorReadings(readings))
            "tank" -> results.addAll(validateTankReadings(readings))
            else -> results.addAll(validateGenericReadings(readings))
        }

        // Always run generic validation for any missed readings
        val processedLocations = results.map { it.parameter }.toSet()
        readings.forEach { reading ->
            val key = "${reading.type}_${reading.location}"
            if (key !in processedLocations) {
                results.addAll(validateGenericReading(reading))
            }
        }

        return results.distinctBy { it.parameter + it.value }
    }

    private fun validateHeaterReadings(readings: List<OilReading>): List<ValidationResult> {
        val results = mutableListOf<ValidationResult>()

        val bathReading = readings.firstOrNull {
            it.locationType() == LocationType.BATH && it.readingType() == ReadingType.TEMPERATURE
        }
        val inletReading = readings.firstOrNull {
            it.locationType() == LocationType.INLET && it.readingType() == ReadingType.TEMPERATURE
        }
        val outletReading = readings.firstOrNull {
            it.locationType() == LocationType.OUTLET && it.readingType() == ReadingType.TEMPERATURE
        }

        bathReading?.let { r ->
            val tempC = tocelsius(r.value, r.unit)
            results.add(HeaterValidator.validateBathTemperature(tempC))
        }

        inletReading?.let { r ->
            val tempC = tocelsius(r.value, r.unit)
            results.add(HeaterValidator.validateInletTemperature(tempC))
        }

        outletReading?.let { r ->
            val tempC = tocelsius(r.value, r.unit)
            results.add(HeaterValidator.validateOutletTemperature(tempC))
        }

        // ΔT analysis when both inlet and outlet are available
        if (inletReading != null && outletReading != null) {
            val inletC = tocelsius(inletReading.value, inletReading.unit)
            val outletC = tocelsius(outletReading.value, outletReading.unit)
            results.add(HeaterValidator.validateHeatTransferEfficiency(inletC, outletC))
        }

        // Validate any pressure readings found in the heater image
        readings.filter { it.readingType() == ReadingType.PRESSURE }
            .forEach { r -> results.add(validatePressureReading(r)) }

        return results
    }

    private fun validateSeparatorReadings(readings: List<OilReading>): List<ValidationResult> {
        val results = mutableListOf<ValidationResult>()
        readings.forEach { reading ->
            when (reading.readingType()) {
                ReadingType.PRESSURE -> results.add(validatePressureReading(reading))
                ReadingType.TEMPERATURE -> {
                    val tempC = tocelsius(reading.value, reading.unit)
                    results.add(HeaterValidator.validateInletTemperature(tempC))
                }
                ReadingType.LEVEL -> results.add(TankLevelValidator.validate(reading.value))
                else -> results.addAll(validateGenericReading(reading))
            }
        }
        return results
    }

    private fun validateTankReadings(readings: List<OilReading>): List<ValidationResult> {
        val results = mutableListOf<ValidationResult>()
        readings.forEach { reading ->
            when (reading.readingType()) {
                ReadingType.LEVEL -> results.add(TankLevelValidator.validate(reading.value))
                ReadingType.TEMPERATURE -> {
                    val tempC = tocelsius(reading.value, reading.unit)
                    results.add(HeaterValidator.validateInletTemperature(tempC))
                }
                ReadingType.PRESSURE -> results.add(validatePressureReading(reading))
                else -> results.addAll(validateGenericReading(reading))
            }
        }
        return results
    }

    private fun validateGenericReadings(readings: List<OilReading>): List<ValidationResult> =
        readings.flatMap { validateGenericReading(it) }

    private fun validateGenericReading(reading: OilReading): List<ValidationResult> {
        return when (reading.readingType()) {
            ReadingType.PRESSURE -> listOf(validatePressureReading(reading))
            ReadingType.LEVEL -> listOf(TankLevelValidator.validate(reading.value))
            ReadingType.FLOW -> listOf(FlowValidator.validate(reading.value, reading.unit))
            ReadingType.TEMPERATURE -> {
                val tempC = tocelsius(reading.value, reading.unit)
                when (reading.locationType()) {
                    LocationType.BATH -> listOf(HeaterValidator.validateBathTemperature(tempC))
                    LocationType.INLET -> listOf(HeaterValidator.validateInletTemperature(tempC))
                    LocationType.OUTLET -> listOf(HeaterValidator.validateOutletTemperature(tempC))
                    else -> listOf(HeaterValidator.validateInletTemperature(tempC))
                }
            }
            else -> emptyList()
        }
    }

    private fun validatePressureReading(reading: OilReading): ValidationResult {
        val unit = reading.unit.lowercase()
        return when {
            "psi" in unit -> PressureValidator.validatePsi(reading.value)
            "bar" in unit -> PressureValidator.validate(reading.value)
            "kpa" in unit -> PressureValidator.validate(reading.value / 100.0)
            "mpa" in unit -> PressureValidator.validate(reading.value * 10.0)
            else -> PressureValidator.validate(reading.value)
        }
    }

    private fun tocelsius(value: Double, unit: String): Double {
        val u = unit.lowercase().replace("°", "").trim()
        return when (u) {
            "f", "°f" -> (value - 32) * 5.0 / 9.0
            "k" -> value - 273.15
            else -> value
        }
    }
}
