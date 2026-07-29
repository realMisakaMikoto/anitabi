package cn.anitabi.navigator.ui.planner

import org.junit.Assert.assertEquals
import org.junit.Test

class PlannerFormattingTest {
    @Test
    fun `transit UTC timestamps use the stop time zone`() {
        assertEquals("09:01", formatTransitTime("2026-07-30T00:01:00Z", "Asia/Tokyo"))
        assertEquals("00:01", formatTransitTime("2026-07-30T00:01:00Z", null))
        assertEquals("时间未知", formatTransitTime(null, "Asia/Tokyo"))
    }
}
