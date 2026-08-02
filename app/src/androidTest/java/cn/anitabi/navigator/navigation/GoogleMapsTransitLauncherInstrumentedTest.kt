package cn.anitabi.navigator.navigation

import androidx.test.ext.junit.runners.AndroidJUnit4
import cn.anitabi.navigator.core.model.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoogleMapsTransitLauncherInstrumentedTest {
    @Test
    fun directionsUrlUsesEncodedCoordinatesAndOnlySupportedParameters() {
        val url = googleMapsTransitDirectionsUrl(
            origin = GeoPoint(12.345678, -98.765432),
            destination = GeoPoint(-1.25, 2.5),
        )

        assertEquals(
            "https://www.google.com/maps/dir/?api=1" +
                "&origin=12.345678%2C-98.765432" +
                "&destination=-1.25%2C2.5" +
                "&travelmode=transit",
            url,
        )
    }
}
