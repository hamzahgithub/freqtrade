package com.oilsite.analyzer.domain.engineering

import com.oilsite.analyzer.data.model.ReadingStatus
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for HeaterValidator — runs on JVM, no device needed.
 * Run with: ./gradlew test  OR  Right-click in Android Studio → Run Tests
 */
class HeaterValidatorTest {

    // ─── Bath Temperature ──────────────────────────────────────────────────────

    @Test
    fun `bath temp below 55 returns UNDERHEAT`() {
        listOf(0.0, 20.0, 38.5, 54.9).forEach { t ->
            val result = HeaterValidator.validateBathTemperature(t)
            assertEquals("Expected UNDERHEAT for $t°C", ReadingStatus.UNDERHEAT, result.status)
        }
    }

    @Test
    fun `bath temp 55 to 88 returns NORMAL`() {
        listOf(55.0, 60.0, 72.5, 80.0, 88.0).forEach { t ->
            val result = HeaterValidator.validateBathTemperature(t)
            assertEquals("Expected NORMAL for $t°C", ReadingStatus.NORMAL, result.status)
        }
    }

    @Test
    fun `bath temp 88 to 95 returns WARNING`() {
        listOf(88.1, 91.3, 94.9, 95.0).forEach { t ->
            val result = HeaterValidator.validateBathTemperature(t)
            assertEquals("Expected WARNING for $t°C", ReadingStatus.WARNING, result.status)
        }
    }

    @Test
    fun `bath temp above 95 returns CRITICAL`() {
        listOf(95.1, 97.8, 99.0, 105.0).forEach { t ->
            val result = HeaterValidator.validateBathTemperature(t)
            assertEquals("Expected CRITICAL for $t°C", ReadingStatus.CRITICAL, result.status)
        }
    }

    @Test
    fun `bath temp at exact boundary 88 is NORMAL not WARNING`() {
        val result = HeaterValidator.validateBathTemperature(88.0)
        assertEquals(ReadingStatus.NORMAL, result.status)
    }

    @Test
    fun `bath temp critical result has non-empty recommendation`() {
        val result = HeaterValidator.validateBathTemperature(98.0)
        assertFalse("Critical result must have recommendation", result.recommendationAr.isNullOrBlank())
    }

    // ─── Heat Transfer ΔT ──────────────────────────────────────────────────────

    @Test
    fun `negative delta T returns CRITICAL`() {
        val result = HeaterValidator.validateHeatTransferEfficiency(60.0, 50.0)
        assertEquals(ReadingStatus.CRITICAL, result.status)
        assertTrue("ΔT must be negative", result.value < 0)
    }

    @Test
    fun `delta T below 5 returns WARNING`() {
        val result = HeaterValidator.validateHeatTransferEfficiency(20.0, 22.0)
        assertEquals("ΔT=2 → WARNING", ReadingStatus.WARNING, result.status)
    }

    @Test
    fun `delta T 5 to 10 returns WARNING (fair)`() {
        val result = HeaterValidator.validateHeatTransferEfficiency(20.0, 27.0)
        assertEquals("ΔT=7 → WARNING", ReadingStatus.WARNING, result.status)
    }

    @Test
    fun `delta T above 10 returns NORMAL`() {
        listOf(
            Pair(18.0, 55.8),   // ΔT = 37.8 (normal heater)
            Pair(10.0, 32.0),   // ΔT = 22.0
            Pair(5.0, 15.5)     // ΔT = 10.5
        ).forEach { (inlet, outlet) ->
            val result = HeaterValidator.validateHeatTransferEfficiency(inlet, outlet)
            assertEquals(
                "Expected NORMAL for inlet=$inlet outlet=$outlet",
                ReadingStatus.NORMAL, result.status
            )
        }
    }

    @Test
    fun `delta T value is correctly computed as outlet minus inlet`() {
        val result = HeaterValidator.validateHeatTransferEfficiency(18.2, 55.8)
        assertEquals(37.6, result.value, 0.01)
    }

    // ─── Inlet Temperature ─────────────────────────────────────────────────────

    @Test
    fun `inlet temp below 15 returns WARNING (hydrate risk)`() {
        listOf(5.0, 8.5, 14.2, 14.9).forEach { t ->
            val result = HeaterValidator.validateInletTemperature(t)
            assertEquals("Hydrate risk expected for $t°C", ReadingStatus.WARNING, result.status)
        }
    }

    @Test
    fun `inlet temp 15 to 90 returns NORMAL`() {
        listOf(15.0, 18.2, 45.0, 80.0, 90.0).forEach { t ->
            val result = HeaterValidator.validateInletTemperature(t)
            assertEquals("Expected NORMAL for $t°C", ReadingStatus.NORMAL, result.status)
        }
    }

    // ─── Outlet Temperature ────────────────────────────────────────────────────

    @Test
    fun `outlet temp below 15 returns CRITICAL`() {
        val result = HeaterValidator.validateOutletTemperature(11.3)
        assertEquals(ReadingStatus.CRITICAL, result.status)
    }

    @Test
    fun `outlet temp in normal range returns NORMAL`() {
        val result = HeaterValidator.validateOutletTemperature(55.8)
        assertEquals(ReadingStatus.NORMAL, result.status)
    }

    // ─── Arabic messages ───────────────────────────────────────────────────────

    @Test
    fun `all results have non-empty Arabic messages`() {
        val results = listOf(
            HeaterValidator.validateBathTemperature(72.0),
            HeaterValidator.validateBathTemperature(92.0),
            HeaterValidator.validateBathTemperature(97.0),
            HeaterValidator.validateBathTemperature(40.0),
            HeaterValidator.validateHeatTransferEfficiency(20.0, 55.0),
            HeaterValidator.validateInletTemperature(10.0),
            HeaterValidator.validateOutletTemperature(60.0)
        )
        results.forEach { r ->
            assertFalse("messageAr must not be empty for ${r.parameter}", r.messageAr.isBlank())
            assertFalse("parameterAr must not be empty", r.parameterAr.isBlank())
        }
    }
}
