package cn.anitabi.navigator.navigation

import cn.anitabi.navigator.core.model.Anime
import cn.anitabi.navigator.core.model.EndPolicy
import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.PilgrimagePoint
import cn.anitabi.navigator.core.model.RouteObjective
import cn.anitabi.navigator.core.model.TourPlan
import cn.anitabi.navigator.core.model.TransitExecutionStrategy
import cn.anitabi.navigator.core.model.TravelMode
import cn.anitabi.navigator.core.region.JapanRegion
import cn.anitabi.navigator.core.routing.RoadRoute
import cn.anitabi.navigator.core.routing.RoadRoutingProvider
import cn.anitabi.navigator.core.routing.TourPlanner
import cn.anitabi.navigator.core.routing.TransitJourney
import cn.anitabi.navigator.core.routing.TransitJourneyProvider
import cn.anitabi.navigator.core.routing.TransitJourneyQuery
import cn.anitabi.navigator.core.routing.TravelMatrix
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalTransitRecoveryPlanTest {
    @Test
    fun `cold recovery rebuilds local placeholder legs without a routing provider`() = runBlocking {
        val first = point("first", 35.6762, 139.6503)
        val second = point("second", 35.6895, 139.6917)
        val start = GeoPoint(35.67, 139.64)
        val unresolved = TourPlan(
            id = "recovered-japan",
            anime = Anime(1, "Japan"),
            selectedPoints = listOf(first, second),
            orderedPoints = listOf(first, second),
            legs = emptyList(),
            mode = TravelMode.TRANSIT,
            objective = RouteObjective.FASTEST,
            endPolicy = EndPolicy.OPEN,
            estimatedDurationSeconds = 0.0,
            attribution = listOf("Google Maps"),
            initialStart = start,
            executionStrategy = TransitExecutionStrategy.EXTERNAL_GOOGLE_MAPS_JAPAN,
        )

        val restored = restoreExternalJapanControlPlan(
            plan = unresolved,
            routeNeedsRefresh = true,
            planner = noNetworkPlanner(),
        )

        assertEquals(2, restored.legs.size)
        assertEquals(listOf("first", "second"), restored.legs.mapNotNull { it.destinationPointId })
        assertEquals(start, restored.legs.first().from)
        assertTrue(restored.legs.all { it.geometry.isEmpty() && it.steps.isEmpty() })
    }

    @Test
    fun `warm recovery preserves an already resolved external plan`() = runBlocking {
        val first = point("first", 35.6762, 139.6503)
        val unresolved = TourPlan(
            id = "warm-japan",
            anime = Anime(1, "Japan"),
            selectedPoints = listOf(first),
            orderedPoints = listOf(first),
            legs = emptyList(),
            mode = TravelMode.TRANSIT,
            objective = RouteObjective.FASTEST,
            endPolicy = EndPolicy.OPEN,
            estimatedDurationSeconds = 0.0,
            attribution = listOf("Google Maps"),
            initialStart = first.coordinate,
            executionStrategy = TransitExecutionStrategy.EXTERNAL_GOOGLE_MAPS_JAPAN,
        )

        val resolved = restoreExternalJapanControlPlan(unresolved, routeNeedsRefresh = true, noNetworkPlanner())
        val warm = restoreExternalJapanControlPlan(resolved, routeNeedsRefresh = false, noNetworkPlanner())

        assertSame(resolved, warm)
        assertEquals(1, warm.legs.size)
        assertEquals(first.id, warm.legs.single().destinationPointId)
        assertTrue(warm.legs.single().geometry.isEmpty())
    }

    private fun noNetworkPlanner() = TourPlanner(
        roadProvider = object : RoadRoutingProvider {
            override suspend fun matrix(
                mode: TravelMode,
                points: List<GeoPoint>,
                objective: RouteObjective,
            ): TravelMatrix = error("Japan recovery must not call matrix")

            override suspend fun directions(mode: TravelMode, points: List<GeoPoint>): RoadRoute =
                error("Japan recovery must not call directions")
        },
        transitProvider = object : TransitJourneyProvider {
            override suspend fun journey(
                from: GeoPoint,
                to: GeoPoint,
                query: TransitJourneyQuery,
            ): TransitJourney = error("Japan recovery must not call transit")
        },
        classifyRegion = { JapanRegion.JAPAN },
    )

    private fun point(id: String, latitude: Double, longitude: Double) = PilgrimagePoint(
        id = id,
        name = id,
        coordinate = GeoPoint(latitude, longitude),
    )
}
