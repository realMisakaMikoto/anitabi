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
            from = TransitPlaceDto("Tokyo", 35.0, 139.0, track = "3"),
            to = TransitPlaceDto("Ueno", 35.1, 139.1, scheduledTrack = "6"),
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
    }
}
