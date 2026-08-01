package cn.anitabi.navigator.core.routing

import cn.anitabi.navigator.core.model.Anime
import cn.anitabi.navigator.core.model.EndPolicy
import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.PilgrimagePoint
import cn.anitabi.navigator.core.model.RouteObjective
import cn.anitabi.navigator.core.model.StoredTourV2
import cn.anitabi.navigator.core.model.TourLeg
import cn.anitabi.navigator.core.model.TransitTimeMode
import cn.anitabi.navigator.core.model.TransitTravelMode
import cn.anitabi.navigator.core.model.TravelMode
import cn.anitabi.navigator.data.network.ApiException
import java.time.OffsetDateTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
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
        assertEquals(listOf(GOOGLE_ROUTES_SOURCE, "Google"), plan.attribution)
    }

    @Test
    fun `transit planner chains dwell time into the next request`() = runBlocking {
        val transit = FakeTransitProvider()
        val planner = TourPlanner(FakeRoadProvider(), transit)
        val departure = "2026-07-29T09:00+09:00"
        val progress = mutableListOf<Pair<Int, Int>>()

        val plan = planner.planTransit(
            TransitPlanRequest(
                anime = anime,
                selectedPoints = listOf(point("near", 0.1), point("far", 0.2)),
                start = GeoPoint(0.0, 0.0),
                endPolicy = EndPolicy.OPEN,
                timeMode = TransitTimeMode.DEPART_AT,
                anchorTime = departure,
                dwellMinutes = 15,
            ),
        ) { completed, total -> progress += completed to total }

        assertEquals("2026-07-29T09:00:00+09:00", transit.queries[0].departureTime)
        assertEquals("2026-07-29T09:25:00+09:00", transit.queries[1].departureTime)
        assertEquals(2, plan.legs.size)
        assertEquals(2, transit.queries.size)
        assertEquals("2026-07-29T09:35:00+09:00", plan.arrivalTime)
        assertEquals(TravelMode.TRANSIT, plan.mode)
        assertEquals(listOf(1 to 2, 2 to 2), progress)
    }

    @Test
    fun `transit planner chains arrive by requests backwards`() = runBlocking {
        val transit = FakeTransitProvider()
        val planner = TourPlanner(FakeRoadProvider(), transit)

        val plan = planner.planTransit(
            TransitPlanRequest(
                anime = anime,
                selectedPoints = listOf(point("near", 0.1), point("far", 0.2)),
                start = GeoPoint(0.0, 0.0),
                endPolicy = EndPolicy.OPEN,
                timeMode = TransitTimeMode.ARRIVE_BY,
                anchorTime = "2026-07-29T10:00:00+09:00",
                dwellMinutes = 15,
            ),
        )

        assertEquals("2026-07-29T10:00:00+09:00", transit.queries[0].arrivalTime)
        assertEquals("2026-07-29T09:35:00+09:00", transit.queries[1].arrivalTime)
        assertEquals("2026-07-29T09:25:00+09:00", plan.departureTime)
        assertEquals("2026-07-29T10:00:00+09:00", plan.arrivalTime)
        assertEquals("2026-07-29T10:00:00+09:00", plan.transitAnchorTime)
        assertEquals(TransitTimeMode.ARRIVE_BY, plan.transitTimeMode)
    }

    @Test
    fun `transit travel modes forward through planning rebuild and replan`() = runBlocking {
        val transit = FakeTransitProvider()
        val planner = TourPlanner(FakeRoadProvider(), transit)
        val travelModes = setOf(TransitTravelMode.BUS, TransitTravelMode.TRAIN)
        val plan = planner.planTransit(
            TransitPlanRequest(
                anime = anime,
                selectedPoints = listOf(point("near", 0.1), point("far", 0.2)),
                start = GeoPoint(0.0, 0.0),
                endPolicy = EndPolicy.OPEN,
                timeMode = TransitTimeMode.DEPART_AT,
                anchorTime = "2026-07-29T09:00:00+09:00",
                transitTravelModes = travelModes,
            ),
        )

        assertEquals(travelModes, plan.transitTravelModes)
        assertEquals(2, transit.queries.size)
        assertTrue(transit.queries.all { it.transitTravelModes == travelModes })

        transit.queries.clear()
        val rebuilt = planner.rebuild(plan, plan.orderedPoints)
        assertEquals(travelModes, rebuilt.transitTravelModes)
        assertEquals(2, transit.queries.size)
        assertTrue(transit.queries.all { it.transitTravelModes == travelModes })

        transit.queries.clear()
        val replanned = planner.replanRemaining(
            plan = rebuilt,
            currentLocation = GeoPoint(0.05, 0.0),
            completedPointIds = emptySet(),
            currentTime = "2026-07-29T09:30:00+09:00",
        )
        assertEquals(travelModes, replanned.transitTravelModes)
        assertEquals(2, transit.queries.size)
        assertTrue(transit.queries.all { it.transitTravelModes == travelModes })
    }

    @Test
    fun `arrive by rebuild keeps the user deadline when Google arrives early`() = runBlocking {
        val transit = EarlyArrivalTransitProvider()
        val planner = TourPlanner(FakeRoadProvider(), transit)
        val deadline = "2026-07-29T10:00:00+09:00"
        val plan = planner.planTransit(
            TransitPlanRequest(
                anime = anime,
                selectedPoints = listOf(point("near", 0.1), point("far", 0.2)),
                start = GeoPoint(0.0, 0.0),
                endPolicy = EndPolicy.OPEN,
                timeMode = TransitTimeMode.ARRIVE_BY,
                anchorTime = deadline,
                dwellMinutes = 15,
            ),
        )

        assertEquals("2026-07-29T09:50:00+09:00", plan.arrivalTime)
        assertEquals(deadline, plan.transitAnchorTime)

        planner.rebuild(plan, plan.orderedPoints)

        assertEquals(deadline, transit.queries[2].arrivalTime)
    }

    @Test
    fun `restored current location tour keeps the first segment when reordered`() = runBlocking {
        val start = GeoPoint(0.0, 0.0)
        val planner = TourPlanner(FakeRoadProvider(), FakeTransitProvider())
        val plan = planner.planTransit(
            TransitPlanRequest(
                anime = anime,
                selectedPoints = listOf(point("near", 0.1), point("far", 0.2)),
                start = start,
                endPolicy = EndPolicy.OPEN,
                timeMode = TransitTimeMode.DEPART_AT,
                anchorTime = "2026-07-29T09:00:00+09:00",
            ),
        )
        val restored = StoredTourV2.from(plan, null).toUnresolvedPlan()

        val reordered = restored.orderedPoints.reversed()
        val rebuilt = planner.rebuild(restored, reordered)

        assertEquals(start, rebuilt.legs.first().from)
        assertEquals(2, rebuilt.legs.size)
        assertEquals(reordered.map { it.id }, rebuilt.legs.mapNotNull(TourLeg::destinationPointId))
    }

    @Test
    fun `missing transit segment becomes a walking connector and planning continues`() = runBlocking {
        val road = FakeRoadProvider()
        val transit = FailingTransitProvider(failingCall = 1)
        val planner = TourPlanner(road, transit)

        val plan = planner.planTransit(
            TransitPlanRequest(
                anime = anime,
                selectedPoints = listOf(point("near", 0.1), point("far", 0.2)),
                start = GeoPoint(0.0, 0.0),
                endPolicy = EndPolicy.OPEN,
                timeMode = TransitTimeMode.NOW,
                anchorTime = "2026-07-29T09:00:00+09:00",
            ),
        )

        assertEquals(listOf(TravelMode.WALK), road.directionModes)
        assertEquals(listOf(TravelMode.WALK, TravelMode.TRANSIT), plan.legs.map(TourLeg::mode))
        assertEquals(listOf("near", "far"), plan.legs.map(TourLeg::destinationPointId))
        assertEquals(2, transit.queries.size)
    }

    @Test
    fun `walking-only fallback is not reported as a transit itinerary`() {
        val road = FakeRoadProvider()
        val planner = TourPlanner(road, AlwaysNoRouteTransitProvider())

        val exception = assertThrows(TransitRideUnavailableException::class.java) {
            runBlocking {
                planner.planTransit(
                    TransitPlanRequest(
                        anime = anime,
                        selectedPoints = listOf(point("near", 0.1), point("far", 0.2)),
                        start = GeoPoint(0.0, 0.0),
                        endPolicy = EndPolicy.OPEN,
                        timeMode = TransitTimeMode.NOW,
                        anchorTime = "2026-07-29T09:00:00+09:00",
                    ),
                )
            }
        }

        assertTrue(exception.message.orEmpty().contains("no transit ride"))
        assertEquals(listOf(TravelMode.WALK, TravelMode.WALK), road.directionModes)
    }

    @Test
    fun `zero-valued walking fallback between different points is not transit evidence`() {
        val planner = TourPlanner(ZeroValuedRoadProvider(), AlwaysNoRouteTransitProvider())

        assertThrows(TransitRideUnavailableException::class.java) {
            runBlocking {
                planner.planTransit(
                    TransitPlanRequest(
                        anime = anime,
                        selectedPoints = listOf(point("near", 0.1), point("far", 0.2)),
                        start = GeoPoint(0.0, 0.0),
                        endPolicy = EndPolicy.OPEN,
                        timeMode = TransitTimeMode.NOW,
                        anchorTime = "2026-07-29T09:00:00+09:00",
                    ),
                )
            }
        }
    }

    @Test
    fun `missing transit and walking route reports the exact segment`() {
        val planner = TourPlanner(NoRouteRoadProvider(), FailingTransitProvider(failingCall = 1))
        val start = GeoPoint(0.0, 0.0)
        val near = point("near", 0.1)

        val exception = assertThrows(TransitSegmentUnavailableException::class.java) {
            runBlocking {
                planner.planTransit(
                    TransitPlanRequest(
                        anime = anime,
                        selectedPoints = listOf(near, point("far", 0.2)),
                        start = start,
                        endPolicy = EndPolicy.OPEN,
                        timeMode = TransitTimeMode.NOW,
                        anchorTime = "2026-07-29T09:00:00+09:00",
                    ),
                )
            }
        }

        assertEquals(1, exception.segmentNumber)
        assertEquals(2, exception.segmentCount)
        assertEquals(start, exception.from)
        assertEquals(near.coordinate, exception.to)
    }

    @Test
    fun `transit service failure is not disguised as a walking-only route`() {
        val road = FakeRoadProvider()
        val planner = TourPlanner(road, UnavailableTransitProvider())

        val exception = assertThrows(ApiException.UpstreamUnavailable::class.java) {
            runBlocking {
                planner.planTransit(
                    TransitPlanRequest(
                        anime = anime,
                        selectedPoints = listOf(point("near", 0.1), point("far", 0.2)),
                        start = GeoPoint(0.0, 0.0),
                        endPolicy = EndPolicy.OPEN,
                        timeMode = TransitTimeMode.NOW,
                        anchorTime = "2026-07-29T09:00:00+09:00",
                    ),
                )
            }
        }

        assertTrue(exception is ApiException.UpstreamUnavailable)
        assertTrue(road.directionModes.isEmpty())
    }

    @Test
    fun `identical adjacent coordinates become local zero-distance connectors`() = runBlocking {
        val road = FakeRoadProvider()
        val transit = FakeTransitProvider()
        val planner = TourPlanner(road, transit)
        val coordinate = GeoPoint(0.0, 0.0)

        val plan = planner.planTransit(
            TransitPlanRequest(
                anime = anime,
                selectedPoints = listOf(
                    PilgrimagePoint("a", "a", coordinate),
                    PilgrimagePoint("b", "b", coordinate),
                ),
                start = coordinate,
                endPolicy = EndPolicy.OPEN,
                timeMode = TransitTimeMode.NOW,
                anchorTime = "2026-07-29T09:00:00+09:00",
            ),
        )

        assertTrue(transit.queries.isEmpty())
        assertTrue(road.directionModes.isEmpty())
        assertEquals(listOf("a", "b"), plan.legs.mapNotNull(TourLeg::destinationPointId))
        assertTrue(plan.legs.all { it.mode == TravelMode.WALK && it.distanceMeters == 0.0 })
    }

    @Test
    fun `transit planner accepts more than eight points and requests adjacent pairs`() = runBlocking {
        val transit = FakeTransitProvider()
        val planner = TourPlanner(FakeRoadProvider(), transit)

        val plan = planner.planTransit(
            TransitPlanRequest(
                anime = anime,
                selectedPoints = (1..14).map { point("point-$it", it.toDouble()) },
                start = GeoPoint(0.0, 0.0),
                endPolicy = EndPolicy.OPEN,
                timeMode = TransitTimeMode.DEPART_AT,
                anchorTime = "2026-07-29T09:00:00+09:00",
            ),
        )

        assertEquals(14, plan.orderedPoints.size)
        assertEquals(14, transit.queries.size)
    }

    @Test
    fun `road planner splits unlimited trip into bounded matrix and route requests`() = runBlocking {
        val road = FakeRoadProvider()
        val planner = TourPlanner(road, FakeTransitProvider())

        val plan = planner.planRoad(
            RoadPlanRequest(
                anime = anime,
                selectedPoints = (1..35).map { point("point-$it", it / 100.0) },
                start = GeoPoint(0.0, 0.0),
                mode = TravelMode.WALK,
                objective = RouteObjective.FASTEST,
                endPolicy = EndPolicy.OPEN,
            ),
        )

        assertEquals(35, plan.orderedPoints.size)
        assertEquals(listOf(10, 10, 10, 9), road.matrixRequestSizes)
        assertEquals(listOf(12, 12, 12, 3), road.directionRequestSizes)
        assertTrue(road.matrixRequestSizes.all { it * it <= 100 })
        assertEquals(35, plan.legs.size)
    }

    @Test
    fun `transit dwell crosses midnight without losing the original offset`() = runBlocking {
        val transit = FakeTransitProvider()
        val planner = TourPlanner(FakeRoadProvider(), transit)

        planner.planTransit(
            TransitPlanRequest(
                anime = anime,
                selectedPoints = listOf(point("near", 0.1), point("far", 0.2)),
                start = GeoPoint(0.0, 0.0),
                endPolicy = EndPolicy.OPEN,
                timeMode = TransitTimeMode.DEPART_AT,
                anchorTime = "2026-07-29T23:40:00+08:00",
                dwellMinutes = 15,
            ),
        )

        assertEquals(
            OffsetDateTime.parse("2026-07-30T00:05:00+08:00"),
            OffsetDateTime.parse(transit.queries[1].departureTime),
        )
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

    @Test
    fun `manual road reorder refreshes only changed adjacent legs`() = runBlocking {
        val road = FakeRoadProvider()
        val planner = TourPlanner(road, FakeTransitProvider())
        val plan = planner.planRoad(
            RoadPlanRequest(
                anime = anime,
                selectedPoints = (1..25).map { point("point-$it", it / 100.0) },
                start = GeoPoint(0.0, 0.0),
                mode = TravelMode.WALK,
                objective = RouteObjective.FASTEST,
                endPolicy = EndPolicy.OPEN,
            ),
        )
        val originalFirstLeg = plan.legs.first()
        val reordered = plan.orderedPoints.toMutableList().apply {
            val moved = removeAt(4)
            add(5, moved)
        }
        road.directionRequestSizes.clear()

        val rebuilt = planner.rebuild(plan, reordered)

        assertEquals(listOf(4), road.directionRequestSizes)
        assertEquals(originalFirstLeg, rebuilt.legs.first())
        assertEquals(reordered.map { it.id }, rebuilt.orderedPoints.map { it.id })
    }

    private fun point(id: String, latitude: Double) = PilgrimagePoint(
        id = id,
        name = id,
        coordinate = GeoPoint(latitude, 0.0),
    )
}

private class FakeRoadProvider : RoadRoutingProvider {
    val matrixRequestSizes = mutableListOf<Int>()
    val directionRequestSizes = mutableListOf<Int>()
    val directionModes = mutableListOf<TravelMode>()

    override suspend fun matrix(
        mode: TravelMode,
        points: List<GeoPoint>,
        objective: RouteObjective,
    ): TravelMatrix {
        matrixRequestSizes += points.size
        val matrix = List(points.size) { from ->
            List<Double?>(points.size) { to -> kotlin.math.abs(from - to).toDouble() }
        }
        return TravelMatrix(durations = matrix, distances = matrix)
    }

    override suspend fun directions(mode: TravelMode, points: List<GeoPoint>): RoadRoute = RoadRoute(
        points.also {
            directionRequestSizes += it.size
            directionModes += mode
        }.zipWithNext().map { (from, to) ->
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
    val queries = mutableListOf<TransitJourneyQuery>()

    override suspend fun journey(
        from: GeoPoint,
        to: GeoPoint,
        query: TransitJourneyQuery,
    ): TransitJourney {
        queries += query
        val departure = query.departureTime ?: OffsetDateTime.parse(query.arrivalTime).minusMinutes(10).toString()
        val arrival = query.arrivalTime ?: OffsetDateTime.parse(query.departureTime).plusMinutes(10).toString()
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
            departureTime = departure,
            arrivalTime = arrival,
        )
    }
}

private class FailingTransitProvider(private val failingCall: Int) : TransitJourneyProvider {
    val queries = mutableListOf<TransitJourneyQuery>()

    override suspend fun journey(
        from: GeoPoint,
        to: GeoPoint,
        query: TransitJourneyQuery,
    ): TransitJourney {
        queries += query
        if (queries.size == failingCall) throw ApiException.NoRoute()
        val departure = query.departureTime ?: OffsetDateTime.parse(query.arrivalTime).minusMinutes(10).toString()
        val arrival = query.arrivalTime ?: OffsetDateTime.parse(query.departureTime).plusMinutes(10).toString()
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
            departureTime = departure,
            arrivalTime = arrival,
        )
    }
}

private class EarlyArrivalTransitProvider : TransitJourneyProvider {
    val queries = mutableListOf<TransitJourneyQuery>()

    override suspend fun journey(
        from: GeoPoint,
        to: GeoPoint,
        query: TransitJourneyQuery,
    ): TransitJourney {
        queries += query
        val requestedArrival = OffsetDateTime.parse(requireNotNull(query.arrivalTime))
        val arrival = requestedArrival.minusMinutes(10)
        val departure = arrival.minusMinutes(10)
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
            departureTime = departure.toString(),
            arrivalTime = arrival.toString(),
        )
    }
}

private class NoRouteRoadProvider : RoadRoutingProvider {
    override suspend fun matrix(
        mode: TravelMode,
        points: List<GeoPoint>,
        objective: RouteObjective,
    ): TravelMatrix = error("Matrix is not used for transit fallback")

    override suspend fun directions(mode: TravelMode, points: List<GeoPoint>): RoadRoute {
        throw ApiException.NoRoute()
    }
}

private class ZeroValuedRoadProvider : RoadRoutingProvider {
    override suspend fun matrix(
        mode: TravelMode,
        points: List<GeoPoint>,
        objective: RouteObjective,
    ): TravelMatrix = error("Matrix is not used for transit fallback")

    override suspend fun directions(mode: TravelMode, points: List<GeoPoint>): RoadRoute = RoadRoute(
        points.zipWithNext().map { (from, to) ->
            RoadRouteSegment(
                geometry = listOf(from, to),
                steps = emptyList(),
                distanceMeters = 0.0,
                durationSeconds = 0.0,
            )
        },
    )
}

private class UnavailableTransitProvider : TransitJourneyProvider {
    override suspend fun journey(
        from: GeoPoint,
        to: GeoPoint,
        query: TransitJourneyQuery,
    ): TransitJourney {
        throw ApiException.UpstreamUnavailable()
    }
}

private class AlwaysNoRouteTransitProvider : TransitJourneyProvider {
    override suspend fun journey(
        from: GeoPoint,
        to: GeoPoint,
        query: TransitJourneyQuery,
    ): TransitJourney {
        throw ApiException.NoRoute()
    }
}
