package cn.anitabi.navigator.ui.planner

import android.Manifest
import cn.anitabi.navigator.navigation.hasVisibleExternalTransitControl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalTransitPermissionTest {
    @Test
    fun `fine and coarse location are requested together for Japan transit`() {
        assertEquals(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.POST_NOTIFICATIONS,
            ),
            externalTransitRuntimePermissions(
                hasFineLocation = false,
                requestNotificationPermission = true,
            ),
        )
        assertEquals(
            emptyList<String>(),
            externalTransitRuntimePermissions(
                hasFineLocation = true,
                requestNotificationPermission = false,
            ),
        )
    }

    @Test
    fun `overlay-only and notification-only controls are equivalent`() {
        var notificationsCheckedWithOverlay = false
        val overlayOnly = hasVisibleExternalTransitControl(
            overlayVisible = { true },
            notificationsVisible = {
                notificationsCheckedWithOverlay = true
                false
            },
        )
        val notificationOnly = hasVisibleExternalTransitControl(
            overlayVisible = { false },
            notificationsVisible = { true },
        )

        assertTrue(overlayOnly)
        assertTrue(notificationOnly)
        assertEquals(false, notificationsCheckedWithOverlay)
        assertNull(externalTransitPermissionError(hasFineLocation = true, overlayOnly))
        assertNull(externalTransitPermissionError(hasFineLocation = true, notificationOnly))
    }

    @Test
    fun `missing both visible controls is rejected`() {
        val hasVisibleControl = hasVisibleExternalTransitControl(
            overlayVisible = { false },
            notificationsVisible = { false },
        )

        assertEquals(
            "请至少启用悬浮窗或可见的导航通知",
            externalTransitPermissionError(hasFineLocation = true, hasVisibleControl),
        )
    }

    @Test
    fun `missing fine location is rejected even with a visible control`() {
        assertEquals(
            "需要精确定位权限才能开始日本公交分段导航",
            externalTransitPermissionError(
                hasFineLocation = false,
                hasVisibleControl = true,
            ),
        )
    }

    @Test
    fun `missing fine location and visible controls reports both requirements`() {
        assertEquals(
            "需要精确定位，并至少启用悬浮窗或可见的导航通知",
            externalTransitPermissionError(
                hasFineLocation = false,
                hasVisibleControl = false,
            ),
        )
    }
}
