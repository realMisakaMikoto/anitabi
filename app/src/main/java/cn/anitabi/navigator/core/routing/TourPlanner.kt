package cn.anitabi.navigator.core.routing

import cn.anitabi.navigator.core.model.Anime
import cn.anitabi.navigator.core.model.EndPolicy
import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.NavigationState
import cn.anitabi.navigator.core.model.PilgrimagePoint
import cn.anitabi.navigator.core.model.RouteObjective
import cn.anitabi.navigator.core.model.TourLeg
import cn.anitabi.navigator.core.model.TourPlan
import cn.anitabi.navigator.core.model.TravelMode
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.Locale
import java.util.UUID

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

    suspend fun planTransit(request: TransitPlanRequest): TourPlan {
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
        val legs = buildTransitLegs(
            start = request.start,
            orderedStops = orderedStops,
            departureTime = request.departureTime,
            dwellMinutes = request.dwellMinutes,
            returnToStart = request.endPolicy == EndPolicy.RETURN_TO_START,
        )
        return TourPlan(
            id = UUID.randomUUID().toString(),
            anime = request.anime,
            selectedPoints = request.selectedPoints,
            orderedPoints = listOfNotNull(startPoint) + orderedStops,
            legs = legs,
            mode = TravelMode.TRANSIT,
            objective = RouteObjective.FASTEST,
            endPolicy = request.endPolicy,
            estimatedDurationSeconds = legs.sumOf(TourLeg::durationSeconds) +
                request.dwellMinutes * 60.0 * orderedStops.size,
            attribution = listOf(GOOGLE_ROUTES_SOURCE, "Google"),
            departureTime = request.departureTime,
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
            val returnLegs = if (plan.endPolicy != EndPolicy.RETURN_TO_START) {
                emptyList()
            } else if (plan.mode == TravelMode.TRANSIT) {
                buildTransitLegs(
                    start = currentLocation,
                    orderedStops = emptyList(),
                    departureTime = currentTime,
                    dwellMinutes = plan.dwellMinutes,
                    returnToStart = true,
                    returnDestination = originalStart,
                )
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
                estimatedDurationSeconds = returnLegs.sumOf(TourLeg::durationSeconds),
                departureTime = if (plan.mode == TravelMode.TRANSIT) currentTime else plan.departureTime,
                initialStart = originalStart,
            )
        }

        val legs: List<TourLeg>
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
            legs = buildTransitLegs(
                start = currentLocation,
                orderedStops = orderedRemaining,
                departureTime = currentTime,
                dwellMinutes = plan.dwellMinutes,
                returnToStart = plan.endPolicy == EndPolicy.RETURN_TO_START,
                returnDestination = originalStart,
            )
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
            estimatedDurationSeconds = legs.sumOf(TourLeg::durationSeconds) +
                if (plan.mode == TravelMode.TRANSIT) {
                    plan.dwellMinutes * 60.0 * orderedRemaining.size
                } else {
                    0.0
                },
            departureTime = if (plan.mode == TravelMode.TRANSIT) currentTime else plan.departureTime,
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
        val start = plan.legs.firstOrNull()?.from ?: orderedPoints.first().coordinate
        val visits = if (orderedPoints.firstOrNull()?.coordinate == start) orderedPoints.drop(1) else orderedPoints
        val legs = if (plan.mode == TravelMode.TRANSIT) {
            buildTransitLegs(
                start = start,
                orderedStops = visits,
                departureTime = requireNotNull(plan.departureTime),
                dwellMinutes = plan.dwellMinutes,
                returnToStart = plan.endPolicy == EndPolicy.RETURN_TO_START,
            )
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
            estimatedDurationSeconds = legs.sumOf(TourLeg::durationSeconds) +
                if (plan.mode == TravelMode.TRANSIT) plan.dwellMinutes * 60.0 * visits.size else 0.0,
        )
    }

    private suspend fun buildTransitLegs(
        start: GeoPoint,
        orderedStops: List<PilgrimagePoint>,
        departureTime: String,
        dwellMinutes: Int,
        returnToStart: Boolean,
        returnDestination: GeoPoint = start,
    ): List<TourLeg> {
        val destinations = orderedStops.map { it.coordinate } +
            if (returnToStart) listOf(returnDestination) else emptyList()
        val legs = mutableListOf<TourLeg>()
        var from = start
        var nextDeparture = formatTransitDepartureTime(OffsetDateTime.parse(departureTime))
        destinations.forEachIndexed { index, destination ->
            val journey = transitProvider.journey(from, destination, nextDeparture)
            val destinationPointId = orderedStops.getOrNull(index)?.id
            legs += journey.legs.mapIndexed { legIndex, leg ->
                if (legIndex == journey.legs.lastIndex) leg.copy(destinationPointId = destinationPointId) else leg
            }
            val isFinalReturn = returnToStart && index == destinations.lastIndex
            nextDeparture = if (isFinalReturn) {
                journey.arrivalTime
            } else {
                formatTransitDepartureTime(
                    OffsetDateTime.parse(journey.arrivalTime).plusMinutes(dwellMinutes.toLong()),
                )
            }
            from = destination
        }
        return legs
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
    val departureTime: String,
    val dwellMinutes: Int = 15,
)
