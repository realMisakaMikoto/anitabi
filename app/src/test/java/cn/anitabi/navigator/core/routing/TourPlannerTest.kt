package cn.anitabi.navigator.core.routing

import cn.anitabi.navigator.core.model.Anime
import cn.anitabi.navigator.core.model.EndPolicy
import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.PilgrimagePoint
import cn.anitabi.navigator.core.model.RouteObjective
import cn.anitabi.navigator.core.model.TourLeg
import cn.anitabi.navigator.core.model.TravelMode
import java.time.OffsetDateTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class TourPlannerTest {
    private val anime = Anime(1, "Test")

    @Test
    fun `road planner uses objective matrix and adds return leg`() = runBlocking {
        val provider = FakeRoadProvider()
        val planner = TourPlanner(provider, FakeTransitProvider())
        val points = listOf(point("a", 1.0), point("b", 2.0))

        val plan = planner.planRoad(
            RoadPlanRequest(
                anime = anime,
                selectedPoints = points,
                start = GeoPoint(0.0, 0.0),
                mode = TravelMode.WALK,
                objective = RouteObjective.FASTEST,
                endPolicy = EndPolicy.RETURN_TO_START,
            ),
        )

        assertEquals(setOf("a", "b"), plan.orderedPoints.map { it.id }.toSet())
        assertEquals(3, plan.legs.size)
        assertEquals(GeoPoint(0.0, 0.0), plan.legs.last().to)
        assertEquals(listOf("openrouteservice / HeiGIT", "© OpenStreetMap contributors"), plan.attribution)
    }

    @Test
    fun `transit planner chains dwell time into the next request`() = runBlocking {
        val transit = FakeTransitProvider()
        val planner = TourPlanner(FakeRoadProvider(), transit)
        val departure = "2026-07-29T09:00:00+09:00"

        val plan = planner.planTransit(
            TransitPlanRequest(
                anime = anime,
                selectedPoints = listOf(point("near", 0.1), point("far", 0.2)),
                start = GeoPoint(0.0, 0.0),
                endPolicy = EndPolicy.OPEN,
                departureTime = departure,
                dwellMinutes = 15,
            ),
        )

        assertEquals(departure, transit.departures[0])
        assertEquals(
            OffsetDateTime.parse("2026-07-29T09:25:00+09:00"),
            OffsetDateTime.parse(transit.departures[1]),
        )
        assertEquals(2, plan.legs.size)
        assertEquals(TravelMode.TRANSIT, plan.mode)
    }

    @Test
    fun `reroute keeps unfinished stops and returns to the original start`() = runBlocking {
        val planner = TourPlanner(FakeRoadProvider(), FakeTransitProvider())
        val start = GeoPoint(0.0, 0.0)
        val plan = planner.planRoad(
            RoadPlanRequest(
                anime = anime,
                selectedPoints = listOf(point("a", 1.0), point("b", 2.0), point("c", 3.0)),
                start = start,
                mode = TravelMode.WALK,
                objective = RouteObjective.FASTEST,
                endPolicy = EndPolicy.RETURN_TO_START,
            ),
        )

        val rerouted = planner.replanRemaining(
            plan = plan,
            currentLocation = GeoPoint(1.2, 0.0),
            completedPointIds = setOf("a"),
            currentTime = "2026-07-29T10:00:00+09:00",
        )

        assertEquals(setOf("b", "c"), rerouted.orderedPoints.map { it.id }.toSet())
        assertEquals(GeoPoint(1.2, 0.0), rerouted.legs.first().from)
        assertEquals(start, rerouted.legs.last().to)
        assertEquals(start, rerouted.initialStart)
    }

    private fun point(id: String, latitude: Double) = PilgrimagePoint(
        id = id,
        name = id,
        coordinate = GeoPoint(latitude, 0.0),
    )
}

private class FakeRoadProvider : RoadRoutingProvider {
    override suspend fun matrix(mode: TravelMode, points: List<GeoPoint>): TravelMatrix {
        val matrix = List(points.size) { from ->
            List<Double?>(points.size) { to -> kotlin.math.abs(from - to).toDouble() }
        }
        return TravelMatrix(durations = matrix, distances = matrix)
    }

    override suspend fun directions(mode: TravelMode, points: List<GeoPoint>): RoadRoute = RoadRoute(
        points.zipWithNext().map { (from, to) ->
            RoadRouteSegment(
                geometry = listOf(from, to),
                steps = emptyList(),
                distanceMeters = 100.0,
                durationSeconds = 60.0,
            )
        },
    )
}

private class FakeTransitProvider : TransitJourneyProvider {
    val departures = mutableListOf<String>()

    override suspend fun journey(from: GeoPoint, to: GeoPoint, departureTime: String): TransitJourney {
        departures += departureTime
        val arrival = OffsetDateTime.parse(departureTime).plusMinutes(10).toString()
        return TransitJourney(
            legs = listOf(
                TourLeg(
                    from = from,
                    to = to,
                    mode = TravelMode.TRANSIT,
                    geometry = listOf(from, to),
                    steps = emptyList(),
                    distanceMeters = 100.0,
                    durationSeconds = 600.0,
                    source = "fake",
                ),
            ),
            arrivalTime = arrival,
        )
    }
}
