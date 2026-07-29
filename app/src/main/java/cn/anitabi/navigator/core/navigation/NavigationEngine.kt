package cn.anitabi.navigator.core.navigation

import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.NavigationProgress
import cn.anitabi.navigator.core.model.NavigationState
import cn.anitabi.navigator.core.model.TourLeg
import cn.anitabi.navigator.core.model.TourPlan
import cn.anitabi.navigator.core.model.TravelMode
import cn.anitabi.navigator.core.routing.TourOptimizer
import java.time.OffsetDateTime
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class NavigationEngine(
    private val plan: TourPlan,
    initialProgress: NavigationProgress = NavigationProgress(tourId = plan.id),
) {
    var progress: NavigationProgress = initialProgress
        private set

    fun start(): NavigationUpdate {
        require(progress.state == NavigationState.PLANNED) { "Only a planned tour can be started" }
        progress = progress.copy(state = NavigationState.NAVIGATING)
        return snapshot(location = null)
    }

    fun onLocation(location: GeoPoint, nowEpochMillis: Long): NavigationUpdate {
        if (progress.state != NavigationState.NAVIGATING) return snapshot(location)
        val leg = currentLeg() ?: return complete(location)
        progress = progress.copy(stepIndex = advancedStepIndex(location, leg, progress.stepIndex))
        val distanceToDestination = TourOptimizer.haversineMeters(location, leg.to)
        val offRouteDistance = distanceToGeometryMeters(location, leg.geometry.ifEmpty { listOf(leg.from, leg.to) })
        var requestReroute = false

        progress = if (plan.mode == TravelMode.TRANSIT || offRouteDistance <= plan.mode.offRouteThresholdMeters()) {
            progress.copy(offRouteSinceEpochMillis = null)
        } else {
            val offRouteSince = progress.offRouteSinceEpochMillis ?: nowEpochMillis
            val sustained = nowEpochMillis - offRouteSince >= OFF_ROUTE_CONFIRMATION_MILLIS
            val cooledDown = progress.lastRerouteEpochMillis?.let {
                nowEpochMillis - it >= REROUTE_COOLDOWN_MILLIS
            } ?: true
            if (sustained && cooledDown) {
                requestReroute = true
                progress.copy(
                    offRouteSinceEpochMillis = offRouteSince,
                    lastRerouteEpochMillis = nowEpochMillis,
                )
            } else {
                progress.copy(offRouteSinceEpochMillis = offRouteSince)
            }
        }

        if (distanceToDestination <= ARRIVAL_RADIUS_METERS) {
            progress = progress.copy(state = NavigationState.ARRIVING, offRouteSinceEpochMillis = null)
        }
        return snapshot(location, requestReroute)
    }

    fun onTick(nowEpochMillis: Long): NavigationUpdate {
        val leg = currentLeg()
        progress = when (progress.state) {
            NavigationState.NAVIGATING -> {
                val scheduledArrival = leg?.transit?.arrivalTime?.let(::parseEpochMillisOrNull)
                if (scheduledArrival != null && nowEpochMillis >= scheduledArrival) {
                    progress.copy(state = NavigationState.ARRIVING)
                } else {
                    progress
                }
            }
            NavigationState.ARRIVING -> {
                val pointId = leg?.destinationPointId
                if (pointId == null) {
                    progress.copy(state = NavigationState.NEXT_STOP)
                } else {
                    progress.copy(
                        state = NavigationState.DWELLING,
                        completedPointIds = progress.completedPointIds + pointId,
                        dwellingUntilEpochMillis = nowEpochMillis + plan.dwellMinutes * 60_000L,
                    )
                }
            }
            NavigationState.DWELLING -> {
                if (nowEpochMillis >= (progress.dwellingUntilEpochMillis ?: nowEpochMillis)) {
                    progress.copy(state = NavigationState.NEXT_STOP, dwellingUntilEpochMillis = null)
                } else {
                    progress
                }
            }
            NavigationState.NEXT_STOP -> {
                val nextLegIndex = progress.legIndex + 1
                if (nextLegIndex >= plan.legs.size) {
                    progress.copy(state = NavigationState.COMPLETED, legIndex = plan.legs.lastIndex.coerceAtLeast(0))
                } else {
                    progress.copy(
                        state = NavigationState.NAVIGATING,
                        legIndex = nextLegIndex,
                        stepIndex = 0,
                    )
                }
            }
            else -> progress
        }
        return snapshot(location = null)
    }

    fun manualArrival(): NavigationUpdate {
        if (progress.state == NavigationState.NAVIGATING) {
            progress = progress.copy(state = NavigationState.ARRIVING, offRouteSinceEpochMillis = null)
        }
        return snapshot(location = null)
    }

    private fun currentLeg(): TourLeg? = plan.legs.getOrNull(progress.legIndex)

    private fun advancedStepIndex(location: GeoPoint, leg: TourLeg, currentIndex: Int): Int {
        var index = currentIndex.coerceIn(0, leg.steps.lastIndex.coerceAtLeast(0))
        while (index < leg.steps.lastIndex) {
            val nextCoordinate = leg.steps[index + 1].coordinate ?: break
            if (TourOptimizer.haversineMeters(location, nextCoordinate) > STEP_ADVANCE_RADIUS_METERS) break
            index += 1
        }
        return index
    }

    private fun complete(location: GeoPoint?): NavigationUpdate {
        progress = progress.copy(state = NavigationState.COMPLETED)
        return snapshot(location)
    }

    private fun snapshot(location: GeoPoint?, requestReroute: Boolean = false): NavigationUpdate {
        val leg = currentLeg()
        val remaining = if (leg == null) {
            0.0
        } else {
            val currentLegDistance = location?.let { TourOptimizer.haversineMeters(it, leg.to) }
                ?: leg.distanceMeters
            currentLegDistance + plan.legs.drop(progress.legIndex + 1).sumOf(TourLeg::distanceMeters)
        }
        return NavigationUpdate(
            progress = progress,
            currentLocation = location,
            target = leg?.to,
            targetPointId = leg?.destinationPointId,
            remainingDistanceMeters = remaining,
            instruction = leg?.steps?.getOrNull(progress.stepIndex)?.instruction
                ?: leg?.transit?.let { transit ->
                    buildString {
                        append(transit.line ?: transit.vehicleMode)
                        transit.direction?.let { append(" · 开往 $it") }
                    }
                }
                ?: "继续前往下一站",
            requestReroute = requestReroute,
        )
    }

    companion object {
        const val ARRIVAL_RADIUS_METERS = 35.0
        const val STEP_ADVANCE_RADIUS_METERS = 30.0
        const val OFF_ROUTE_CONFIRMATION_MILLIS = 15_000L
        const val REROUTE_COOLDOWN_MILLIS = 60_000L

        private fun parseEpochMillisOrNull(value: String): Long? = runCatching {
            OffsetDateTime.parse(value).toInstant().toEpochMilli()
        }.getOrNull()
    }
}

data class NavigationUpdate(
    val progress: NavigationProgress,
    val currentLocation: GeoPoint?,
    val target: GeoPoint?,
    val targetPointId: String?,
    val remainingDistanceMeters: Double,
    val instruction: String,
    val requestReroute: Boolean,
)

private fun TravelMode.offRouteThresholdMeters(): Double = when (this) {
    TravelMode.DRIVE -> 100.0
    TravelMode.BIKE, TravelMode.WALK -> 60.0
    TravelMode.TRANSIT -> Double.POSITIVE_INFINITY
}

private fun distanceToGeometryMeters(point: GeoPoint, geometry: List<GeoPoint>): Double {
    if (geometry.isEmpty()) return Double.POSITIVE_INFINITY
    if (geometry.size == 1) return TourOptimizer.haversineMeters(point, geometry.single())
    return geometry.zipWithNext().minOf { (start, end) -> distanceToSegmentMeters(point, start, end) }
}

private fun distanceToSegmentMeters(point: GeoPoint, start: GeoPoint, end: GeoPoint): Double {
    val referenceLatitude = Math.toRadians(point.latitude)
    fun GeoPoint.localX(): Double = Math.toRadians(longitude - point.longitude) * cos(referenceLatitude) * EARTH_RADIUS
    fun GeoPoint.localY(): Double = Math.toRadians(latitude - point.latitude) * EARTH_RADIUS
    val startX = start.localX()
    val startY = start.localY()
    val endX = end.localX()
    val endY = end.localY()
    val deltaX = endX - startX
    val deltaY = endY - startY
    val lengthSquared = deltaX * deltaX + deltaY * deltaY
    if (lengthSquared == 0.0) return sqrt(startX * startX + startY * startY)
    val projection = max(0.0, min(1.0, -(startX * deltaX + startY * deltaY) / lengthSquared))
    val nearestX = startX + projection * deltaX
    val nearestY = startY + projection * deltaY
    return sqrt(nearestX * nearestX + nearestY * nearestY)
}

private const val EARTH_RADIUS = 6_371_000.0
