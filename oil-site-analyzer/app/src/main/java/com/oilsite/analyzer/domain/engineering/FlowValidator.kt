package com.oilsite.analyzer.domain.engineering

import com.oilsite.analyzer.data.model.ReadingStatus
import com.oilsite.analyzer.data.model.ValidationResult

object FlowValidator {

    fun validate(flowValue: Double, unit: String, designFlow: Double? = null): ValidationResult {
        if (designFlow == null) {
            return ValidationResult(
                parameter = "Flow Rate",
                parameterAr = "معدل التدفق",
                value = flowValue,
                unit = unit,
                status = ReadingStatus.NORMAL,
                message = "Flow rate: ${fmt(flowValue)} $unit (no design basis for comparison)",
                messageAr = "معدل التدفق: ${fmt(flowValue)} $unit (لا يوجد معدل تصميم للمقارنة)"
            )
        }

        val deviation = ((flowValue - designFlow) / designFlow) * 100.0
        val fd = EngineeringThresholds.FlowDeviation

        return when {
            kotlin.math.abs(deviation) >= fd.CRITICAL_PERCENT -> ValidationResult(
                parameter = "Flow Rate",
                parameterAr = "معدل التدفق",
                value = flowValue,
                unit = unit,
                status = ReadingStatus.CRITICAL,
                message = "Flow ${fmt(flowValue)} $unit — ${fmt(kotlin.math.abs(deviation))}% ${if (deviation > 0) "above" else "below"} design (${fmt(designFlow)} $unit)",
                messageAr = "التدفق ${fmt(flowValue)} $unit — ${fmt(kotlin.math.abs(deviation))}% ${if (deviation > 0) "فوق" else "تحت"} التصميم",
                recommendation = if (deviation > 0) "Reduce inlet valve opening. Check control valve." else "Check for blockage, valve closure, or pump issues.",
                recommendationAr = if (deviation > 0) "قلل فتح صمام الإمداد. تحقق من صمام التحكم." else "تحقق من انسداد، إغلاق صمام، أو مشكلة في المضخة."
            )
            kotlin.math.abs(deviation) >= fd.WARNING_PERCENT -> ValidationResult(
                parameter = "Flow Rate",
                parameterAr = "معدل التدفق",
                value = flowValue,
                unit = unit,
                status = ReadingStatus.WARNING,
                message = "Flow ${fmt(flowValue)} $unit — ${fmt(kotlin.math.abs(deviation))}% ${if (deviation > 0) "above" else "below"} design",
                messageAr = "التدفق ${fmt(flowValue)} $unit — ${fmt(kotlin.math.abs(deviation))}% ${if (deviation > 0) "فوق" else "تحت"} التصميم",
                recommendation = "Monitor trend. Adjust control valve if deviation continues.",
                recommendationAr = "راقب الاتجاه. اضبط صمام التحكم إذا استمر الانحراف."
            )
            else -> ValidationResult(
                parameter = "Flow Rate",
                parameterAr = "معدل التدفق",
                value = flowValue,
                unit = unit,
                status = ReadingStatus.NORMAL,
                message = "Flow rate normal: ${fmt(flowValue)} $unit (${fmt(deviation, true)}% of design)",
                messageAr = "معدل التدفق طبيعي: ${fmt(flowValue)} $unit"
            )
        }
    }

    private fun fmt(v: Double, signed: Boolean = false): String {
        val s = String.format("%.1f", v)
        return if (signed && v > 0) "+$s" else s
    }
}
