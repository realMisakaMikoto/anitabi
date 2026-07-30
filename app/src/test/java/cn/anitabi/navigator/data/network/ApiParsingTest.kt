package cn.anitabi.navigator.data.network

import cn.anitabi.navigator.data.network.anitabi.AnitabiLiteDto
import cn.anitabi.navigator.data.network.anitabi.AnitabiPointDto
import cn.anitabi.navigator.data.network.bangumi.BangumiSearchResponse
import kotlinx.serialization.builtins.ListSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
            """[{"id":"p1","name":"","cn":null,"ep":"CD","geo":[35.6812,139.7671],"origin":"","originURL":null}]""",
        )
        val point = points.single().toPilgrimagePointOrNull()!!

        assertEquals("未命名地点", point.name)
        assertEquals(listOf(139.7671, 35.6812), point.coordinate.toGeoJsonPosition())
        assertNull(point.origin)
    }

    @Test
    fun `Anitabi optional text type changes do not reject valid point data`() {
        val points = json.decodeFromString(
            ListSerializer(AnitabiPointDto.serializer()),
            """[{"id":"p1","name":28,"cn":null,"image":false,"geo":[35.6812,139.7671],"origin":1,"originURL":[]}]""",
        )
        val point = points.single().toPilgrimagePointOrNull()!!

        assertEquals("未命名地点", point.name)
        assertNull(point.imageUrl)
        assertNull(point.origin)
        assertNull(point.originUrl)
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
    fun `HTTP statuses map to explicit errors`() {
        assertTrue(ApiException.fromStatus(404, "missing") is ApiException.NotFound)
        assertTrue(ApiException.fromStatus(429, "quota") is ApiException.RateLimited)
        assertTrue(ApiException.fromStatus(503, "down") is ApiException.Server)
        assertTrue(ApiException.fromStatus(401, "do not expose this body") is ApiException.InvalidCredentials)
        assertTrue(ApiException.fromStatus(403, "blocked") is ApiException.Forbidden)
    }

    @Test
    fun `HTTP errors never expose response bodies`() {
        val secret = "upstream response must stay private"

        listOf(404, 418, 429, 503).forEach { status ->
            val exception = ApiException.fromStatus(status, secret)
            assertFalse(exception.message.orEmpty().contains(secret))
        }
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
