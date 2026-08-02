package cn.anitabi.navigator.ui.planner

import cn.anitabi.navigator.core.model.Anime
import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.PilgrimagePoint
import cn.anitabi.navigator.core.model.RouteObjective
import cn.anitabi.navigator.core.model.TravelMode
import cn.anitabi.navigator.core.region.JapanRegion
import cn.anitabi.navigator.core.region.JapanRegionDataException
import cn.anitabi.navigator.core.routing.MIXED_TRANSIT_REGION_MESSAGE
import cn.anitabi.navigator.core.routing.REGION_DATA_ERROR_MESSAGE
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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
class JapanTransitPlannerViewModelTest {
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
    fun `mixed transit is blocked before location and routing providers`() = runTest(dispatcher) {
        val fixture = viewModelFixture { coordinate ->
            if (coordinate.longitude >= 130.0) JapanRegion.JAPAN else JapanRegion.NON_JAPAN
        }
        fixture.viewModel.configure(
            Anime(1, "Mixed"),
            listOf(
                point("tokyo", 35.6762, 139.6503),
                point("seoul", 37.5665, 126.9780),
            ),
        )
        fixture.viewModel.setMode(TravelMode.TRANSIT)
        fixture.viewModel.setUseCurrentLocation()

        fixture.viewModel.generate()
        advanceUntilIdle()

        assertEquals(MIXED_TRANSIT_REGION_MESSAGE, fixture.viewModel.state.value.errorMessage)
        assertFalse(fixture.viewModel.state.value.isLoading)
        assertNull(fixture.viewModel.state.value.plan)
        assertEquals(0, fixture.location.calls)
        assertEquals(0, fixture.road.calls)
        assertEquals(0, fixture.transit.calls)
        assertTrue(fixture.dao.entities.isEmpty())
    }

    @Test
    fun `corrupt region data maps to the region error before location or providers`() = runTest(dispatcher) {
        val fixture = viewModelFixture {
            throw JapanRegionDataException("corrupt test asset")
        }
        fixture.viewModel.configure(
            Anime(1, "Corrupt data"),
            listOf(
                point("first", 35.6762, 139.6503),
                point("second", 35.6895, 139.6917),
            ),
        )
        fixture.viewModel.setMode(TravelMode.TRANSIT)
        fixture.viewModel.setUseCurrentLocation()

        fixture.viewModel.generate()
        advanceUntilIdle()

        assertEquals(REGION_DATA_ERROR_MESSAGE, fixture.viewModel.state.value.errorMessage)
        assertEquals(REGION_DATA_ERROR_MESSAGE, fixture.viewModel.state.value.transitRegionError)
        assertFalse(fixture.viewModel.state.value.isLoading)
        assertNull(fixture.viewModel.state.value.plan)
        assertEquals(0, fixture.location.calls)
        assertEquals(0, fixture.road.calls)
        assertEquals(0, fixture.transit.calls)
        assertTrue(fixture.dao.entities.isEmpty())
    }

    @Test
    fun `road planner configuration never loads transit region data`() {
        var classificationCalls = 0
        val fixture = viewModelFixture {
            classificationCalls += 1
            throw JapanRegionDataException("Road mode must not classify transit regions")
        }

        fixture.viewModel.configure(
            Anime(1, "Road"),
            listOf(
                point("first", 35.6762, 139.6503),
                point("second", 37.5665, 126.9780),
            ),
        )
        fixture.viewModel.setMode(TravelMode.WALK)

        assertEquals(0, classificationCalls)
        assertNull(fixture.viewModel.state.value.transitRegionError)
        assertNull(fixture.viewModel.state.value.errorMessage)
    }

    private fun viewModelFixture(classifyRegion: (GeoPoint) -> JapanRegion): ViewModelFixture {
        val road = NeverCalledRoadProvider()
        val transit = NeverCalledTransitProvider()
        val location = CountingLocationProvider()
        val dao = RegionGateTourPlanDao()
        return ViewModelFixture(
            viewModel = PlannerViewModel(
                planner = TourPlanner(road, transit, classifyRegion = classifyRegion),
                repository = TourRepository(dao, ApiHttpClient.defaultJson),
                locationProvider = location,
                clock = Clock.fixed(Instant.parse("2026-08-01T00:00:00Z"), ZoneId.of("Asia/Tokyo")),
            ),
            road = road,
            transit = transit,
            location = location,
            dao = dao,
        )
    }

    private fun point(id: String, latitude: Double, longitude: Double) = PilgrimagePoint(
        id = id,
        name = id,
        coordinate = GeoPoint(latitude, longitude),
    )
}

private data class ViewModelFixture(
    val viewModel: PlannerViewModel,
    val road: NeverCalledRoadProvider,
    val transit: NeverCalledTransitProvider,
    val location: CountingLocationProvider,
    val dao: RegionGateTourPlanDao,
)

private class CountingLocationProvider : CurrentLocationProvider {
    var calls = 0

    override suspend fun currentLocation(): GeoPoint {
        calls += 1
        return GeoPoint(35.0, 139.0)
    }
}

private class NeverCalledRoadProvider : RoadRoutingProvider {
    var calls = 0

    override suspend fun matrix(
        mode: TravelMode,
        points: List<GeoPoint>,
        objective: RouteObjective,
    ): TravelMatrix {
        calls += 1
        error("Road provider must not be called")
    }

    override suspend fun directions(mode: TravelMode, points: List<GeoPoint>): RoadRoute {
        calls += 1
        error("Road provider must not be called")
    }
}

private class NeverCalledTransitProvider : TransitJourneyProvider {
    var calls = 0

    override suspend fun journey(
        from: GeoPoint,
        to: GeoPoint,
        query: TransitJourneyQuery,
    ): TransitJourney {
        calls += 1
        error("Transit provider must not be called")
    }
}

private class RegionGateTourPlanDao : TourPlanDao {
    val entities = linkedMapOf<String, TourPlanEntity>()

    override suspend fun get(id: String): TourPlanEntity? = entities[id]

    override suspend fun getMostRecent(): TourPlanEntity? = entities.values.lastOrNull()

    override suspend fun getIdsMostRecentFirst(): List<String> = entities.values
        .sortedWith(compareByDescending<TourPlanEntity> { it.updatedAtEpochMillis }.thenByDescending { it.id })
        .map(TourPlanEntity::id)

    override suspend fun upsert(entity: TourPlanEntity) {
        entities[entity.id] = entity
    }

    override suspend fun finishLegacyMigration(
        id: String,
        storedTourJson: String,
        updatedAtEpochMillis: Long,
    ) = Unit

    override suspend fun recordMigrationError(id: String, message: String) = Unit

    override suspend fun getMostRecentMigrationError(): String? = null
}
