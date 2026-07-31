package cn.anitabi.navigator.ui.planner

import cn.anitabi.navigator.core.model.Anime
import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.PilgrimagePoint
import cn.anitabi.navigator.core.model.RouteObjective
import cn.anitabi.navigator.core.model.TourLeg
import cn.anitabi.navigator.core.model.TransitTimeMode
import cn.anitabi.navigator.core.model.TravelMode
import cn.anitabi.navigator.core.routing.RoadRoute
import cn.anitabi.navigator.core.routing.RoadRoutingProvider
import cn.anitabi.navigator.core.routing.TourPlanner
import cn.anitabi.navigator.core.routing.TransitJourney
import cn.anitabi.navigator.core.routing.TransitJourneyProvider
import cn.anitabi.navigator.core.routing.TransitJourneyQuery
import cn.anitabi.navigator.core.routing.TravelMatrix
import cn.anitabi.navigator.data.local.TourPlanDao
import cn.anitabi.navigator.data.local.TourPlanEntity
import cn.anitabi.navigator.data.network.ApiHttpClient
import cn.anitabi.navigator.data.repository.TourRepository
import cn.anitabi.navigator.navigation.CurrentLocationProvider
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlannerViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `reconfigure cancels old transit planning without saving or overwriting new selection`() =
        runTest(dispatcher) {
            val transit = BlockingTransitProvider()
            val dao = RecordingTourPlanDao()
            val repository = TourRepository(dao, ApiHttpClient.defaultJson)
            val viewModel = PlannerViewModel(
                planner = TourPlanner(UnusedRoadProvider(), transit),
                repository = repository,
                locationProvider = object : CurrentLocationProvider {
                    override suspend fun currentLocation(): GeoPoint = GeoPoint(0.0, 0.0)
                },
                clock = Clock.fixed(Instant.parse("2026-07-31T04:36:47Z"), ZoneId.of("Asia/Shanghai")),
            )
            viewModel.configure(Anime(1, "Old"), points("old"))
            viewModel.setMode(TravelMode.TRANSIT)
            viewModel.setTransitSchedule(
                TransitTimeMode.NOW,
                viewModel.state.value.transitDate,
                viewModel.state.value.transitTime,
            )

            viewModel.generate()
            runCurrent()
            assertTrue(transit.started.isCompleted)

            viewModel.configure(Anime(2, "New"), points("new"))
            transit.release.complete(Unit)
            advanceUntilIdle()

            assertEquals("New", viewModel.state.value.anime?.name)
            assertFalse(viewModel.state.value.isLoading)
            assertNull(viewModel.state.value.plan)
            assertTrue(dao.entities.isEmpty())
            assertEquals(1, transit.calls)
        }

    private fun points(prefix: String): List<PilgrimagePoint> = (1..3).map { index ->
        PilgrimagePoint("$prefix-$index", "$prefix $index", GeoPoint(index.toDouble(), 0.0))
    }
}

private class BlockingTransitProvider : TransitJourneyProvider {
    val started = CompletableDeferred<Unit>()
    val release = CompletableDeferred<Unit>()
    var calls = 0

    override suspend fun journey(
        from: GeoPoint,
        to: GeoPoint,
        query: TransitJourneyQuery,
    ): TransitJourney {
        calls += 1
        started.complete(Unit)
        release.await()
        val departure = requireNotNull(query.departureTime)
        val arrival = java.time.OffsetDateTime.parse(departure).plusMinutes(10).toString()
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
                    source = "test",
                ),
            ),
            departureTime = departure,
            arrivalTime = arrival,
        )
    }
}

private class UnusedRoadProvider : RoadRoutingProvider {
    override suspend fun matrix(
        mode: TravelMode,
        points: List<GeoPoint>,
        objective: RouteObjective,
    ): TravelMatrix = error("Not used")

    override suspend fun directions(mode: TravelMode, points: List<GeoPoint>): RoadRoute = error("Not used")
}

private class RecordingTourPlanDao : TourPlanDao {
    val entities = linkedMapOf<String, TourPlanEntity>()

    override suspend fun get(id: String): TourPlanEntity? = entities[id]
    override suspend fun getMostRecent(): TourPlanEntity? = entities.values.lastOrNull()
    override suspend fun upsert(entity: TourPlanEntity) {
        entities[entity.id] = entity
    }
    override suspend fun finishLegacyMigration(id: String, storedTourJson: String, updatedAtEpochMillis: Long) = Unit
    override suspend fun recordMigrationError(id: String, message: String) = Unit
    override suspend fun getMostRecentMigrationError(): String? = null
}
