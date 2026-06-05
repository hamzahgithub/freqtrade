package com.oilsite.analyzer.domain.engineering

import com.oilsite.analyzer.data.model.ReadingStatus
import com.oilsite.analyzer.data.model.ValidationResult

object TankLevelValidator {

    private val t = EngineeringThresholds.TankLevel

    fun validate(levelPercent: Double): ValidationResult = when {
        levelPercent < t.LOW_LOW -> ValidationResult(
            parameter = "Tank Level",
            parameterAr = "مستوى الخزان",
            value = levelPercent,
            unit = "%",
            status = ReadingStatus.CRITICAL,
            message = "Low-Low level: ${fmt(levelPercent)}% — pump cavitation risk",
            messageAr = "مستوى منخفض جداً: ${fmt(levelPercent)}% — خطر تجويف المضخة",
            recommendation = "Stop transfer pump immediately. Check inlet source.",
            recommendationAr = "أوقف مضخة النقل فوراً. تحقق من مصدر الإمداد."
        )
        levelPercent < t.LOW -> ValidationResult(
            parameter = "Tank Level",
            parameterAr = "مستوى الخزان",
            value = levelPercent,
            unit = "%",
            status = ReadingStatus.WARNING,
            message = "Low level: ${fmt(levelPercent)}% — approaching Low-Low alarm",
            messageAr = "مستوى منخفض: ${fmt(levelPercent)}% — يقترب من حد الإيقاف",
            recommendation = "Reduce discharge or increase inlet flow.",
            recommendationAr = "قلل معدل الضخ أو زيادة التدفق الواردة."
        )
        levelPercent <= t.NORMAL_MAX -> ValidationResult(
            parameter = "Tank Level",
            parameterAr = "مستوى الخزان",
            value = levelPercent,
            unit = "%",
            status = ReadingStatus.NORMAL,
            message = "Tank level normal: ${fmt(levelPercent)}%",
            messageAr = "مستوى الخزان طبيعي: ${fmt(levelPercent)}%"
        )
        levelPercent <= t.HIGH -> ValidationResult(
            parameter = "Tank Level",
            parameterAr = "مستوى الخزان",
            value = levelPercent,
            unit = "%",
            status = ReadingStatus.WARNING,
            message = "High level: ${fmt(levelPercent)}% — approaching high alarm",
            messageAr = "مستوى مرتفع: ${fmt(levelPercent)}% — يقترب من حد التحذير",
            recommendation = "Increase export/discharge rate.",
            recommendationAr = "زيادة معدل التصدير/الضخ."
        )
        else -> ValidationResult(
            parameter = "Tank Level",
            parameterAr = "مستوى الخزان",
            value = levelPercent,
            unit = "%",
            status = ReadingStatus.CRITICAL,
            message = "High-High level: ${fmt(levelPercent)}% — overflow risk",
            messageAr = "مستوى مرتفع جداً: ${fmt(levelPercent)}% — خطر الفيضان",
            recommendation = "STOP all inlet sources. Open emergency overflow if available.",
            recommendationAr = "أوقف جميع مصادر الإمداد. افتح صمام الطوارئ إن توفر."
        )
    }

    private fun fmt(v: Double) = String.format("%.1f", v)
}
