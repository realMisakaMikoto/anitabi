package cn.anitabi.navigator.core.region

import cn.anitabi.navigator.core.model.GeoPoint
import java.io.InputStream
import java.nio.charset.StandardCharsets
import kotlin.math.abs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

enum class JapanRegion {
    JAPAN,
    NON_JAPAN,
}

class JapanRegionDataException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause)

class JapanRegionClassifier private constructor(
    private val polygons: List<Polygon>,
    private val bounds: Bounds,
) {
    fun classify(point: GeoPoint): JapanRegion {
        val coordinate = Coordinate(point.longitude, point.latitude)
        if (!bounds.contains(coordinate)) return JapanRegion.NON_JAPAN
        return if (polygons.any { it.contains(coordinate) }) {
            JapanRegion.JAPAN
        } else {
            JapanRegion.NON_JAPAN
        }
    }

    companion object {
        const val ASSET_PATH = "natural_earth/ne_10m_admin_0_countries_japan_v5.1.1.geojson"
        private const val DATA_VERSION = "5.1.1"
        private const val DATA_NAME = "Japan"

        fun load(openAsset: (String) -> InputStream): JapanRegionClassifier = try {
            val content = openAsset(ASSET_PATH).use { stream ->
                stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            }
            parse(content)
        } catch (exception: JapanRegionDataException) {
            throw exception
        } catch (exception: Exception) {
            throw JapanRegionDataException("Japan region data could not be loaded", exception)
        }

        private fun parse(content: String): JapanRegionClassifier = try {
            val feature = Json.parseToJsonElement(content).jsonObject
            requireValue(feature["type"]?.jsonPrimitive?.contentOrNull == "Feature") {
                "Root must be a GeoJSON Feature"
            }
            val properties = feature["properties"]?.jsonObject
                ?: invalidData("Feature properties are missing")
            requireValue(properties["name"]?.jsonPrimitive?.contentOrNull == DATA_NAME) {
                "Feature is not Japan"
            }
            requireValue(properties["version"]?.jsonPrimitive?.contentOrNull == DATA_VERSION) {
                "Natural Earth version does not match $DATA_VERSION"
            }
            val geometry = feature["geometry"]?.jsonObject
                ?: invalidData("Feature geometry is missing")
            requireValue(geometry["type"]?.jsonPrimitive?.contentOrNull == "MultiPolygon") {
                "Japan geometry must be a MultiPolygon"
            }
            val polygonElements = geometry["coordinates"]?.jsonArray
                ?: invalidData("MultiPolygon coordinates are missing")
            requireValue(polygonElements.isNotEmpty()) { "MultiPolygon has no polygons" }
            val polygons = polygonElements.mapIndexed(::parsePolygon)
            JapanRegionClassifier(
                polygons = polygons,
                bounds = Bounds.enclosingBounds(polygons.map(Polygon::bounds)),
            )
        } catch (exception: JapanRegionDataException) {
            throw exception
        } catch (exception: Exception) {
            throw JapanRegionDataException("Japan region data is invalid", exception)
        }

        private fun parsePolygon(index: Int, element: JsonElement): Polygon {
            val ringElements = element.jsonArray
            requireValue(ringElements.isNotEmpty()) { "Polygon $index has no rings" }
            val rings = ringElements.mapIndexed { ringIndex, ring ->
                parseRing(index, ringIndex, ring.jsonArray)
            }
            return Polygon(outer = rings.first(), holes = rings.drop(1))
        }

        private fun parseRing(polygonIndex: Int, ringIndex: Int, positions: JsonArray): Ring {
            requireValue(positions.size >= 4) {
                "Polygon $polygonIndex ring $ringIndex has fewer than four positions"
            }
            val points = positions.mapIndexed { positionIndex, position ->
                val values = position.jsonArray
                requireValue(values.size >= 2) {
                    "Polygon $polygonIndex ring $ringIndex position $positionIndex is incomplete"
                }
                val coordinate = Coordinate(
                    longitude = values[0].jsonPrimitive.double,
                    latitude = values[1].jsonPrimitive.double,
                )
                requireValue(coordinate.longitude in -180.0..180.0) {
                    "Polygon $polygonIndex ring $ringIndex has an invalid longitude"
                }
                requireValue(coordinate.latitude in -90.0..90.0) {
                    "Polygon $polygonIndex ring $ringIndex has an invalid latitude"
                }
                coordinate
            }
            requireValue(points.first() == points.last()) {
                "Polygon $polygonIndex ring $ringIndex is not closed"
            }
            requireValue(points.dropLast(1).toSet().size >= 3) {
                "Polygon $polygonIndex ring $ringIndex is degenerate"
            }
            requireValue(abs(signedArea(points)) > AREA_EPSILON) {
                "Polygon $polygonIndex ring $ringIndex has zero area"
            }
            return Ring(points)
        }

        private fun requireValue(value: Boolean, message: () -> String) {
            if (!value) invalidData(message())
        }

        private fun invalidData(message: String): Nothing =
            throw JapanRegionDataException("Japan region data is invalid: $message")

        private fun signedArea(points: List<Coordinate>): Double = points.zipWithNext().sumOf { (from, to) ->
            from.longitude * to.latitude - to.longitude * from.latitude
        } / 2.0
    }
}

private data class Polygon(
    val outer: Ring,
    val holes: List<Ring>,
) {
    val bounds: Bounds = outer.bounds

    fun contains(point: Coordinate): Boolean {
        if (!bounds.contains(point)) return false
        return when (outer.relationTo(point)) {
            PointRelation.OUTSIDE -> false
            PointRelation.BOUNDARY -> true
            PointRelation.INSIDE -> holes.none { hole ->
                when (hole.relationTo(point)) {
                    PointRelation.INSIDE -> true
                    PointRelation.BOUNDARY -> return true
                    PointRelation.OUTSIDE -> false
                }
            }
        }
    }
}

private data class Ring(val points: List<Coordinate>) {
    val bounds: Bounds = Bounds.enclosingCoordinates(points)

    fun relationTo(point: Coordinate): PointRelation {
        if (!bounds.contains(point)) return PointRelation.OUTSIDE
        var inside = false
        points.zipWithNext().forEach { (from, to) ->
            if (point.isOnSegment(from, to)) return PointRelation.BOUNDARY
            val crossesLatitude = (from.latitude > point.latitude) != (to.latitude > point.latitude)
            if (crossesLatitude) {
                val intersectionLongitude = (to.longitude - from.longitude) *
                    (point.latitude - from.latitude) / (to.latitude - from.latitude) + from.longitude
                if (point.longitude < intersectionLongitude) inside = !inside
            }
        }
        return if (inside) PointRelation.INSIDE else PointRelation.OUTSIDE
    }
}

private data class Coordinate(
    val longitude: Double,
    val latitude: Double,
) {
    fun isOnSegment(from: Coordinate, to: Coordinate): Boolean {
        val deltaLongitude = to.longitude - from.longitude
        val deltaLatitude = to.latitude - from.latitude
        val cross = (longitude - from.longitude) * deltaLatitude -
            (latitude - from.latitude) * deltaLongitude
        val tolerance = BOUNDARY_EPSILON * (abs(deltaLongitude) + abs(deltaLatitude) + 1.0)
        if (abs(cross) > tolerance) return false
        return longitude >= minOf(from.longitude, to.longitude) - BOUNDARY_EPSILON &&
            longitude <= maxOf(from.longitude, to.longitude) + BOUNDARY_EPSILON &&
            latitude >= minOf(from.latitude, to.latitude) - BOUNDARY_EPSILON &&
            latitude <= maxOf(from.latitude, to.latitude) + BOUNDARY_EPSILON
    }
}

private data class Bounds(
    val minimumLongitude: Double,
    val minimumLatitude: Double,
    val maximumLongitude: Double,
    val maximumLatitude: Double,
) {
    fun contains(point: Coordinate): Boolean =
        point.longitude >= minimumLongitude - BOUNDARY_EPSILON &&
            point.longitude <= maximumLongitude + BOUNDARY_EPSILON &&
            point.latitude >= minimumLatitude - BOUNDARY_EPSILON &&
            point.latitude <= maximumLatitude + BOUNDARY_EPSILON

    companion object {
        fun enclosingCoordinates(points: List<Coordinate>): Bounds {
            require(points.isNotEmpty())
            return Bounds(
                minimumLongitude = points.minOf(Coordinate::longitude),
                minimumLatitude = points.minOf(Coordinate::latitude),
                maximumLongitude = points.maxOf(Coordinate::longitude),
                maximumLatitude = points.maxOf(Coordinate::latitude),
            )
        }

        fun enclosingBounds(bounds: List<Bounds>): Bounds {
            require(bounds.isNotEmpty())
            return Bounds(
                minimumLongitude = bounds.minOf(Bounds::minimumLongitude),
                minimumLatitude = bounds.minOf(Bounds::minimumLatitude),
                maximumLongitude = bounds.maxOf(Bounds::maximumLongitude),
                maximumLatitude = bounds.maxOf(Bounds::maximumLatitude),
            )
        }
    }
}

private enum class PointRelation {
    OUTSIDE,
    INSIDE,
    BOUNDARY,
}

private const val BOUNDARY_EPSILON = 1e-10
private const val AREA_EPSILON = 1e-14
