package cn.anitabi.navigator.ui.planner

import cn.anitabi.navigator.core.model.TravelMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlannerFormattingTest {
    @Test
    fun `transit UTC timestamps use the stop time zone`() {
        assertEquals("09:01", formatTransitTime("2026-07-30T00:01:00Z", "Asia/Tokyo"))
        assertEquals("00:01", formatTransitTime("2026-07-30T00:01:00Z", null))
        assertEquals("时间未知", formatTransitTime(null, "Asia/Tokyo"))
    }

    @Test
    fun `navigation requires both location and notification permissions`() {
        assertNull(navigationPermissionError(hasLocation = true, hasNotifications = true))
        assertEquals(
            "需要定位权限才能开始导航",
            navigationPermissionError(hasLocation = false, hasNotifications = true),
        )
        assertEquals(
            "需要通知权限才能在锁屏和后台持续导航",
            navigationPermissionError(hasLocation = true, hasNotifications = false),
        )
        assertEquals(
            "需要定位和通知权限才能开始导航",
            navigationPermissionError(hasLocation = false, hasNotifications = false),
        )
    }

    @Test
    fun `Google walking and cycling routes show beta notice`() {
        val notice = "Google 地图的步行和骑行路线仍为测试版，请以现场道路和交通规则为准。"

        assertEquals(notice, googleRouteBetaNotice(TravelMode.WALK))
        assertEquals(notice, googleRouteBetaNotice(TravelMode.BIKE))
        assertNull(googleRouteBetaNotice(TravelMode.DRIVE))
        assertNull(googleRouteBetaNotice(TravelMode.TRANSIT))
    }
}
