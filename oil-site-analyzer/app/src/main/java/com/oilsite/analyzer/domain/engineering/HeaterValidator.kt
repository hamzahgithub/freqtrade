package com.oilsite.analyzer.domain.engineering

import com.oilsite.analyzer.data.model.ReadingStatus
import com.oilsite.analyzer.data.model.ValidationResult

object HeaterValidator {

    private val t = EngineeringThresholds.HeaterBath
    private val dt = EngineeringThresholds.HeatTransfer

    fun validateBathTemperature(tempC: Double): ValidationResult = when {
        tempC < t.UNDERHEAT_LIMIT -> ValidationResult(
            parameter = "Bath Temperature",
            parameterAr = "حرارة الباث",
            value = tempC,
            unit = "°C",
            status = ReadingStatus.UNDERHEAT,
            message = "Bath temperature below minimum operating level (${t.UNDERHEAT_LIMIT}°C)",
            messageAr = "حرارة الباث أقل من الحد الأدنى للتشغيل (${t.UNDERHEAT_LIMIT}°C)",
            recommendation = "Check burner ignition, fuel supply (gas/oil), and thermostat setpoint.",
            recommendationAr = "تحقق من اشتعال الموقد ومصدر الوقود وضبط الثيرموستات."
        )
        tempC <= t.NORMAL_MAX -> ValidationResult(
            parameter = "Bath Temperature",
            parameterAr = "حرارة الباث",
            value = tempC,
            unit = "°C",
            status = ReadingStatus.NORMAL,
            message = "Bath temperature within safe operating range (${t.NORMAL_MIN}–${t.NORMAL_MAX}°C)",
            messageAr = "حرارة الباث ضمن نطاق التشغيل الآمن (${t.NORMAL_MIN}–${t.NORMAL_MAX}°C)"
        )
        tempC <= t.WARNING_HIGH -> ValidationResult(
            parameter = "Bath Temperature",
            parameterAr = "حرارة الباث",
            value = tempC,
            unit = "°C",
            status = ReadingStatus.WARNING,
            message = "Bath temperature approaching high limit — reduce burner output",
            messageAr = "حرارة الباث تقترب من الحد الأعلى — قلل قدرة الموقد",
            recommendation = "Reduce burner firing rate. Monitor closely every 10 minutes.",
            recommendationAr = "خفض معدل حرق الموقد. المراقبة كل 10 دقائق."
        )
        else -> ValidationResult(
            parameter = "Bath Temperature",
            parameterAr = "حرارة الباث",
            value = tempC,
            unit = "°C",
            status = ReadingStatus.CRITICAL,
            message = "CRITICAL: Bath temperature dangerously high — risk of steam/boiling",
            messageAr = "حرج: حرارة الباث مرتفعة جداً — خطر الغليان",
            recommendation = "IMMEDIATE: Shut off burner. Do NOT open bath vent until temp drops below 80°C.",
            recommendationAr = "فوري: أوقف الموقد. لا تفتح صمام الباث حتى تنخفض الحرارة تحت 80°C."
        )
    }

    fun validateHeatTransferEfficiency(inletTempC: Double, outletTempC: Double): ValidationResult {
        val deltaT = outletTempC - inletTempC
        return when {
            deltaT < 0 -> ValidationResult(
                parameter = "Heat Transfer ΔT (Outlet - Inlet)",
                parameterAr = "فرق الحرارة (خروج - دخول)",
                value = deltaT,
                unit = "°C",
                status = ReadingStatus.CRITICAL,
                message = "Outlet temp lower than inlet — possible sensor fault or flow reversal",
                messageAr = "حرارة الخروج أقل من الدخول — خطأ في المستشعر أو انعكاس التدفق",
                recommendation = "Verify sensor wiring and flow direction immediately.",
                recommendationAr = "تحقق فوراً من توصيلات المستشعر واتجاه التدفق."
            )
            deltaT < dt.FAIR_DT -> ValidationResult(
                parameter = "Heat Transfer ΔT",
                parameterAr = "فرق الحرارة",
                value = deltaT,
                unit = "°C",
                status = ReadingStatus.WARNING,
                message = "Poor heat transfer: ΔT = ${fmt(deltaT)}°C (expected ≥ ${dt.FAIR_DT}°C)",
                messageAr = "ضعف في نقل الحرارة: ΔT = ${fmt(deltaT)}°C (المتوقع ≥ ${dt.FAIR_DT}°C)",
                recommendation = "Possible causes: tube bundle fouling, excessive flow rate, low bath temp.",
                recommendationAr = "الأسباب المحتملة: ترسبات على الأنابيب، معدل تدفق عالٍ، حرارة باث منخفضة."
            )
            deltaT < dt.GOOD_DT -> ValidationResult(
                parameter = "Heat Transfer ΔT",
                parameterAr = "فرق الحرارة",
                value = deltaT,
                unit = "°C",
                status = ReadingStatus.WARNING,
                message = "Fair heat transfer: ΔT = ${fmt(deltaT)}°C — monitor trend",
                messageAr = "نقل حرارة متوسط: ΔT = ${fmt(deltaT)}°C — راقب الاتجاه",
                recommendation = "Schedule tube cleaning inspection if ΔT continues declining.",
                recommendationAr = "جدول فحص تنظيف الأنابيب إذا استمر انخفاض ΔT."
            )
            deltaT < dt.EXCELLENT_DT -> ValidationResult(
                parameter = "Heat Transfer ΔT",
                parameterAr = "فرق الحرارة",
                value = deltaT,
                unit = "°C",
                status = ReadingStatus.NORMAL,
                message = "Good heat transfer: ΔT = ${fmt(deltaT)}°C",
                messageAr = "نقل حرارة جيد: ΔT = ${fmt(deltaT)}°C"
            )
            else -> ValidationResult(
                parameter = "Heat Transfer ΔT",
                parameterAr = "فرق الحرارة",
                value = deltaT,
                unit = "°C",
                status = ReadingStatus.NORMAL,
                message = "Excellent heat transfer: ΔT = ${fmt(deltaT)}°C",
                messageAr = "نقل حرارة ممتاز: ΔT = ${fmt(deltaT)}°C"
            )
        }
    }

    fun validateInletTemperature(tempC: Double): ValidationResult {
        val pt = EngineeringThresholds.ProcessTemp
        return when {
            tempC < pt.HYDRATE_RISK_BELOW -> ValidationResult(
                parameter = "Inlet Temperature",
                parameterAr = "حرارة الدخول",
                value = tempC,
                unit = "°C",
                status = ReadingStatus.WARNING,
                message = "Low inlet temp — hydrate formation risk below ${pt.HYDRATE_RISK_BELOW}°C",
                messageAr = "حرارة دخول منخفضة — خطر تكون هيدرات أقل من ${pt.HYDRATE_RISK_BELOW}°C",
                recommendation = "Ensure heater is operating. Check for hydrate inhibitor injection.",
                recommendationAr = "تأكد من تشغيل الهيتر. تحقق من حقن مانع الهيدرات."
            )
            tempC > pt.CRUDE_MAX_WARNING -> ValidationResult(
                parameter = "Inlet Temperature",
                parameterAr = "حرارة الدخول",
                value = tempC,
                unit = "°C",
                status = ReadingStatus.WARNING,
                message = "High inlet temperature — check upstream source",
                messageAr = "حرارة دخول مرتفعة — تحقق من المصدر المتصل",
                recommendation = "Verify no unintended heating upstream. Check for cross-flow with hot stream.",
                recommendationAr = "تأكد من عدم وجود تسخين غير مقصود في المنبع."
            )
            else -> ValidationResult(
                parameter = "Inlet Temperature",
                parameterAr = "حرارة الدخول",
                value = tempC,
                unit = "°C",
                status = ReadingStatus.NORMAL,
                message = "Inlet temperature normal: ${fmt(tempC)}°C",
                messageAr = "حرارة الدخول طبيعية: ${fmt(tempC)}°C"
            )
        }
    }

    fun validateOutletTemperature(tempC: Double, designOutletC: Double? = null): ValidationResult {
        val pt = EngineeringThresholds.ProcessTemp
        return when {
            tempC < pt.HYDRATE_RISK_BELOW -> ValidationResult(
                parameter = "Outlet Temperature",
                parameterAr = "حرارة الخروج",
                value = tempC,
                unit = "°C",
                status = ReadingStatus.CRITICAL,
                message = "Outlet temperature critically low — heater not functioning",
                messageAr = "حرارة الخروج منخفضة جداً — الهيتر لا يعمل",
                recommendation = "Check burner flame, fuel supply, bath level, and tube integrity.",
                recommendationAr = "تحقق من لهب الموقد، الوقود، مستوى الباث، وسلامة الأنابيب."
            )
            designOutletC != null && tempC < designOutletC * 0.85 -> ValidationResult(
                parameter = "Outlet Temperature",
                parameterAr = "حرارة الخروج",
                value = tempC,
                unit = "°C",
                status = ReadingStatus.WARNING,
                message = "Outlet temp ${fmt(tempC)}°C is 15%+ below design (${fmt(designOutletC)}°C)",
                messageAr = "حرارة الخروج ${fmt(tempC)}°C أقل من التصميم بـ 15%+ (${fmt(designOutletC)}°C)",
                recommendation = "Increase bath temperature or reduce throughput.",
                recommendationAr = "ارفع حرارة الباث أو قلل معدل التدفق."
            )
            tempC > pt.CRUDE_MAX_WARNING -> ValidationResult(
                parameter = "Outlet Temperature",
                parameterAr = "حرارة الخروج",
                value = tempC,
                unit = "°C",
                status = ReadingStatus.WARNING,
                message = "High outlet temperature — risk of overheating process fluid",
                messageAr = "حرارة خروج مرتفعة — خطر ارتفاع درجة حرارة سائل العملية",
                recommendation = "Reduce bath temperature or increase flow rate.",
                recommendationAr = "خفض حرارة الباث أو زيادة معدل التدفق."
            )
            else -> ValidationResult(
                parameter = "Outlet Temperature",
                parameterAr = "حرارة الخروج",
                value = tempC,
                unit = "°C",
                status = ReadingStatus.NORMAL,
                message = "Outlet temperature normal: ${fmt(tempC)}°C",
                messageAr = "حرارة الخروج طبيعية: ${fmt(tempC)}°C"
            )
        }
    }

    private fun fmt(v: Double) = String.format("%.1f", v)
}
