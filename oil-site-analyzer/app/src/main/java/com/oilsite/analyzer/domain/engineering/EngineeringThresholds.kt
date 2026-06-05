package com.oilsite.analyzer.domain.engineering

/**
 * Industry-standard engineering thresholds for oil & gas surface facilities.
 * All temperatures in Celsius unless noted.
 */
object EngineeringThresholds {

    // ---- INDIRECT FIRED HEATER (Water Bath / Glycol Bath) ----
    object HeaterBath {
        const val UNDERHEAT_LIMIT = 55.0       // Below this: heater not working properly
        const val NORMAL_MIN = 55.0
        const val NORMAL_MAX = 88.0            // Safe operating range upper bound
        const val WARNING_HIGH = 95.0          // Start reducing burner output
        const val CRITICAL_HIGH = 99.0         // Risk of boiling (water bath ~100°C)
    }

    // ---- HEAT TRANSFER (ΔT = Outlet - Inlet) ----
    object HeatTransfer {
        const val EXCELLENT_DT = 20.0
        const val GOOD_DT = 10.0
        const val FAIR_DT = 5.0
        // Below FAIR_DT: poor heat transfer (fouling, low bath temp, high flow rate)
    }

    // ---- SEPARATOR OPERATING PRESSURE (barg - generic) ----
    object SeparatorPressure {
        const val LOW_WARNING = 2.0            // Below design: gas blowby risk
        const val NORMAL_MIN = 3.0
        const val NORMAL_MAX = 50.0
        const val HIGH_WARNING_PERCENT = 0.90  // 90% of MAWP = warning
        const val HIGH_CRITICAL_PERCENT = 0.95 // 95% of MAWP = critical
    }

    // ---- TANK LEVELS (%) ----
    object TankLevel {
        const val LOW_LOW = 10.0
        const val LOW = 20.0
        const val NORMAL_MIN = 20.0
        const val NORMAL_MAX = 80.0
        const val HIGH = 85.0
        const val HIGH_HIGH = 92.0
    }

    // ---- FLOW RATE DEVIATION (%) ----
    object FlowDeviation {
        const val WARNING_PERCENT = 15.0   // ±15% from design
        const val CRITICAL_PERCENT = 30.0  // ±30% from design
    }

    // ---- PROCESS TEMPERATURES (°C) ----
    object ProcessTemp {
        // Hydrate formation risk zone (depends on pressure, general guide)
        const val HYDRATE_RISK_BELOW = 15.0

        // Wax deposition risk (crude-dependent, general guide)
        const val WAX_RISK_BELOW = 30.0

        // Typical crude handling max temp
        const val CRUDE_MAX_NORMAL = 80.0
        const val CRUDE_MAX_WARNING = 90.0
    }
}
