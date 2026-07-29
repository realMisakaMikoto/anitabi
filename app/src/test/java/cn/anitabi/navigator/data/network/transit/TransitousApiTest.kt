package cn.anitabi.navigator.data.network.transit

import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.data.network.ApiHttpClient
import cn.anitabi.navigator.data.network.UserAgentInterceptor
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TransitousApiTest {
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
    fun `plan uses official endpoint contract and identifiable user agent`() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"itineraries":[],"direct":[]}"""))
        val client = ApiHttpClient(
            UserAgentInterceptor(
                appName = "AnitabiNavigator",
                appVersion = "0.1.3",
                contact = "https://github.com/realMisakaMikoto",
            ),
        )
        val api = TransitousApi(client, server.url("/api/v6/plan").toString())

        api.plan(
            from = GeoPoint(35.681236, 139.767125),
            to = GeoPoint(35.710063, 139.8107),
            departureTime = "2026-07-29T09:00:00+09:00",
        )

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/v6/plan", request.requestUrl?.encodedPath)
        assertEquals("35.681236,139.767125", request.requestUrl?.queryParameter("fromPlace"))
        assertEquals("35.710063,139.8107", request.requestUrl?.queryParameter("toPlace"))
        assertEquals("2026-07-29T09:00:00+09:00", request.requestUrl?.queryParameter("time"))
        assertEquals("TRANSIT", request.requestUrl?.queryParameter("transitModes"))
        assertEquals("", request.requestUrl?.queryParameter("directModes"))
        assertEquals("true", request.requestUrl?.queryParameter("detailedLegs"))
        assertEquals(
            "AnitabiNavigator/0.1.3 (https://github.com/realMisakaMikoto)",
            request.getHeader("User-Agent"),
        )
        assertEquals("https://api.transitous.org/api/v6/plan", TransitousApi.PLAN_URL)
    }
}
