package cn.anitabi.navigator.core.navigation

import cn.anitabi.navigator.core.model.Anime
import cn.anitabi.navigator.core.model.EndPolicy
import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.NavigationState
import cn.anitabi.navigator.core.model.NavigationProgress
import cn.anitabi.navigator.core.model.PilgrimagePoint
import cn.anitabi.navigator.core.model.RouteObjective
import cn.anitabi.navigator.core.model.RouteStep
import cn.anitabi.navigator.core.model.TourLeg
import cn.anitabi.navigator.core.model.TourPlan
import cn.anitabi.navigator.core.model.TransitLegDetails
import cn.anitabi.navigator.core.model.TravelMode
import java.time.OffsetDateTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationEngineTest {
    @Test
    fun `route refresh resumes the first remaining leg after next-stop state`() {
        val plan = planWithTwoStops(dwellMinutes = 0)
        val recovered = NavigationProgress(
            tourId = plan.id,
            legIndex = 8,
            stepIndex = 3,
            state = NavigationState.NEXT_STOP,
            offRouteSinceEpochMillis = 1L,
        ).afterRouteRefresh(hasRemainingLegs = true)
        val engine = NavigationEngine(plan, recovered)

        assertEquals(-1, recovered.legIndex)
        assertEquals(NavigationState.NAVIGATING, engine.onTick(2L).progress.state)
        assertEquals(0, engine.progress.legIndex)
    }

    @Test
    fun `route refresh completes when no remaining leg exists`() {
        val recovered = NavigationProgress(
            tourId = "tour",
            state = NavigationState.NAVIGATING,
        ).afterRouteRefresh(hasRemainingLegs = false)

        assertEquals(NavigationState.COMPLETED, recovered.state)
        assertEquals(0, recovered.legIndex)
    }

    @Test
    fun `arrival dwell next stop and completion follow the documented state sequence`() {
        val plan = planWithTwoStops(dwellMinutes = 1)
        val engine = NavigationEngine(plan)

        assertEquals(NavigationState.NAVIGATING, engine.start().progress.state)
        assertEquals(
            NavigationState.ARRIVING,
            engine.onLocation(plan.legs[0].to, nowEpochMillis = 1_000L).progress.state,
        )
        assertEquals(NavigationState.DWELLING, engine.onTick(1_000L).progress.state)
        assertEquals(setOf("a"), engine.progress.completedPointIds)
        assertEquals(NavigationState.DWELLING, engine.onTick(60_999L).progress.state)
        assertEquals(NavigationState.NEXT_STOP, engine.onTick(61_000L).progress.state)
        assertEquals(NavigationState.NAVIGATING, engine.onTick(61_001L).progress.state)
        assertEquals(1, engine.progress.legIndex)
        engine.onLocation(plan.legs[1].to, 62_000L)
        engine.onTick(62_000L)
        engine.onTick(122_000L)
        assertEquals(NavigationState.COMPLETED, engine.onTick(122_001L).progress.state)
    }

    @Test
    fun `off route must persist 15 seconds and reroutes are at least 60 seconds apart`() {
        val plan = planWithTwoStops(dwellMinutes = 0)
        val engine = NavigationEngine(plan)
        val farFromRoute = GeoPoint(0.002, 0.0005)
        engine.start()

        assertFalse(engine.onLocation(farFromRoute, 0L).requestReroute)
        assertFalse(engine.onLocation(farFromRoute, 14_999L).requestReroute)
        assertTrue(engine.onLocation(farFromRoute, 15_000L).requestReroute)
        assertFalse(engine.onLocation(farFromRoute, 74_999L).requestReroute)
        assertTrue(engine.onLocation(farFromRoute, 75_000L).requestReroute)
    }

    @Test
    fun `return leg without pilgrimage destination skips dwell and still completes`() {
        val start = GeoPoint(0.0, 0.0)
        val stop = point("a", 0.001)
        val outbound = leg(start, stop.coordinate, "a")
        val returning = leg(stop.coordinate, start, destinationPointId = null)
        val engine = NavigationEngine(plan(listOf(stop), listOf(outbound, returning), dwellMinutes = 0))
        engine.start()
        engine.onLocation(stop.coordinate, 1L)
        engine.onTick(1L)
        engine.onTick(1L)
        engine.onTick(2L)
        engine.onLocation(start, 3L)
        assertEquals(NavigationState.ARRIVING, engine.progress.state)
        engine.onTick(3L)
        assertEquals(NavigationState.NEXT_STOP, engine.progress.state)
        assertEquals(NavigationState.COMPLETED, engine.onTick(4L).progress.state)
    }

    @Test
    fun `transit leg advances by scheduled time when GPS is unavailable`() {
        val arrival = OffsetDateTime.parse("2026-07-29T09:10:00+09:00")
        val stop = point("a", 0.001)
        val transitLeg = leg(GeoPoint(0.0, 0.0), stop.coordinate, "a").copy(
            mode = TravelMode.TRANSIT,
            transit = TransitLegDetails(
                vehicleMode = "TRAIN",
                arrivalTime = arrival.toString(),
            ),
        )
        val engine = NavigationEngine(plan(listOf(stop), listOf(transitLeg), dwellMinutes = 0))
        engine.start()

        assertEquals(
            NavigationState.ARRIVING,
            engine.onTick(arrival.toInstant().toEpochMilli()).progress.state,
        )
    }

    @Test
    fun `approaching the next maneuver advances spoken step`() {
        val start = GeoPoint(0.0, 0.0)
        val turn = GeoPoint(0.0005, 0.0)
        val stop = point("a", 0.001)
        val routeLeg = leg(start, stop.coordinate, "a").copy(
            steps = listOf(
                RouteStep("直行", 50.0, 30.0, start),
                RouteStep("右转", 50.0, 30.0, turn),
            ),
        )
        val engine = NavigationEngine(plan(listOf(stop), listOf(routeLeg), dwellMinutes = 0))
        engine.start()

        val update = engine.onLocation(turn, 1_000L)

        assertEquals(1, update.progress.stepIndex)
        assertEquals("右转", update.instruction)
    }

    private fun planWithTwoStops(dwellMinutes: Int): TourPlan {
        val start = GeoPoint(0.0, 0.0)
        val first = point("a", 0.001)
        val second = point("b", 0.002)
        return plan(
            points = listOf(first, second),
            legs = listOf(
                leg(start, first.coordinate, first.id),
                leg(first.coordinate, second.coordinate, second.id),
            ),
            dwellMinutes = dwellMinutes,
        )
    }

    private fun plan(
        points: List<PilgrimagePoint>,
        legs: List<TourLeg>,
        dwellMinutes: Int,
    ) = TourPlan(
        id = "tour",
        anime = Anime(1, "Test"),
        selectedPoints = points,
        orderedPoints = points,
        legs = legs,
        mode = legs.first().mode,
        objective = RouteObjective.FASTEST,
        endPolicy = EndPolicy.OPEN,
        estimatedDurationSeconds = legs.sumOf { it.durationSeconds },
        attribution = emptyList(),
        dwellMinutes = dwellMinutes,
    )

    private fun leg(from: GeoPoint, to: GeoPoint, destinationPointId: String?) = TourLeg(
        from = from,
        to = to,
        mode = TravelMode.WALK,
        geometry = listOf(from, to),
        steps = emptyList(),
        distanceMeters = 111.0,
        durationSeconds = 60.0,
        source = "test",
        destinationPointId = destinationPointId,
    )

    private fun point(id: String, latitude: Double) = PilgrimagePoint(
        id = id,
        name = id,
        coordinate = GeoPoint(latitude, 0.0),
    )
}
