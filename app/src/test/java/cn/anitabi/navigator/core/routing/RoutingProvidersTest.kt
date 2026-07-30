package cn.anitabi.navigator.core.routing

import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.RouteObjective
import cn.anitabi.navigator.core.model.TravelMode
import cn.anitabi.navigator.data.auth.IdTokenProvider
import cn.anitabi.navigator.data.network.ApiHttpClient
import cn.anitabi.navigator.data.network.UserAgentInterceptor
import cn.anitabi.navigator.data.network.backend.BackendApi
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RoutingProvidersTest {
    private lateinit var server: MockWebServer
    private lateinit var api: BackendApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = BackendApi(
            httpClient = ApiHttpClient(UserAgentInterceptor("AnitabiNavigator", "test", "https://example.org")),
            tokenProvider = IdTokenProvider { "test-token" },
            baseUrl = server.url("/").toString(),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `road provider maps matrix unreachable elements and Google polyline`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"elements":[{"originIndex":0,"destinationIndex":0,"status":"OK","distanceMeters":0,"durationSeconds":0},{"originIndex":0,"destinationIndex":1,"status":"UNREACHABLE"},{"originIndex":1,"destinationIndex":0,"status":"OK","distanceMeters":1000,"durationSeconds":600},{"originIndex":1,"destinationIndex":1,"status":"OK","distanceMeters":0,"durationSeconds":0}]}""",
            ),
        )
        server.enqueue(
            MockResponse().setBody(
                """{"distanceMeters":1000,"durationSeconds":600,"legs":[{"distanceMeters":1000,"durationSeconds":600,"encodedPolyline":"_p~iF~ps|U_ulLnnqC_mqNvxq`@","steps":[{"travelMode":"WALK","distanceMeters":1000,"durationSeconds":600,"encodedPolyline":"_p~iF~ps|U_ulLnnqC_mqNvxq`@","instruction":"直行"}]}]}""",
            ),
        )
        val provider = BackendRoadRoutingProvider(api)
        val points = listOf(GeoPoint(38.5, -120.2), GeoPoint(43.252, -126.453))

        val matrix = provider.matrix(TravelMode.WALK, points, RouteObjective.FASTEST)
        val route = provider.directions(TravelMode.WALK, points)

        assertNull(matrix.durations[0][1])
        assertEquals(600.0, matrix.durations[1][0])
        assertEquals(3, route.segments.single().geometry.size)
        assertEquals("直行", route.segments.single().steps.single().instruction)
    }

    @Test
    fun `transit provider keeps Google line stop times and uses no invented platform`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"distanceMeters":5000,"durationSeconds":900,"legs":[{"distanceMeters":5000,"durationSeconds":900,"steps":[{"travelMode":"WALK","distanceMeters":200,"durationSeconds":180,"encodedPolyline":"_p~iF~ps|U_ulLnnqC"},{"travelMode":"TRANSIT","distanceMeters":4800,"durationSeconds":720,"encodedPolyline":"_ulLnnqC_mqNvxq`@","transit":{"departureStop":"Tokyo","arrivalStop":"Ueno","departureTime":"2026-07-29T09:03:00+09:00","arrivalTime":"2026-07-29T09:15:00+09:00","lineShortName":"JY","headsign":"Ueno","vehicleType":"HEAVY_RAIL","stopCount":3}}]}]}""",
            ),
        )
        val journey = BackendTransitJourneyProvider(api).journey(
            from = GeoPoint(38.5, -120.2),
            to = GeoPoint(43.252, -126.453),
            departureTime = "2026-07-29T09:00:00+09:00",
        )

        assertEquals(2, journey.legs.size)
        assertEquals("2026-07-29T09:15:00+09:00", journey.arrivalTime)
        val transit = journey.legs.last().transit!!
        assertEquals("JY", transit.line)
        assertEquals("Ueno", transit.direction)
        assertEquals("HEAVY_RAIL", transit.vehicleMode)
        assertEquals("Tokyo", transit.departureStop)
        assertEquals("Ueno", transit.arrivalStop)
        assertEquals(3, transit.stopCount)
        assertNull(transit.departurePlatform)
        assertTrue(transit.intermediateStops.isEmpty())
        assertTrue(journey.legs.all { it.source == GOOGLE_ROUTES_SOURCE })
    }
}
