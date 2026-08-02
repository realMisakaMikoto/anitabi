package cn.anitabi.navigator.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingReadinessTest {
    @Test
    fun `location is required while notification can be deferred`() {
        assertFalse(
            OnboardingReadiness(
                hasLocationPermission = false,
                hasNotificationPermission = true,
            ).canFinish,
        )
        assertTrue(
            OnboardingReadiness(
                hasLocationPermission = true,
                hasNotificationPermission = false,
            ).canFinish,
        )
        assertTrue(
            OnboardingReadiness(
                hasLocationPermission = true,
                hasNotificationPermission = true,
            ).canFinish,
        )
    }

    @Test
    fun `permission error only blocks missing location`() {
        assertEquals(
            "还需要定位权限，请授权后继续",
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
        assertNull(
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
