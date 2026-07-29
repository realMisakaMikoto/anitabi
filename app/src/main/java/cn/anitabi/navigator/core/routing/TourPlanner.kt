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
import java.util.UUID

class TourPlanner(
    private val roadProvider: RoadRoutingProvider,
    private val transitProvider: TransitJourneyProvider,
    private val optimizer: TourOptimizer = TourOptimizer(),
) {
    suspend fun planRoad(request: RoadPlanRequest): TourPlan {
        require(request.mode != TravelMode.TRANSIT) { "Road planner requires drive, bike or walk mode" }
        require(request.selectedPoints.size in 2..12) { "Select 2 to 12 pilgrimage points" }
        val startPoint = request.startPointId?.let { id ->
            request.selectedPoints.singleOrNull { it.id == id }
                ?: throw IllegalArgumentException("Start point must be selected")
        }
        val stops = request.selectedPoints.filterNot { it.id == request.startPointId }
        require(stops.isNotEmpty()) { "At least one stop must remain after the start" }
        val fixedEndIndex = if (request.endPolicy == EndPolicy.FIXED) {
            val fixedId = requireNotNull(request.fixedEndPointId) { "A fixed end point is required" }
            stops.indexOfFirst { it.id == fixedId }.takeIf { it >= 0 }
                ?: throw IllegalArgumentException("Fixed end must be a selected point other than the start")
        } else {
            null
        }

        val matrixCoordinates = listOf(request.start) + stops.map { it.coordinate }
        val matrix = roadProvider.matrix(request.mode, matrixCoordinates)
        val costs = when (request.objective) {
            RouteObjective.FASTEST -> matrix.durations
            RouteObjective.SHORTEST -> matrix.distances
        }
        val order = optimizer.optimizeRoad(costs, request.endPolicy, fixedEndIndex)
        val orderedStops = order.map(stops::get)
        val orderedPoints = listOfNotNull(startPoint) + orderedStops
        val routeCoordinates = buildRouteCoordinates(request.start, orderedStops, request.endPolicy)
        val route = roadProvider.directions(request.mode, routeCoordinates)
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
            attribution = listOf("openrouteservice / HeiGIT", "© OpenStreetMap contributors"),
            initialStart = request.start,
            state = NavigationState.PLANNED,
        )
    }

    suspend fun planTransit(request: TransitPlanRequest): TourPlan {
        require(request.selectedPoints.size in 2..MAX_TRANSIT_POINTS) {
            "Transit supports 2 to $MAX_TRANSIT_POINTS pilgrimage points"
        }
        require(request.dwellMinutes >= 0) { "Dwell time cannot be negative" }
        val startPoint = request.startPointId?.let { id ->
            request.selectedPoints.singleOrNull { it.id == id }
                ?: throw IllegalArgumentException("Start point must be selected")
        }
        val stops = request.selectedPoints.filterNot { it.id == request.startPointId }
        val orderedStops = when (request.endPolicy) {
            EndPolicy.FIXED -> {
                val fixedId = requireNotNull(request.fixedEndPointId) { "A fixed end point is required" }
                val fixed = stops.singleOrNull { it.id == fixedId }
                    ?: throw IllegalArgumentException("Fixed end must be a selected point other than the start")
                optimizer.recommendTransitOrder(request.start, stops - fixed) + fixed
            }
            else -> optimizer.recommendTransitOrder(request.start, stops)
        }
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
            attribution = listOf(
                "Transitous / MOTIS",
                "Transit data sources: https://transitous.org/sources/",
                "© OpenStreetMap contributors",
            ),
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
                roadProvider.directions(plan.mode, coordinates).toTourLegs(
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
            val matrixCoordinates = listOf(currentLocation) + remaining.map { it.coordinate }
            val matrix = roadProvider.matrix(plan.mode, matrixCoordinates)
            val costs = when (plan.objective) {
                RouteObjective.FASTEST -> matrix.durations
                RouteObjective.SHORTEST -> matrix.distances
            }
            val fixedEndIndex = if (plan.endPolicy == EndPolicy.FIXED) remaining.lastIndex else null
            orderedRemaining = optimizer.optimizeRoad(costs, plan.endPolicy, fixedEndIndex).map(remaining::get)
            val coordinates = listOf(currentLocation) + orderedRemaining.map { it.coordinate } +
                if (plan.endPolicy == EndPolicy.RETURN_TO_START) listOf(originalStart) else emptyList()
            legs = roadProvider.directions(plan.mode, coordinates).toTourLegs(
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
            roadProvider.directions(plan.mode, coordinates).toTourLegs(
                points = coordinates,
                mode = plan.mode,
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
        var nextDeparture = departureTime
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
                OffsetDateTime.parse(journey.arrivalTime).plusMinutes(dwellMinutes.toLong()).toString()
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
                source = "openrouteservice / HeiGIT",
                destinationPointId = destinationPointIds.getOrNull(index),
            )
        }

    companion object {
        const val MAX_TRANSIT_POINTS = 8
    }
}

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
