package cn.anitabi.navigator.data.network.ors

import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.TravelMode
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

class OrsApiTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `matrix uses the correct profile for every road mode`() = runBlocking {
        repeat(3) {
            server.enqueue(
                MockResponse().setResponseCode(200).setBody(
                    """{"durations":[[0,60],[60,0]],"distances":[[0,1000],[1000,0]]}""",
                ),
            )
        }
        val api = createApi(apiKey = "test-key")
        val points = listOf(GeoPoint(35.0, 139.0), GeoPoint(35.1, 139.1))
        val profiles = listOf(
            TravelMode.DRIVE to "driving-car",
            TravelMode.BIKE to "cycling-regular",
            TravelMode.WALK to "foot-walking",
        )

        profiles.forEach { (mode, profile) ->
            api.matrix(mode, points)
            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/v2/matrix/$profile", request.requestUrl?.encodedPath)
            assertEquals("test-key", request.getHeader("Authorization"))
            val requestBody = request.body.readUtf8()
            assertTrue(requestBody.contains("\"locations\":[[139.0,35.0],[139.1,35.1]]"))
            assertTrue(requestBody.contains("\"metrics\":[\"distance\",\"duration\"]"))
            assertTrue(requestBody.contains("\"units\":\"m\""))
        }
    }

    @Test
    fun `directions requests Chinese instructions and GeoJSON`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"features":[{"geometry":{"coordinates":[[139.0,35.0],[139.1,35.1]]},"properties":{"segments":[]}}]}""",
            ),
        )
        val api = createApi(apiKey = "test-key")

        val response = api.directions(
            TravelMode.WALK,
            listOf(GeoPoint(35.0, 139.0), GeoPoint(35.1, 139.1)),
        )

        val request = server.takeRequest()
        assertEquals("/v2/directions/foot-walking/geojson", request.requestUrl?.encodedPath)
        assertEquals("application/json; charset=utf-8", request.getHeader("Content-Type"))
        assertEquals("AnitabiNavigator/test (https://example.org)", request.getHeader("User-Agent"))
        val requestBody = request.body.readUtf8()
        assertTrue(requestBody.contains("\"instructions\":true"))
        assertTrue(requestBody.contains("\"language\":\"zh-cn\""))
        assertEquals(2, response.features.single().geometry.coordinates.size)
    }

    @Test(expected = ApiException.MissingOrsKey::class)
    fun `missing key fails before making a request`() {
        runBlocking {
            createApi(apiKey = null).matrix(
                TravelMode.DRIVE,
                listOf(GeoPoint(35.0, 139.0), GeoPoint(35.1, 139.1)),
            )
        }
    }

    private fun createApi(apiKey: String?): OrsApi {
        val client = ApiHttpClient(
            UserAgentInterceptor(
                appName = "AnitabiNavigator",
                appVersion = "test",
                contact = "https://example.org",
            ),
        )
        return OrsApi(
            httpClient = client,
            apiKey = { apiKey },
            baseUrl = server.url("/v2").toString().removeSuffix("/"),
        )
    }
}
