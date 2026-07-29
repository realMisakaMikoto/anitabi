package cn.anitabi.navigator.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingReadinessTest {
    @Test
    fun `cannot finish until permissions and key are ready`() {
        assertFalse(
            OnboardingReadiness(
                hasLocationPermission = true,
                hasNotificationPermission = true,
                hasOrsKey = false,
            ).canFinish,
        )
        assertFalse(
            OnboardingReadiness(
                hasLocationPermission = false,
                hasNotificationPermission = true,
                hasOrsKey = true,
            ).canFinish,
        )
        assertTrue(
            OnboardingReadiness(
                hasLocationPermission = true,
                hasNotificationPermission = true,
                hasOrsKey = true,
            ).canFinish,
        )
    }

    @Test
    fun `permission error names every missing permission`() {
        assertEquals(
            "还需要定位和通知权限，请授权后继续",
            onboardingPermissionError(
                hasLocationPermission = false,
                hasNotificationPermission = false,
            ),
        )
        assertEquals(
            "还需要定位权限，请授权后继续",
            onboardingPermissionError(
                hasLocationPermission = false,
                hasNotificationPermission = true,
            ),
        )
        assertEquals(
            "还需要通知权限，请授权后继续",
            onboardingPermissionError(
                hasLocationPermission = true,
                hasNotificationPermission = false,
            ),
        )
        assertNull(
            onboardingPermissionError(
                hasLocationPermission = true,
                hasNotificationPermission = true,
            ),
        )
    }
}
