package cn.anitabi.navigator.ui.map

import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.PilgrimagePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MapMarkerOptionsTest {
    private val point = PilgrimagePoint(
        id = "point",
        name = "Point",
        coordinate = GeoPoint(latitude = 35.0, longitude = 135.0),
    )

    @Test
    fun `pilgrimage markers do not require bitmap descriptor initialization`() {
        val selected = pilgrimageMarkerOptions(point, selected = true)
        val unselected = pilgrimageMarkerOptions(point, selected = false)

        assertNull(selected.icon)
        assertNull(unselected.icon)
        assertEquals(1f, selected.alpha, 0f)
        assertEquals(0.62f, unselected.alpha, 0f)
        assertEquals(1f, selected.zIndex, 0f)
        assertEquals(0f, unselected.zIndex, 0f)
    }

    @Test
    fun `preview markers do not require bitmap descriptor initialization`() {
        val routePoint = routePointMarkerOptions(point)
        val currentLocation = currentLocationMarkerOptions(point.coordinate, "Current location")

        assertNull(routePoint.icon)
        assertNull(currentLocation.icon)
        assertEquals(0f, routePoint.zIndex, 0f)
        assertEquals(2f, currentLocation.zIndex, 0f)
    }
}
