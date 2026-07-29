package cn.anitabi.navigator.core.routing

import cn.anitabi.navigator.data.network.transit.TransitLegDto
import cn.anitabi.navigator.data.network.transit.TransitPlaceDto
import cn.anitabi.navigator.data.network.transit.TransitPolylineDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutingProvidersTest {
    @Test
    fun `transit mapper keeps line platforms stops realtime and cancellation`() {
        val leg = TransitLegDto(
            mode = "TRAIN",
            from = TransitPlaceDto("Tokyo", 35.0, 139.0, tz = "Asia/Tokyo", track = "3"),
            to = TransitPlaceDto("Ueno", 35.1, 139.1, tz = "Asia/Tokyo", scheduledTrack = "6"),
            duration = 600,
            startTime = "2026-07-29T23:55:00+09:00",
            endTime = "2026-07-30T00:05:00+09:00",
            distance = 5_000.0,
            headsign = "Ueno",
            routeShortName = "JY",
            intermediateStops = listOf(TransitPlaceDto("Akihabara", 35.05, 139.05)),
            legGeometry = TransitPolylineDto(),
            realTime = true,
            cancelled = true,
        ).toTourLeg()

        val transit = leg.transit!!
        assertEquals("JY", transit.line)
        assertEquals("Ueno", transit.direction)
        assertEquals("3", transit.departurePlatform)
        assertEquals("6", transit.arrivalPlatform)
        assertEquals(listOf("Akihabara"), transit.intermediateStops)
        assertTrue(transit.realtime)
        assertTrue(transit.cancelled)
        assertEquals("2026-07-30T00:05:00+09:00", transit.arrivalTime)
        assertEquals("Asia/Tokyo", transit.departureTimeZone)
        assertEquals("Asia/Tokyo", transit.arrivalTimeZone)
    }

    @Test
    fun `transit mapper derives missing distance and rejects single point geometry`() {
        val route = TransitLegDto(
            mode = "REGIONAL_RAIL",
            from = TransitPlaceDto("Tokyo", 35.0, 139.0),
            to = TransitPlaceDto("Ueno", 35.1, 139.1),
            duration = 600,
            startTime = "2026-07-30T00:00:00Z",
            endTime = "2026-07-30T00:10:00Z",
            distance = null,
            legGeometry = TransitPolylineDto("_p~iF~ps|U_ulLnnqC_mqNvxq`@", length = 3),
        ).toTourLeg()
        val stationary = TransitLegDto(
            mode = "WALK",
            from = TransitPlaceDto("Same", 35.0, 139.0),
            to = TransitPlaceDto("Same", 35.0, 139.0),
            duration = 0,
            startTime = "2026-07-30T00:00:00Z",
            endTime = "2026-07-30T00:00:00Z",
            legGeometry = TransitPolylineDto(),
        ).toTourLeg()

        assertTrue(route.distanceMeters > 0.0)
        assertEquals(route.distanceMeters, route.steps.single().distanceMeters, 0.001)
        assertEquals(3, route.geometry.size)
        assertEquals(1, stationary.geometry.size)
        assertEquals(0.0, stationary.distanceMeters, 0.001)
    }
}
