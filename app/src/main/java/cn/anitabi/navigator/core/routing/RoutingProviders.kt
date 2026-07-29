package cn.anitabi.navigator.core.routing

import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.RouteStep
import cn.anitabi.navigator.core.model.TourLeg
import cn.anitabi.navigator.core.model.TransitLegDetails
import cn.anitabi.navigator.core.model.TravelMode
import cn.anitabi.navigator.data.network.ApiException
import cn.anitabi.navigator.data.network.ors.OrsApi
import cn.anitabi.navigator.data.network.transit.TransitLegDto
import cn.anitabi.navigator.data.network.transit.TransitousApi

interface RoadRoutingProvider {
    suspend fun matrix(mode: TravelMode, points: List<GeoPoint>): TravelMatrix
    suspend fun directions(mode: TravelMode, points: List<GeoPoint>): RoadRoute
}

data class TravelMatrix(
    val durations: List<List<Double?>>,
    val distances: List<List<Double?>>,
)

data class RoadRoute(val segments: List<RoadRouteSegment>)

data class RoadRouteSegment(
    val geometry: List<GeoPoint>,
    val steps: List<RouteStep>,
    val distanceMeters: Double,
    val durationSeconds: Double,
)

class OrsRoadRoutingProvider(private val api: OrsApi) : RoadRoutingProvider {
    override suspend fun matrix(mode: TravelMode, points: List<GeoPoint>): TravelMatrix {
        val response = api.matrix(mode, points)
        return TravelMatrix(response.durations, response.distances)
    }

    override suspend fun directions(mode: TravelMode, points: List<GeoPoint>): RoadRoute {
        val response = api.directions(mode, points)
        val feature = response.features.singleOrNull()
            ?: throw ApiException.InvalidResponse(IllegalStateException("ORS returned no unique route feature"))
        if (feature.properties.segments.size != points.size - 1) {
            throw ApiException.InvalidResponse(IllegalStateException("ORS segment count does not match waypoints"))
        }
        val fullGeometry = feature.geometry.coordinates.mapNotNull { coordinate ->
            if (coordinate.size < 2) null else runCatching {
                GeoPoint(latitude = coordinate[1], longitude = coordinate[0])
            }.getOrNull()
        }
        val segments = feature.properties.segments.map { segment ->
            val geometryIndexes = segment.steps.flatMap { it.wayPoints }.filter { it in fullGeometry.indices }
            val segmentGeometry = if (geometryIndexes.isEmpty()) {
                fullGeometry
            } else {
                fullGeometry.slice(geometryIndexes.min()..geometryIndexes.max())
            }
            RoadRouteSegment(
                geometry = segmentGeometry,
                steps = segment.steps.map { step ->
                    RouteStep(
                        instruction = step.instruction,
                        distanceMeters = step.distance,
                        durationSeconds = step.duration,
                        coordinate = step.wayPoints.firstOrNull()
                            ?.takeIf(fullGeometry.indices::contains)
                            ?.let(fullGeometry::get),
                    )
                },
                distanceMeters = segment.distance,
                durationSeconds = segment.duration,
            )
        }
        return RoadRoute(segments)
    }
}

interface TransitJourneyProvider {
    suspend fun journey(from: GeoPoint, to: GeoPoint, departureTime: String): TransitJourney
}

data class TransitJourney(
    val legs: List<TourLeg>,
    val arrivalTime: String,
)

class TransitousJourneyProvider(private val api: TransitousApi) : TransitJourneyProvider {
    override suspend fun journey(
        from: GeoPoint,
        to: GeoPoint,
        departureTime: String,
    ): TransitJourney {
        val itinerary = api.plan(from, to, departureTime).itineraries.firstOrNull()
            ?: throw NoTransitDataException("No public transit itinerary covers this area and time")
        return TransitJourney(
            legs = itinerary.legs.map(TransitLegDto::toTourLeg),
            arrivalTime = itinerary.endTime,
        )
    }
}

private fun TransitLegDto.toTourLeg(): TourLeg {
    val fromPoint = GeoPoint(from.lat, from.lon)
    val toPoint = GeoPoint(to.lat, to.lon)
    val decodedGeometry = PolylineDecoder.decode(legGeometry.points, precision = 6)
    val geometry = decodedGeometry.ifEmpty { listOf(fromPoint, toPoint) }
    val lineName = routeShortName?.takeIf(String::isNotBlank)
        ?: routeLongName?.takeIf(String::isNotBlank)
    val instruction = when (mode) {
        "WALK" -> "步行至 ${to.name}"
        else -> buildString {
            append(lineName ?: mode)
            headsign?.takeIf(String::isNotBlank)?.let { append(" · 开往 $it") }
        }
    }
    return TourLeg(
        from = fromPoint,
        to = toPoint,
        mode = TravelMode.TRANSIT,
        geometry = geometry,
        steps = listOf(RouteStep(instruction, distance ?: 0.0, duration.toDouble(), fromPoint)),
        distanceMeters = distance ?: 0.0,
        durationSeconds = duration.toDouble(),
        source = "Transitous / MOTIS",
        transit = TransitLegDetails(
            vehicleMode = mode,
            line = lineName,
            direction = headsign,
            departureTime = startTime,
            arrivalTime = endTime,
            departurePlatform = from.track ?: from.scheduledTrack,
            arrivalPlatform = to.track ?: to.scheduledTrack,
            intermediateStops = intermediateStops.map { it.name },
            realtime = realTime,
            cancelled = cancelled,
        ),
    )
}

class NoTransitDataException(message: String) : Exception(message)
