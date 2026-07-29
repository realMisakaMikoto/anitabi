package cn.anitabi.navigator.data.network

import cn.anitabi.navigator.data.network.anitabi.AnitabiLiteDto
import cn.anitabi.navigator.data.network.anitabi.AnitabiPointDto
import cn.anitabi.navigator.data.network.bangumi.BangumiSearchResponse
import cn.anitabi.navigator.data.network.ors.OrsDirectionsResponse
import cn.anitabi.navigator.data.network.transit.TransitPlanDto
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiParsingTest {
    private val json = ApiHttpClient.defaultJson

    @Test
    fun `Bangumi search accepts optional images and Chinese name`() {
        val response = json.decodeFromString(
            BangumiSearchResponse.serializer(),
            """{"total":1,"data":[{"id":115908,"name":"Hibike! Euphonium","name_cn":"吹响！上低音号","images":null,"unknown":true}]}""",
        )

        assertEquals("吹响！上低音号", response.data.single().toAnime().nameCn)
        assertNull(response.data.single().toAnime().imageUrl)
    }

    @Test
    fun `Anitabi empty fields are tolerated and coordinates are swapped`() {
        val points = json.decodeFromString(
            ListSerializer(AnitabiPointDto.serializer()),
            """[{"id":"p1","name":"","cn":null,"geo":[35.6812,139.7671],"origin":"","originURL":null}]""",
        )
        val point = points.single().toPilgrimagePointOrNull()!!

        assertEquals("未命名地点", point.name)
        assertEquals(listOf(139.7671, 35.6812), point.coordinate.toGeoJsonPosition())
        assertNull(point.origin)
    }

    @Test
    fun `Anitabi invalid coordinate is skipped`() {
        val point = AnitabiPointDto(id = "bad", geo = listOf(200.0))

        assertNull(point.toPilgrimagePointOrNull())
    }

    @Test
    fun `Anitabi rejects non web origin links`() {
        val point = AnitabiPointDto(
            id = "unsafe",
            geo = listOf(35.0, 139.0),
            originUrl = "intent://open-an-app",
        ).toPilgrimagePointOrNull()!!

        assertNull(point.originUrl)
    }

    @Test
    fun `Anitabi images are restricted to the documented image API`() {
        val officialCover = "https://image.anitabi.cn/bangumi/115908.jpg?plan=h160"
        val officialPoint = "https://image.anitabi.cn/points/115908/qys7fu.jpg?plan=h360"

        assertEquals(officialCover, AnitabiLiteDto(id = 115908, cover = officialCover).toAnime().imageUrl)
        assertEquals(
            officialPoint,
            AnitabiPointDto(id = "safe", image = officialPoint, geo = listOf(35.0, 139.0))
                .toPilgrimagePointOrNull()
                ?.imageUrl,
        )
        assertNull(
            AnitabiLiteDto(id = 115908, cover = "https://anitabi.cn/cover.jpg").toAnime().imageUrl,
        )
        assertNull(
            AnitabiPointDto(
                id = "lookalike",
                image = "https://image.anitabi.cn.example.org/point.jpg",
                geo = listOf(35.0, 139.0),
            ).toPilgrimagePointOrNull()?.imageUrl,
        )
    }

    @Test
    fun `ORS GeoJSON and Transitous itinerary samples parse`() {
        val ors = json.decodeFromString(
            OrsDirectionsResponse.serializer(),
            """{"features":[{"geometry":{"coordinates":[[139.0,35.0],[139.1,35.1]]},"properties":{"segments":[{"distance":1000.0,"duration":600.0,"steps":[{"distance":100.0,"duration":60.0,"instruction":"直行","way_points":[0,1]}]}]}}]}""",
        )
        val transit = json.decodeFromString(
            TransitPlanDto.serializer(),
            """{"itineraries":[{"id":"j1","duration":900,"startTime":"2026-07-29T09:00:00+09:00","endTime":"2026-07-29T09:15:00+09:00","transfers":0,"legs":[{"mode":"WALK","from":{"name":"A","lat":35.0,"lon":139.0},"to":{"name":"B","lat":35.1,"lon":139.1},"duration":900,"startTime":"2026-07-29T09:00:00+09:00","endTime":"2026-07-29T09:15:00+09:00","legGeometry":{"points":"","length":0},"realTime":false,"scheduled":true}]}],"direct":[],"previousPageCursor":"","nextPageCursor":""}""",
        )

        assertEquals("直行", ors.features.single().properties.segments.single().steps.single().instruction)
        assertEquals("WALK", transit.itineraries.single().legs.single().mode)
    }

    @Test
    fun `HTTP statuses map to explicit errors`() {
        assertTrue(ApiException.fromStatus(404, "missing") is ApiException.NotFound)
        assertTrue(ApiException.fromStatus(429, "quota") is ApiException.RateLimited)
        assertTrue(ApiException.fromStatus(503, "down") is ApiException.Server)
        assertTrue(ApiException.fromStatus(401, "do not expose this body") is ApiException.InvalidCredentials)
        assertTrue(ApiException.fromStatus(403, "blocked") is ApiException.Forbidden)
    }

    @Test
    fun `User-Agent requires an identifiable contact`() {
        val value = UserAgentInterceptor.buildUserAgent(
            appName = "AnitabiNavigator",
            appVersion = "0.1.0",
            contact = "https://example.org/contact",
        )

        assertEquals("AnitabiNavigator/0.1.0 (https://example.org/contact)", value)
    }
}
