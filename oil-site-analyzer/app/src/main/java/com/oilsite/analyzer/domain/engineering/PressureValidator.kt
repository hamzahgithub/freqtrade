package com.oilsite.analyzer.domain.engineering

import com.oilsite.analyzer.data.model.ReadingStatus
import com.oilsite.analyzer.data.model.ValidationResult

object PressureValidator {

    fun validate(pressureBarg: Double, mawpBarg: Double? = null): ValidationResult {
        val t = EngineeringThresholds.SeparatorPressure

        return when {
            pressureBarg < 0 -> ValidationResult(
                parameter = "Pressure",
                parameterAr = "الضغط",
                value = pressureBarg,
                unit = "barg",
                status = ReadingStatus.CRITICAL,
                message = "Negative pressure — vacuum condition or sensor fault",
                messageAr = "ضغط سالب — تفريغ هواء أو خلل في المستشعر",
                recommendation = "Check sensor calibration. Verify vessel is not under vacuum.",
                recommendationAr = "تحقق من معايرة المستشعر. تأكد من عدم وجود تفريغ في الوعاء."
            )
            pressureBarg < t.LOW_WARNING -> ValidationResult(
                parameter = "Pressure",
                parameterAr = "الضغط",
                value = pressureBarg,
                unit = "barg",
                status = ReadingStatus.WARNING,
                message = "Low pressure — potential gas blowby or inlet supply issue",
                messageAr = "ضغط منخفض — خطر تسرب الغاز أو مشكلة في مصدر الإمداد",
                recommendation = "Check inlet choke valve, upstream separator, and gas source pressure.",
                recommendationAr = "تحقق من صمام الخنق، الفاصل المتصل، وضغط مصدر الغاز."
            )
            mawpBarg != null && pressureBarg >= mawpBarg * t.HIGH_CRITICAL_PERCENT -> ValidationResult(
                parameter = "Pressure",
                parameterAr = "الضغط",
                value = pressureBarg,
                unit = "barg",
                status = ReadingStatus.CRITICAL,
                message = "Pressure at ${fmt(pressureBarg)} barg — exceeds 95% of MAWP (${fmt(mawpBarg)} barg)",
                messageAr = "الضغط ${fmt(pressureBarg)} barg — يتجاوز 95% من الضغط الأقصى (${fmt(mawpBarg)} barg)",
                recommendation = "IMMEDIATE: Check PSV operation. Reduce inlet flow. Prepare for ESD.",
                recommendationAr = "فوري: تحقق من صمام الأمان. قلل التدفق. استعد للإيقاف الطارئ."
            )
            mawpBarg != null && pressureBarg >= mawpBarg * t.HIGH_WARNING_PERCENT -> ValidationResult(
                parameter = "Pressure",
                parameterAr = "الضغط",
                value = pressureBarg,
                unit = "barg",
                status = ReadingStatus.WARNING,
                message = "Pressure at ${fmt(pressureBarg)} barg — approaching 90% of MAWP (${fmt(mawpBarg)} barg)",
                messageAr = "الضغط ${fmt(pressureBarg)} barg — يقترب من 90% من الضغط الأقصى",
                recommendation = "Increase back-pressure control valve opening. Monitor closely.",
                recommendationAr = "افتح صمام التحكم في الضغط. مراقبة مستمرة."
            )
            pressureBarg > t.NORMAL_MAX -> ValidationResult(
                parameter = "Pressure",
                parameterAr = "الضغط",
                value = pressureBarg,
                unit = "barg",
                status = ReadingStatus.WARNING,
                message = "High pressure reading: ${fmt(pressureBarg)} barg — verify MAWP for this vessel",
                messageAr = "قراءة ضغط عالية: ${fmt(pressureBarg)} barg — تحقق من الضغط الأقصى للوعاء"
            )
            else -> ValidationResult(
                parameter = "Pressure",
                parameterAr = "الضغط",
                value = pressureBarg,
                unit = "barg",
                status = ReadingStatus.NORMAL,
                message = "Pressure normal: ${fmt(pressureBarg)} barg",
                messageAr = "الضغط طبيعي: ${fmt(pressureBarg)} barg"
            )
        }
    }

    fun validatePsi(pressurePsi: Double, mawpPsi: Double? = null): ValidationResult {
        val barg = (pressurePsi - 14.696) / 14.504
        val mawpBarg = mawpPsi?.let { (it - 14.696) / 14.504 }
        return validate(barg, mawpBarg).copy(unit = "psi", value = pressurePsi)
    }

    private fun fmt(v: Double) = String.format("%.1f", v)
}
