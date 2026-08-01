package cn.anitabi.navigator.core.routing

import cn.anitabi.navigator.core.model.Anime
import cn.anitabi.navigator.core.model.EndPolicy
import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.NavigationState
import cn.anitabi.navigator.core.model.PilgrimagePoint
import cn.anitabi.navigator.core.model.RouteObjective
import cn.anitabi.navigator.core.model.TourLeg
import cn.anitabi.navigator.core.model.TourPlan
import cn.anitabi.navigator.core.model.TransitRoutingPreference
import cn.anitabi.navigator.core.model.TransitTimeMode
import cn.anitabi.navigator.core.model.TransitTravelMode
import cn.anitabi.navigator.core.model.TravelMode
import cn.anitabi.navigator.data.network.ApiException
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToLong

class TourPlanner(
    private val roadProvider: RoadRoutingProvider,
    private val transitProvider: TransitJourneyProvider,
    private val optimizer: TourOptimizer = TourOptimizer(),
) {
    suspend fun planRoad(request: RoadPlanRequest): TourPlan {
        require(request.mode != TravelMode.TRANSIT) { "Road planner requires drive, bike or walk mode" }
        require(request.selectedPoints.size >= 2) { "Select at least 2 pilgrimage points" }
        val startPoint = request.startPointId?.let { id ->
            request.selectedPoints.singleOrNull { it.id == id }
                ?: throw IllegalArgumentException("Start point must be selected")
        }
        val stops = request.selectedPoints.filterNot { it.id == request.startPointId }
        require(stops.isNotEmpty()) { "At least one stop must remain after the start" }
        val fixedEndPointId = if (request.endPolicy == EndPolicy.FIXED) {
            val fixedId = requireNotNull(request.fixedEndPointId) { "A fixed end point is required" }
            stops.singleOrNull { it.id == fixedId }?.id
                ?: throw IllegalArgumentException("Fixed end must be a selected point other than the start")
        } else {
            null
        }
        val orderedStops = orderRoadStops(
            start = request.start,
            stops = stops,
            mode = request.mode,
            objective = request.objective,
            endPolicy = request.endPolicy,
            fixedEndPointId = fixedEndPointId,
        )
        val orderedPoints = listOfNotNull(startPoint) + orderedStops
        val routeCoordinates = buildRouteCoordinates(request.start, orderedStops, request.endPolicy)
        val route = roadDirections(request.mode, routeCoordinates)
        return TourPlan(
            id = UUID.randomUUID().toString(),
            anime = request.anime,
            selectedPoints = request.selectedPoints,
            orderedPoints = orderedPoints,
            legs = route.toTourLegs(
                points = routeCoordinates,
                mode = request.mode,
                destinationPointIds = orderedStops.map { it.id } +
                    if (request.endPolicy == EndPolicy.RETURN_TO_START) listOf(null) else emptyList(),
            ),
            mode = request.mode,
            objective = request.objective,
            endPolicy = request.endPolicy,
            estimatedDurationSeconds = route.segments.sumOf { it.durationSeconds },
            attribution = listOf(GOOGLE_ROUTES_SOURCE, "Google"),
            initialStart = request.start,
            state = NavigationState.PLANNED,
        )
    }

    suspend fun planTransit(
        request: TransitPlanRequest,
        onProgress: (completedSegments: Int, segmentCount: Int) -> Unit = { _, _ -> },
    ): TourPlan {
        require(request.selectedPoints.size >= 2) { "Select at least 2 pilgrimage points" }
        require(request.dwellMinutes >= 0) { "Dwell time cannot be negative" }
        val startPoint = request.startPointId?.let { id ->
            request.selectedPoints.singleOrNull { it.id == id }
                ?: throw IllegalArgumentException("Start point must be selected")
        }
        val stops = request.selectedPoints.filterNot { it.id == request.startPointId }
        val orderedStops = optimizer.approximateGlobalOrder(
            start = request.start,
            points = stops,
            endPolicy = request.endPolicy,
            fixedEndPointId = request.fixedEndPointId,
        )
        val itinerary = buildTransitItinerary(
            start = request.start,
            orderedStops = orderedStops,
            timeMode = request.timeMode,
            anchorTime = request.anchorTime,
            routingPreference = request.routingPreference,
            transitTravelModes = request.transitTravelModes,
            dwellMinutes = request.dwellMinutes,
            returnToStart = request.endPolicy == EndPolicy.RETURN_TO_START,
            onProgress = onProgress,
        )
        return TourPlan(
            id = UUID.randomUUID().toString(),
            anime = request.anime,
            selectedPoints = request.selectedPoints,
            orderedPoints = listOfNotNull(startPoint) + orderedStops,
            legs = itinerary.legs,
            mode = TravelMode.TRANSIT,
            objective = RouteObjective.FASTEST,
            endPolicy = request.endPolicy,
            estimatedDurationSeconds = itinerary.estimatedDurationSeconds(
                finalDwellMinutes = if (request.endPolicy == EndPolicy.RETURN_TO_START) 0 else request.dwellMinutes,
            ),
            attribution = listOf(GOOGLE_ROUTES_SOURCE, "Google"),
            departureTime = itinerary.departureTime,
            arrivalTime = itinerary.arrivalTime,
            transitTimeMode = request.timeMode,
            transitAnchorTime = if (request.timeMode == TransitTimeMode.NOW) {
                null
            } else {
                formatTransitDepartureTime(OffsetDateTime.parse(request.anchorTime))
            },
            transitRoutingPreference = request.routingPreference,
            transitTravelModes = request.transitTravelModes,
            dwellMinutes = request.dwellMinutes,
            initialStart = request.start,
            state = NavigationState.PLANNED,
        )
    }

    suspend fun replanRemaining(
        plan: TourPlan,
        currentLocation: GeoPoint,
        completedPointIds: Set<String>,
        currentTime: String,
    ): TourPlan {
        val remaining = plan.orderedPoints.filterNot { it.id in completedPointIds }
        val originalStart = plan.initialStart ?: plan.legs.firstOrNull()?.from ?: currentLocation
        if (remaining.isEmpty()) {
            val returnItinerary = if (plan.endPolicy != EndPolicy.RETURN_TO_START) {
                null
            } else if (plan.mode == TravelMode.TRANSIT) {
                buildTransitItinerary(
                    start = currentLocation,
                    orderedStops = emptyList(),
                    timeMode = TransitTimeMode.NOW,
                    anchorTime = currentTime,
                    routingPreference = plan.transitRoutingPreference,
                    transitTravelModes = plan.transitTravelModes,
                    dwellMinutes = plan.dwellMinutes,
                    returnToStart = true,
                    returnDestination = originalStart,
                )
            } else {
                null
            }
            val returnLegs = if (plan.endPolicy != EndPolicy.RETURN_TO_START) {
                emptyList()
            } else if (plan.mode == TravelMode.TRANSIT) {
                requireNotNull(returnItinerary).legs
            } else {
                val coordinates = listOf(currentLocation, originalStart)
                roadDirections(plan.mode, coordinates).toTourLegs(
                    points = coordinates,
                    mode = plan.mode,
                    destinationPointIds = listOf(null),
                )
            }
            return plan.copy(
                orderedPoints = emptyList(),
                legs = returnLegs,
                estimatedDurationSeconds = returnItinerary?.estimatedDurationSeconds() ?:
                    returnLegs.sumOf(TourLeg::durationSeconds),
                departureTime = returnItinerary?.departureTime ?: plan.departureTime,
                arrivalTime = returnItinerary?.arrivalTime ?: plan.arrivalTime,
                transitTimeMode = if (plan.mode == TravelMode.TRANSIT) TransitTimeMode.NOW else plan.transitTimeMode,
                transitAnchorTime = if (plan.mode == TravelMode.TRANSIT) null else plan.transitAnchorTime,
                initialStart = originalStart,
            )
        }

        val legs: List<TourLeg>
        var transitItinerary: BuiltTransitItinerary? = null
        val orderedRemaining: List<PilgrimagePoint>
        if (plan.mode == TravelMode.TRANSIT) {
            orderedRemaining = when (plan.endPolicy) {
                EndPolicy.FIXED -> {
                    val fixedEnd = remaining.lastOrNull()
                    if (fixedEnd == null) emptyList()
                    else optimizer.recommendTransitOrder(currentLocation, remaining.dropLast(1)) + fixedEnd
                }
                else -> optimizer.recommendTransitOrder(currentLocation, remaining)
            }
            transitItinerary = buildTransitItinerary(
                start = currentLocation,
                orderedStops = orderedRemaining,
                timeMode = TransitTimeMode.NOW,
                anchorTime = currentTime,
                routingPreference = plan.transitRoutingPreference,
                transitTravelModes = plan.transitTravelModes,
                dwellMinutes = plan.dwellMinutes,
                returnToStart = plan.endPolicy == EndPolicy.RETURN_TO_START,
                returnDestination = originalStart,
            )
            legs = transitItinerary.legs
        } else {
            orderedRemaining = orderRoadStops(
                start = currentLocation,
                stops = remaining,
                mode = plan.mode,
                objective = plan.objective,
                endPolicy = plan.endPolicy,
                fixedEndPointId = if (plan.endPolicy == EndPolicy.FIXED) remaining.last().id else null,
            )
            val coordinates = listOf(currentLocation) + orderedRemaining.map { it.coordinate } +
                if (plan.endPolicy == EndPolicy.RETURN_TO_START) listOf(originalStart) else emptyList()
            legs = roadDirections(plan.mode, coordinates).toTourLegs(
                points = coordinates,
                mode = plan.mode,
                destinationPointIds = orderedRemaining.map { it.id } +
                    if (plan.endPolicy == EndPolicy.RETURN_TO_START) listOf(null) else emptyList(),
            )
        }
        return plan.copy(
            orderedPoints = orderedRemaining,
            legs = legs,
            estimatedDurationSeconds = transitItinerary?.estimatedDurationSeconds(
                finalDwellMinutes = if (plan.endPolicy == EndPolicy.RETURN_TO_START) 0 else plan.dwellMinutes,
            ) ?: legs.sumOf(TourLeg::durationSeconds),
            departureTime = transitItinerary?.departureTime ?: plan.departureTime,
            arrivalTime = transitItinerary?.arrivalTime ?: plan.arrivalTime,
            transitTimeMode = if (plan.mode == TravelMode.TRANSIT) TransitTimeMode.NOW else plan.transitTimeMode,
            transitAnchorTime = if (plan.mode == TravelMode.TRANSIT) null else plan.transitAnchorTime,
            initialStart = originalStart,
        )
    }

    suspend fun rebuild(plan: TourPlan, orderedPoints: List<PilgrimagePoint>): TourPlan {
        require(
            orderedPoints.size == plan.selectedPoints.size &&
                orderedPoints.map { it.id }.toSet() == plan.selectedPoints.map { it.id }.toSet(),
        ) {
            "Manual order must contain every selected point exactly once"
        }
        val start = plan.initialStart ?: plan.legs.firstOrNull()?.from ?: orderedPoints.first().coordinate
        val visits = if (orderedPoints.firstOrNull()?.coordinate == start) orderedPoints.drop(1) else orderedPoints
        var transitItinerary: BuiltTransitItinerary? = null
        val legs = if (plan.mode == TravelMode.TRANSIT) {
            val timeMode = plan.transitTimeMode
            val anchorTime = if (timeMode == TransitTimeMode.NOW) {
                requireNotNull(plan.departureTime)
            } else {
                requireNotNull(plan.transitAnchorTime)
            }
            transitItinerary = buildTransitItinerary(
                start = start,
                orderedStops = visits,
                timeMode = timeMode,
                anchorTime = anchorTime,
                routingPreference = plan.transitRoutingPreference,
                transitTravelModes = plan.transitTravelModes,
                dwellMinutes = plan.dwellMinutes,
                returnToStart = plan.endPolicy == EndPolicy.RETURN_TO_START,
            )
            transitItinerary.legs
        } else {
            val coordinates = buildRouteCoordinates(start, visits, plan.endPolicy)
            refreshChangedRoadLegs(
                plan = plan,
                coordinates = coordinates,
                destinationPointIds = visits.map { it.id } +
                    if (plan.endPolicy == EndPolicy.RETURN_TO_START) listOf(null) else emptyList(),
            )
        }
        return plan.copy(
            orderedPoints = orderedPoints,
            legs = legs,
            estimatedDurationSeconds = transitItinerary?.estimatedDurationSeconds(
                finalDwellMinutes = if (plan.endPolicy == EndPolicy.RETURN_TO_START) 0 else plan.dwellMinutes,
            ) ?: legs.sumOf(TourLeg::durationSeconds),
            departureTime = transitItinerary?.departureTime ?: plan.departureTime,
            arrivalTime = transitItinerary?.arrivalTime ?: plan.arrivalTime,
        )
    }

    private suspend fun buildTransitItinerary(
        start: GeoPoint,
        orderedStops: List<PilgrimagePoint>,
        timeMode: TransitTimeMode,
        anchorTime: String,
        routingPreference: TransitRoutingPreference,
        transitTravelModes: Set<TransitTravelMode>,
        dwellMinutes: Int,
        returnToStart: Boolean,
        returnDestination: GeoPoint = start,
        onProgress: (completedSegments: Int, segmentCount: Int) -> Unit = { _, _ -> },
    ): BuiltTransitItinerary {
        val destinations = orderedStops.map { it.coordinate } +
            if (returnToStart) listOf(returnDestination) else emptyList()
        val normalizedAnchor = formatTransitDepartureTime(OffsetDateTime.parse(anchorTime))
        if (destinations.isEmpty()) {
            return BuiltTransitItinerary(emptyList(), normalizedAnchor, normalizedAnchor)
        }
        val itinerary = if (timeMode == TransitTimeMode.ARRIVE_BY) {
            buildArriveByTransitItinerary(
                start = start,
                destinations = destinations,
                orderedStops = orderedStops,
                arrivalTime = normalizedAnchor,
                routingPreference = routingPreference,
                transitTravelModes = transitTravelModes,
                dwellMinutes = dwellMinutes,
                onProgress = onProgress,
            )
        } else {
            buildDepartAtTransitItinerary(
                start = start,
                destinations = destinations,
                orderedStops = orderedStops,
                departureTime = normalizedAnchor,
                routingPreference = routingPreference,
                transitTravelModes = transitTravelModes,
                dwellMinutes = dwellMinutes,
                onProgress = onProgress,
            )
        }
        if (
            itinerary.legs.any {
                it.from != it.to || it.distanceMeters > 0.0 || it.durationSeconds > 0.0
            } &&
            itinerary.legs.none { it.mode == TravelMode.TRANSIT }
        ) {
            throw TransitRideUnavailableException()
        }
        return itinerary
    }

    private suspend fun buildDepartAtTransitItinerary(
        start: GeoPoint,
        destinations: List<GeoPoint>,
        orderedStops: List<PilgrimagePoint>,
        departureTime: String,
        routingPreference: TransitRoutingPreference,
        transitTravelModes: Set<TransitTravelMode>,
        dwellMinutes: Int,
        onProgress: (completedSegments: Int, segmentCount: Int) -> Unit,
    ): BuiltTransitItinerary {
        val legs = mutableListOf<TourLeg>()
        var from = start
        var nextDeparture = departureTime
        var actualDeparture = departureTime
        var actualArrival = departureTime
        destinations.forEachIndexed { index, destination ->
            val journey = transitJourneyOrWalk(
                from = from,
                to = destination,
                query = TransitJourneyQuery(
                    departureTime = nextDeparture,
                    routingPreference = routingPreference,
                    transitTravelModes = transitTravelModes,
                ),
                segmentIndex = index,
                segmentCount = destinations.size,
            )
            if (index == 0) {
                actualDeparture = formatTransitDepartureTime(OffsetDateTime.parse(journey.departureTime))
            }
            actualArrival = formatTransitDepartureTime(OffsetDateTime.parse(journey.arrivalTime))
            legs += journey.withDestinationPointId(orderedStops.getOrNull(index)?.id)
            onProgress(index + 1, destinations.size)
            if (index < destinations.lastIndex) {
                nextDeparture = formatTransitDepartureTime(
                    OffsetDateTime.parse(journey.arrivalTime).plusMinutes(dwellMinutes.toLong()),
                )
            }
            from = destination
        }
        return BuiltTransitItinerary(legs, actualDeparture, actualArrival)
    }

    private suspend fun buildArriveByTransitItinerary(
        start: GeoPoint,
        destinations: List<GeoPoint>,
        orderedStops: List<PilgrimagePoint>,
        arrivalTime: String,
        routingPreference: TransitRoutingPreference,
        transitTravelModes: Set<TransitTravelMode>,
        dwellMinutes: Int,
        onProgress: (completedSegments: Int, segmentCount: Int) -> Unit,
    ): BuiltTransitItinerary {
        val origins = listOf(start) + destinations.dropLast(1)
        val segmentLegs = MutableList<List<TourLeg>>(destinations.size) { emptyList() }
        var previousArrivalDeadline = arrivalTime
        var actualDeparture = arrivalTime
        var actualArrival = arrivalTime
        var completedSegments = 0
        for (index in destinations.indices.reversed()) {
            val journey = transitJourneyOrWalk(
                from = origins[index],
                to = destinations[index],
                query = TransitJourneyQuery(
                    arrivalTime = previousArrivalDeadline,
                    routingPreference = routingPreference,
                    transitTravelModes = transitTravelModes,
                ),
                segmentIndex = index,
                segmentCount = destinations.size,
            )
            if (index == destinations.lastIndex) {
                actualArrival = formatTransitDepartureTime(OffsetDateTime.parse(journey.arrivalTime))
            }
            actualDeparture = formatTransitDepartureTime(OffsetDateTime.parse(journey.departureTime))
            segmentLegs[index] = journey.withDestinationPointId(orderedStops.getOrNull(index)?.id)
            completedSegments += 1
            onProgress(completedSegments, destinations.size)
            if (index > 0) {
                previousArrivalDeadline = formatTransitDepartureTime(
                    OffsetDateTime.parse(journey.departureTime).minusMinutes(dwellMinutes.toLong()),
                )
            }
        }
        return BuiltTransitItinerary(segmentLegs.flatten(), actualDeparture, actualArrival)
    }

    private suspend fun transitJourneyOrWalk(
        from: GeoPoint,
        to: GeoPoint,
        query: TransitJourneyQuery,
        segmentIndex: Int,
        segmentCount: Int,
    ): TransitJourney = if (from == to) {
        walkingConnector(from, to, query)
    } else try {
        transitProvider.journey(from, to, query)
    } catch (transitFailure: ApiException.NoRoute) {
        try {
            walkingConnector(from, to, query)
        } catch (walkingFailure: ApiException.NoRoute) {
            throw TransitSegmentUnavailableException(
                segmentNumber = segmentIndex + 1,
                segmentCount = segmentCount,
                from = from,
                to = to,
                cause = walkingFailure,
            )
        }
    }

    private suspend fun walkingConnector(
        from: GeoPoint,
        to: GeoPoint,
        query: TransitJourneyQuery,
    ): TransitJourney {
        val route = if (from == to) {
            RoadRoute(
                listOf(
                    RoadRouteSegment(
                        geometry = listOf(from),
                        steps = emptyList(),
                        distanceMeters = 0.0,
                        durationSeconds = 0.0,
                    ),
                ),
            )
        } else {
            roadProvider.directions(TravelMode.WALK, listOf(from, to))
        }
        if (route.segments.size != 1) {
            throw ApiException.InvalidResponse(IllegalStateException("Walking connector must contain one segment"))
        }
        val durationSeconds = route.segments.single().durationSeconds.roundToLong()
        val requestedDeparture = query.departureTime?.let(OffsetDateTime::parse)
        val requestedArrival = query.arrivalTime?.let(OffsetDateTime::parse)
        val departure = requestedDeparture ?: requireNotNull(requestedArrival).minusSeconds(durationSeconds)
        val arrival = requestedArrival ?: departure.plusSeconds(durationSeconds)
        return TransitJourney(
            legs = route.toTourLegs(
                points = listOf(from, to),
                mode = TravelMode.WALK,
                destinationPointIds = listOf(null),
            ),
            departureTime = formatTransitDepartureTime(departure),
            arrivalTime = formatTransitDepartureTime(arrival),
        )
    }

    private fun TransitJourney.withDestinationPointId(pointId: String?): List<TourLeg> =
        legs.mapIndexed { index, leg ->
            if (index == legs.lastIndex) leg.copy(destinationPointId = pointId) else leg
        }

    private fun buildRouteCoordinates(
        start: GeoPoint,
        orderedStops: List<PilgrimagePoint>,
        endPolicy: EndPolicy,
    ): List<GeoPoint> = listOf(start) + orderedStops.map { it.coordinate } +
        if (endPolicy == EndPolicy.RETURN_TO_START) listOf(start) else emptyList()

    private suspend fun orderRoadStops(
        start: GeoPoint,
        stops: List<PilgrimagePoint>,
        mode: TravelMode,
        objective: RouteObjective,
        endPolicy: EndPolicy,
        fixedEndPointId: String?,
    ): List<PilgrimagePoint> {
        val approximate = optimizer.approximateGlobalOrder(
            start = start,
            points = stops,
            endPolicy = endPolicy,
            fixedEndPointId = fixedEndPointId,
        )
        val refined = ArrayList<PilgrimagePoint>(approximate.size)
        var anchor = start
        approximate.chunked(TourRequestBatcher.MAX_MATRIX_LOCATIONS - 1).forEachIndexed { index, window ->
            val matrix = roadProvider.matrix(
                mode = mode,
                points = listOf(anchor) + window.map(PilgrimagePoint::coordinate),
                objective = objective,
            )
            val costs = when (objective) {
                RouteObjective.FASTEST -> matrix.durations
                RouteObjective.SHORTEST -> matrix.distances
            }
            val isLastWindow = index == (approximate.size - 1) / (TourRequestBatcher.MAX_MATRIX_LOCATIONS - 1)
            val localEndPolicy = if (isLastWindow && endPolicy == EndPolicy.FIXED) {
                EndPolicy.FIXED
            } else {
                EndPolicy.OPEN
            }
            val localFixedEndIndex = if (localEndPolicy == EndPolicy.FIXED) window.lastIndex else null
            val localOrder = optimizer.optimizeRoad(costs, localEndPolicy, localFixedEndIndex)
            val orderedWindow = localOrder.map(window::get)
            refined += orderedWindow
            anchor = orderedWindow.last().coordinate
        }
        return refined
    }

    private suspend fun roadDirections(mode: TravelMode, locations: List<GeoPoint>): RoadRoute = RoadRoute(
        segments = TourRequestBatcher.routeBatches(locations).flatMap { batch ->
            roadProvider.directions(mode, batch).segments
        },
    )

    private suspend fun refreshChangedRoadLegs(
        plan: TourPlan,
        coordinates: List<GeoPoint>,
        destinationPointIds: List<String?>,
    ): List<TourLeg> {
        if (plan.legs.size != coordinates.size - 1 || plan.legs.any { it.mode != plan.mode }) {
            return roadDirections(plan.mode, coordinates).toTourLegs(
                points = coordinates,
                mode = plan.mode,
                destinationPointIds = destinationPointIds,
            )
        }
        val changedIndexes = plan.legs.indices.filter { index ->
            plan.legs[index].from != coordinates[index] ||
                plan.legs[index].to != coordinates[index + 1] ||
                plan.legs[index].destinationPointId != destinationPointIds.getOrNull(index)
        }
        if (changedIndexes.isEmpty()) return plan.legs

        val refreshed = plan.legs.toMutableList()
        changedLegRanges(changedIndexes).forEach { range ->
            val requestPoints = coordinates.slice(range.first..range.last + 1)
            val segments = roadProvider.directions(plan.mode, requestPoints).segments
            check(segments.size == range.count()) { "Route leg count does not match changed locations" }
            segments.forEachIndexed { offset, segment ->
                val index = range.first + offset
                refreshed[index] = TourLeg(
                    from = coordinates[index],
                    to = coordinates[index + 1],
                    mode = plan.mode,
                    geometry = segment.geometry,
                    steps = segment.steps,
                    distanceMeters = segment.distanceMeters,
                    durationSeconds = segment.durationSeconds,
                    source = GOOGLE_ROUTES_SOURCE,
                    destinationPointId = destinationPointIds.getOrNull(index),
                )
            }
        }
        return refreshed
    }

    private fun changedLegRanges(changedIndexes: List<Int>): List<IntRange> {
        val maxLegs = TourRequestBatcher.MAX_ROUTE_LOCATIONS - 1
        val ranges = mutableListOf<IntRange>()
        var start = changedIndexes.first()
        var end = start
        changedIndexes.drop(1).forEach { index ->
            if (index == end + 1 && index - start < maxLegs) {
                end = index
            } else {
                ranges += start..end
                start = index
                end = index
            }
        }
        ranges += start..end
        return ranges
    }

    private fun RoadRoute.toTourLegs(
        points: List<GeoPoint>,
        mode: TravelMode,
        destinationPointIds: List<String?>,
    ): List<TourLeg> =
        segments.mapIndexed { index, segment ->
            TourLeg(
                from = points[index],
                to = points[index + 1],
                mode = mode,
                geometry = segment.geometry,
                steps = segment.steps,
                distanceMeters = segment.distanceMeters,
                durationSeconds = segment.durationSeconds,
                source = GOOGLE_ROUTES_SOURCE,
                destinationPointId = destinationPointIds.getOrNull(index),
            )
        }

}

private val transitDepartureTimeFormatter = DateTimeFormatterBuilder()
    .append(DateTimeFormatter.ISO_LOCAL_DATE)
    .appendLiteral('T')
    .appendPattern("HH:mm:ss")
    .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
    .appendOffsetId()
    .toFormatter(Locale.ROOT)

internal fun formatTransitDepartureTime(value: OffsetDateTime): String =
    transitDepartureTimeFormatter.format(value)

data class RoadPlanRequest(
    val anime: Anime,
    val selectedPoints: List<PilgrimagePoint>,
    val start: GeoPoint,
    val startPointId: String? = null,
    val mode: TravelMode,
    val objective: RouteObjective,
    val endPolicy: EndPolicy,
    val fixedEndPointId: String? = null,
)

data class TransitPlanRequest(
    val anime: Anime,
    val selectedPoints: List<PilgrimagePoint>,
    val start: GeoPoint,
    val startPointId: String? = null,
    val endPolicy: EndPolicy,
    val fixedEndPointId: String? = null,
    val timeMode: TransitTimeMode,
    val anchorTime: String,
    val routingPreference: TransitRoutingPreference = TransitRoutingPreference.RECOMMENDED,
    val transitTravelModes: Set<TransitTravelMode> = emptySet(),
    val dwellMinutes: Int = 15,
)

private data class BuiltTransitItinerary(
    val legs: List<TourLeg>,
    val departureTime: String,
    val arrivalTime: String,
) {
    fun estimatedDurationSeconds(finalDwellMinutes: Int = 0): Double =
        Duration.between(
            OffsetDateTime.parse(departureTime),
            OffsetDateTime.parse(arrivalTime),
        ).seconds.coerceAtLeast(0).toDouble() + finalDwellMinutes * 60.0
}

class TransitSegmentUnavailableException(
    val segmentNumber: Int,
    val segmentCount: Int,
    val from: GeoPoint,
    val to: GeoPoint,
    cause: Throwable? = null,
) : Exception("Transit segment $segmentNumber of $segmentCount has no transit or walking route", cause)

class TransitRideUnavailableException : Exception("Transit itinerary contains no transit ride")
