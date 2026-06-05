package com.oilsite.analyzer.domain.engineering

import com.oilsite.analyzer.data.model.ReadingStatus
import org.junit.Assert.*
import org.junit.Test

class TankLevelValidatorTest {

    @Test fun `level below 10 returns CRITICAL (LL)`() =
        assertEquals(ReadingStatus.CRITICAL, TankLevelValidator.validate(7.5).status)

    @Test fun `level 10 to 20 returns WARNING (L)`() =
        assertEquals(ReadingStatus.WARNING, TankLevelValidator.validate(15.0).status)

    @Test fun `level 20 to 85 returns NORMAL`() =
        listOf(20.0, 50.0, 80.0, 85.0).forEach {
            assertEquals("NORMAL expected for $it%", ReadingStatus.NORMAL, TankLevelValidator.validate(it).status)
        }

    @Test fun `level 85 to 92 returns WARNING (H)`() =
        assertEquals(ReadingStatus.WARNING, TankLevelValidator.validate(88.2).status)

    @Test fun `level above 92 returns CRITICAL (HH)`() =
        assertEquals(ReadingStatus.CRITICAL, TankLevelValidator.validate(95.0).status)

    @Test fun `LL result has stop-pump recommendation`() {
        val r = TankLevelValidator.validate(5.0)
        assertNotNull(r.recommendationAr)
        assertTrue("Recommendation must mention stopping pump",
            r.recommendationAr!!.contains("مضخة") || r.recommendationAr.contains("أوقف"))
    }
}
