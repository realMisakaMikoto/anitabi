package cn.anitabi.navigator.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GeoPointTest {
    @Test
    fun `Anitabi latitude longitude converts to GeoJSON longitude latitude`() {
        val point = GeoPoint.fromAnitabiGeo(listOf(35.6812, 139.7671))

        assertEquals(listOf(139.7671, 35.6812), point.toGeoJsonPosition())
    }

    @Test
    fun `invalid latitude is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            GeoPoint(latitude = 91.0, longitude = 0.0)
        }
    }
}
