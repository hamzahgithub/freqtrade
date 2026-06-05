package com.oilsite.analyzer.domain.engineering

import com.oilsite.analyzer.data.model.ReadingStatus
import org.junit.Assert.*
import org.junit.Test

class PressureValidatorTest {

    @Test
    fun `negative pressure returns CRITICAL`() {
        val r = PressureValidator.validate(-1.0)
        assertEquals(ReadingStatus.CRITICAL, r.status)
    }

    @Test
    fun `pressure below 2 barg returns WARNING`() {
        listOf(0.0, 0.8, 1.5, 1.99).forEach { p ->
            assertEquals("Expected WARNING for $p barg", ReadingStatus.WARNING, PressureValidator.validate(p).status)
        }
    }

    @Test
    fun `normal pressure 3 to 50 barg returns NORMAL`() {
        listOf(3.0, 8.4, 18.7, 22.1, 50.0).forEach { p ->
            assertEquals("Expected NORMAL for $p barg", ReadingStatus.NORMAL, PressureValidator.validate(p).status)
        }
    }

    @Test
    fun `PSI conversion: 50 psi returns correct status`() {
        // 50 psi → (50-14.696)/14.504 ≈ 2.43 barg → NORMAL
        val r = PressureValidator.validatePsi(50.0)
        assertEquals(ReadingStatus.NORMAL, r.status)
    }

    @Test
    fun `PSI display value stays in psi not barg`() {
        val r = PressureValidator.validatePsi(50.0)
        assertEquals("psi", r.unit)
        assertEquals(50.0, r.value, 0.001)
    }

    @Test
    fun `MAWP threshold: 95% triggers CRITICAL`() {
        // MAWP=100 barg → 95% = 95 barg → CRITICAL
        val r = PressureValidator.validate(96.0, mawpBarg = 100.0)
        assertEquals(ReadingStatus.CRITICAL, r.status)
    }

    @Test
    fun `MAWP threshold: 90% triggers WARNING`() {
        // MAWP=100 barg → 90% = 90 barg
        val r = PressureValidator.validate(91.0, mawpBarg = 100.0)
        assertEquals(ReadingStatus.WARNING, r.status)
    }
}
