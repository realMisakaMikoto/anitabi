package cn.anitabi.navigator.ui.search

import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.PilgrimagePoint
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoBoundsTest {
    @Test
    fun `point inside visible map bounds is included`() {
        val bounds = GeoBounds(north = 36.0, east = 140.0, south = 35.0, west = 139.0)

        assertTrue(bounds.contains(pointAt(latitude = 35.5, longitude = 139.5)))
        assertFalse(bounds.contains(pointAt(latitude = 34.9, longitude = 139.5)))
    }

    @Test
    fun `bounds crossing antimeridian include both sides`() {
        val bounds = GeoBounds(north = 20.0, east = -170.0, south = -20.0, west = 170.0)

        assertTrue(bounds.contains(pointAt(latitude = 0.0, longitude = 175.0)))
        assertTrue(bounds.contains(pointAt(latitude = 0.0, longitude = -175.0)))
        assertFalse(bounds.contains(pointAt(latitude = 0.0, longitude = 0.0)))
    }

    private fun pointAt(latitude: Double, longitude: Double) = PilgrimagePoint(
        id = "point",
        name = "Point",
        coordinate = GeoPoint(latitude, longitude),
    )
}
