package cn.anitabi.navigator.data.network.backend

import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.RouteObjective
import cn.anitabi.navigator.core.model.TravelMode
import cn.anitabi.navigator.data.auth.IdTokenProvider
import cn.anitabi.navigator.data.network.ApiException
import cn.anitabi.navigator.data.network.ApiHttpClient
import cn.anitabi.navigator.data.network.UserAgentInterceptor
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackendApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: BackendApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = BackendApi(
            httpClient = ApiHttpClient(UserAgentInterceptor("AnitabiNavigator", "0.2.1", "https://example.org")),
            tokenProvider = IdTokenProvider { "firebase-test-token" },
            baseUrl = server.url("/").toString(),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `matrix sends bearer JSON and normalized bounded contract`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"elements":[{"originIndex":0,"destinationIndex":1,"status":"OK","distanceMeters":1000,"durationSeconds":600}]}""",
            ),
        )

        val response = api.matrix(
            mode = TravelMode.BIKE,
            coordinates = listOf(GeoPoint(35.0, 139.0), GeoPoint(35.1, 139.1)),
            objective = RouteObjective.SHORTEST,
        )

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/v1/matrix", request.requestUrl?.encodedPath)
        assertEquals("Bearer firebase-test-token", request.getHeader("Authorization"))
        assertEquals("application/json; charset=utf-8", request.getHeader("Content-Type"))
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"mode\":\"BICYCLE\""))
        assertTrue(body.contains("\"latitude\":35.0,\"longitude\":139.0"))
        assertTrue(body.contains("\"objective\":\"SHORTEST\""))
        assertEquals(600.0, response.elements.single().durationSeconds)
    }

    @Test
    fun `transit route sends exactly two locations and departure time`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"distanceMeters":1000,"durationSeconds":600,"legs":[{"distanceMeters":1000,"durationSeconds":600,"steps":[]}]}""",
            ),
        )

        api.route(
            mode = TravelMode.TRANSIT,
            locations = listOf(GeoPoint(35.0, 139.0), GeoPoint(35.1, 139.1)),
            departureTime = "2026-07-29T09:00:00+09:00",
        )

        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"mode\":\"TRANSIT\""))
        assertTrue(body.contains("\"departureTime\":\"2026-07-29T09:00:00+09:00\""))
        assertEquals(2, Regex("\"latitude\"").findAll(body).count())
    }

    @Test
    fun `backend errors map by stable code without exposing message`() {
        server.enqueue(
            MockResponse().setResponseCode(429).setBody(
                """{"error":{"code":"QUOTA_EXHAUSTED","message":"private upstream detail"}}""",
            ),
        )

        val exception = runCatching {
            runBlocking {
                api.reserveNavigation(25)
            }
        }.exceptionOrNull()!!

        assertTrue(exception is ApiException.QuotaExhausted)
        assertTrue(!exception.message.orEmpty().contains("private upstream detail"))
    }

    @Test
    fun `navigation reservation accepts the maximum batch`() = runBlocking {
        server.enqueue(
            MockResponse().setBody("""{"reservedDestinations":25,"remainingToday":5}"""),
        )

        val reservation = api.reserveNavigation(25)

        assertEquals(25, reservation.reservedDestinations)
        assertTrue(server.takeRequest().body.readUtf8().contains("\"destinationCount\":25"))
        assertEquals(BackendApi.BASE_URL, "https://api.anitabi.afunnypersonlol0.site")
    }
}
