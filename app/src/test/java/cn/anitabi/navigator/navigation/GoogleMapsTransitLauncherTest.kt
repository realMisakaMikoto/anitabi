package cn.anitabi.navigator.navigation

import cn.anitabi.navigator.core.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleMapsTransitLauncherTest {
    private val origin = GeoPoint(12.345678, -98.765432)
    private val destination = GeoPoint(-1.25, 2.5)
    private val expectedUrl = "https://www.google.com/maps/dir/?api=1" +
        "&origin=12.345678%2C-98.765432" +
        "&destination=-1.25%2C2.5" +
        "&travelmode=transit"

    @Test
    fun `url spec contains only the supported transit directions parameters`() {
        val spec = googleMapsTransitUrlSpec(origin, destination)

        assertEquals("https", spec.scheme)
        assertEquals("www.google.com", spec.authority)
        assertEquals("/maps/dir/", spec.path)
        assertEquals(
            listOf(
                "api" to "1",
                "origin" to "12.345678,-98.765432",
                "destination" to "-1.25,2.5",
                "travelmode" to "transit",
            ),
            spec.queryParameters,
        )
    }

    @Test
    fun `google maps success does not open a browser fallback`() {
        val attempts = mutableListOf<LaunchAttempt>()
        val launcher = launcher(attempts) { packageName -> packageName == GOOGLE_MAPS_PACKAGE }

        assertTrue(launcher.launch(origin, destination))
        assertEquals(listOf(LaunchAttempt(expectedUrl, GOOGLE_MAPS_PACKAGE)), attempts)
    }

    @Test
    fun `missing google maps falls back to the same https url`() {
        val attempts = mutableListOf<LaunchAttempt>()
        val launcher = launcher(attempts) { packageName -> packageName == null }

        assertTrue(launcher.launch(origin, destination))
        assertEquals(
            listOf(
                LaunchAttempt(expectedUrl, GOOGLE_MAPS_PACKAGE),
                LaunchAttempt(expectedUrl, null),
            ),
            attempts,
        )
    }

    @Test
    fun `failure of both launch targets returns false`() {
        val attempts = mutableListOf<LaunchAttempt>()
        val launcher = launcher(attempts) { false }

        assertFalse(launcher.launch(origin, destination))
        assertEquals(
            listOf(
                LaunchAttempt(expectedUrl, GOOGLE_MAPS_PACKAGE),
                LaunchAttempt(expectedUrl, null),
            ),
            attempts,
        )
    }

    @Test
    fun `android starter does not launch when no handler resolves`() {
        var startCalled = false

        assertFalse(
            startResolvableGoogleMapsIntent(hasHandler = false) {
                startCalled = true
            },
        )
        assertFalse(startCalled)
    }

    @Test
    fun `android starter treats security exception as launch failure`() {
        assertFalse(
            startResolvableGoogleMapsIntent(hasHandler = true) {
                throw SecurityException("blocked")
            },
        )
    }

    @Test
    fun `android starter reports a resolved successful launch`() {
        assertTrue(startResolvableGoogleMapsIntent(hasHandler = true) {})
    }

    private fun launcher(
        attempts: MutableList<LaunchAttempt>,
        succeeds: (String?) -> Boolean,
    ): GoogleMapsTransitLauncher = GoogleMapsTransitLauncher(
        urlFactory = GoogleMapsTransitUrlFactory { _, _ -> expectedUrl },
        starter = GoogleMapsTransitStarter { url, packageName ->
            attempts += LaunchAttempt(url, packageName)
            succeeds(packageName)
        },
    )

    private data class LaunchAttempt(
        val url: String,
        val packageName: String?,
    )

    private companion object {
        const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"
    }
}
