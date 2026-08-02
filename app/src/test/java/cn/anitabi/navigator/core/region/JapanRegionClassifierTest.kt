package cn.anitabi.navigator.core.region

import cn.anitabi.navigator.core.model.GeoPoint
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class JapanRegionClassifierTest {
    private val classifier by lazy {
        JapanRegionClassifier.load(::openPackagedAsset)
    }

    @Test
    fun `fixed Natural Earth asset classifies representative Japanese islands`() {
        val japaneseLocations = mapOf(
            "Tokyo" to GeoPoint(35.6762, 139.6503),
            "Hokkaido" to GeoPoint(43.0618, 141.3545),
            "Kyushu" to GeoPoint(33.5902, 130.4017),
            "Okinawa" to GeoPoint(26.2124, 127.6809),
            "Ogasawara" to GeoPoint(27.0667, 142.2000),
            "Tsushima" to GeoPoint(34.2000, 129.3000),
        )

        japaneseLocations.forEach { (name, point) ->
            assertEquals("$name should be classified as Japan", JapanRegion.JAPAN, classifier.classify(point))
        }
    }

    @Test
    fun `nearby foreign cities and Japanese offshore water are not Japan`() {
        val nonJapaneseLocations = mapOf(
            "Seoul" to GeoPoint(37.5665, 126.9780),
            "Busan" to GeoPoint(35.1796, 129.0756),
            "Taipei" to GeoPoint(25.0330, 121.5654),
            "Shanghai" to GeoPoint(31.2304, 121.4737),
            "Vladivostok" to GeoPoint(43.1155, 131.8855),
            "Japanese offshore water" to GeoPoint(35.0000, 145.0000),
        )

        nonJapaneseLocations.forEach { (name, point) ->
            assertEquals(
                "$name should not be classified as Japan",
                JapanRegion.NON_JAPAN,
                classifier.classify(point),
            )
        }
    }

    @Test
    fun `a vertex from the packaged Japan geometry is treated as Japan`() {
        val root = Files.newBufferedReader(packagedAssetPath(), StandardCharsets.UTF_8).use { reader ->
            Json.parseToJsonElement(reader.readText()).jsonObject
        }
        val position = root.getValue("geometry").jsonObject
            .getValue("coordinates").jsonArray[0].jsonArray[0].jsonArray[0].jsonArray
        val boundaryPoint = GeoPoint(
            latitude = position[1].jsonPrimitive.double,
            longitude = position[0].jsonPrimitive.double,
        )

        assertEquals(JapanRegion.JAPAN, classifier.classify(boundaryPoint))
    }

    @Test
    fun `point in polygon supports holes and treats every boundary as Japan`() {
        val classifier = classifierFrom(
            """{"type":"Feature","properties":{"name":"Japan","version":"5.1.1"},"geometry":{"type":"MultiPolygon","coordinates":[[[[0,0],[10,0],[10,10],[0,10],[0,0]],[[4,4],[4,6],[6,6],[6,4],[4,4]]]]}}""",
        )

        assertEquals(JapanRegion.JAPAN, classifier.classify(GeoPoint(2.0, 2.0)))
        assertEquals(JapanRegion.NON_JAPAN, classifier.classify(GeoPoint(5.0, 5.0)))
        assertEquals(JapanRegion.JAPAN, classifier.classify(GeoPoint(0.0, 5.0)))
        assertEquals(JapanRegion.JAPAN, classifier.classify(GeoPoint(5.0, 4.0)))
        assertEquals(JapanRegion.NON_JAPAN, classifier.classify(GeoPoint(20.0, 20.0)))
    }

    @Test
    fun `missing packaged asset fails closed`() {
        val failure = assertThrows(JapanRegionDataException::class.java) {
            JapanRegionClassifier.load { throw FileNotFoundException("asset unavailable") }
        }

        assertTrue(failure.cause is FileNotFoundException)
    }

    @Test
    fun `malformed or unexpected assets fail closed`() {
        listOf(
            "{not-json",
            """{"type":"Feature","properties":{"name":"Japan","version":"5.1.1"},"geometry":{"type":"Polygon","coordinates":[]}}""",
            """{"type":"Feature","properties":{"name":"Japan","version":"5.1.0"},"geometry":{"type":"MultiPolygon","coordinates":[]}}""",
            """{"type":"Feature","properties":{"name":"Japan","version":"5.1.1"},"geometry":{"type":"MultiPolygon","coordinates":[[[[0,0],[1,1],[0,0]]]]}}""",
        ).forEach { content ->
            assertThrows(JapanRegionDataException::class.java) {
                classifierFrom(content)
            }
        }
    }

    private fun openPackagedAsset(path: String): InputStream {
        assertEquals(JapanRegionClassifier.ASSET_PATH, path)
        return Files.newInputStream(packagedAssetPath())
    }

    private fun packagedAssetPath(): Path = sequenceOf(
        Path.of("src", "main", "assets").resolve(JapanRegionClassifier.ASSET_PATH),
        Path.of("app", "src", "main", "assets").resolve(JapanRegionClassifier.ASSET_PATH),
    ).firstOrNull(Files::isRegularFile)
        ?: throw FileNotFoundException(JapanRegionClassifier.ASSET_PATH)

    private fun classifierFrom(content: String): JapanRegionClassifier = JapanRegionClassifier.load {
        ByteArrayInputStream(content.toByteArray(StandardCharsets.UTF_8))
    }
}
