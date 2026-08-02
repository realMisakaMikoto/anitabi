package cn.anitabi.navigator.core.routing

import cn.anitabi.navigator.core.model.EndPolicy
import cn.anitabi.navigator.core.model.NavigationProgress
import cn.anitabi.navigator.core.model.NavigationState
import cn.anitabi.navigator.core.model.PilgrimagePoint
import cn.anitabi.navigator.core.model.TourLeg
import cn.anitabi.navigator.core.model.TourPlan
import cn.anitabi.navigator.core.model.TransitExecutionStrategy
import cn.anitabi.navigator.core.model.TransitTimeMode
import cn.anitabi.navigator.core.model.TravelMode
import java.time.Instant
import java.time.OffsetDateTime
import kotlin.math.roundToLong

data class ActiveTourEditResult(
    val plan: TourPlan,
    val progress: NavigationProgress,
)

class ActiveTourEditException(message: String) : IllegalArgumentException(message)

/**
 * Builds an updated active tour without persisting it. [orderedFuturePoints] contains only the
 * visits after the current target, so callers can save [ActiveTourEditResult] atomically after
 * this function has completed successfully.
 */
suspend fun editActiveTourFuture(
    currentPlan: TourPlan,
    currentProgress: NavigationProgress,
    orderedFuturePoints: List<PilgrimagePoint>,
    planner: TourPlanner,
    transitAnchorTime: String = formatTransitDepartureTime(OffsetDateTime.now()),
): ActiveTourEditResult {
    if (currentProgress.tourId != currentPlan.id) {
        throw ActiveTourEditException("Navigation progress belongs to a different tour")
    }
    if (currentProgress.state in TERMINAL_OR_INACTIVE_STATES) {
        throw ActiveTourEditException("Only an active tour can be edited")
    }
    val recoveredBetweenLegs = currentProgress.legIndex == -1 &&
        currentProgress.state in setOf(NavigationState.DWELLING, NavigationState.NEXT_STOP)
    if (!recoveredBetweenLegs && currentProgress.legIndex !in currentPlan.legs.indices) {
        throw ActiveTourEditException("Active leg is outside the current tour")
    }
    requireUniquePointIds(orderedFuturePoints)

    val currentDestinationLegIndex = if (recoveredBetweenLegs) {
        -1
    } else {
        currentPlan.legs.indices
            .drop(currentProgress.legIndex)
            .firstOrNull { currentPlan.legs[it].destinationPointId != null }
    }
    if (currentDestinationLegIndex == null) {
        return editWhileReturningToStart(
            currentPlan = currentPlan,
            currentProgress = currentProgress,
            orderedFuturePoints = orderedFuturePoints,
            planner = planner,
        )
    }

    val currentPointIndex = if (recoveredBetweenLegs) {
        -1
    } else {
        val currentPointId = requireNotNull(
            currentPlan.legs[currentDestinationLegIndex].destinationPointId,
        )
        currentPlan.orderedPoints.indexOfFirst { it.id == currentPointId }.also { index ->
            if (index < 0) throw ActiveTourEditException("Current target is missing from the visit order")
        }
    }
    val lockedOrderedPoints = if (recoveredBetweenLegs) {
        emptyList()
    } else {
        currentPlan.orderedPoints.take(currentPointIndex + 1)
    }
    val originalFuturePoints = currentPlan.orderedPoints.drop(currentPointIndex + 1)
    val lockedPointIds = currentProgress.completedPointIds + lockedOrderedPoints.map(PilgrimagePoint::id)
    validateFixedEndPoint(currentPlan, orderedFuturePoints, lockedPointIds)
    validateLockedAndFuturePoints(
        currentPlan = currentPlan,
        orderedFuturePoints = orderedFuturePoints,
        lockedPointIds = lockedPointIds,
        originalFuturePoints = originalFuturePoints,
    )

    val lockedSelectedPoints = currentPlan.selectedPoints.filter { it.id in lockedPointIds }
    if (!lockedPointIds.all { lockedId -> lockedSelectedPoints.any { it.id == lockedId } }) {
        throw ActiveTourEditException("Locked points are missing from the selected tour")
    }
    val selectedPoints = lockedSelectedPoints + orderedFuturePoints
    requireUniquePointIds(selectedPoints)
    val executionStrategy = validatedActiveExecutionStrategy(currentPlan, selectedPoints, planner)

    val workingPlan = currentPlan.copy(
        selectedPoints = selectedPoints,
        orderedPoints = lockedOrderedPoints + orderedFuturePoints,
        executionStrategy = executionStrategy,
    )
    if (orderedFuturePoints == originalFuturePoints) {
        return ActiveTourEditResult(plan = currentPlan, progress = currentProgress)
    }

    val unchangedFutureCount = originalFuturePoints.zip(orderedFuturePoints)
        .takeWhile { (original, submitted) -> original == submitted }
        .size
    val unchangedFuturePoints = orderedFuturePoints.take(unchangedFutureCount)
    val preservedThroughLegIndex = findPreservedDestinationLegIndex(
        plan = currentPlan,
        currentDestinationLegIndex = currentDestinationLegIndex,
        unchangedFuturePoints = unchangedFuturePoints,
    )
    val suffixStart = unchangedFuturePoints.lastOrNull()?.coordinate
        ?: if (recoveredBetweenLegs) {
            currentPlan.legs.firstOrNull()?.from
                ?: currentPlan.initialStart
                ?: throw ActiveTourEditException("The remaining route has no start")
        } else {
            currentPlan.orderedPoints[currentPointIndex].coordinate
        }
    val suffixTransitAnchorTime = if (
        currentPlan.mode == TravelMode.TRANSIT &&
        currentPlan.executionStrategy == TransitExecutionStrategy.IN_APP_GOOGLE_ROUTES
    ) {
        activeTransitSuffixAnchor(
            plan = currentPlan,
            progress = currentProgress,
            currentDestinationLegIndex = currentDestinationLegIndex,
            preservedThroughLegIndex = preservedThroughLegIndex,
            fallbackAnchorTime = transitAnchorTime,
        )
    } else {
        transitAnchorTime
    }
    val suffixFuturePoints = orderedFuturePoints.drop(unchangedFutureCount)
    val suffix = planner.rebuildActiveFutureSuffix(
        plan = workingPlan,
        start = suffixStart,
        orderedFuturePoints = suffixFuturePoints,
        transitAnchorTime = suffixTransitAnchorTime,
        reusableRoadLegs = currentPlan.legs.drop(preservedThroughLegIndex + 1),
    )
    val preservedLegs = currentPlan.legs.take(preservedThroughLegIndex + 1)
    val rebuiltLegs = preservedLegs + suffix.legs
    val preservedDwellSeconds = if (
        currentPlan.mode == TravelMode.TRANSIT &&
        currentPlan.executionStrategy == TransitExecutionStrategy.IN_APP_GOOGLE_ROUTES
    ) {
        preservedLegs.count { it.destinationPointId != null } * currentPlan.dwellMinutes * 60.0
    } else {
        0.0
    }
    val rebuiltPlan = workingPlan.copy(
        id = currentPlan.id,
        legs = rebuiltLegs,
        estimatedDurationSeconds = preservedLegs.sumOf(TourLeg::durationSeconds) + preservedDwellSeconds +
            suffix.estimatedDurationSeconds,
        departureTime = currentPlan.departureTime,
        arrivalTime = suffix.arrivalTime,
        transitTimeMode = if (
            currentPlan.mode == TravelMode.TRANSIT &&
            currentPlan.executionStrategy == TransitExecutionStrategy.IN_APP_GOOGLE_ROUTES
        ) {
            TransitTimeMode.NOW
        } else {
            currentPlan.transitTimeMode
        },
        transitAnchorTime = if (
            currentPlan.mode == TravelMode.TRANSIT &&
            currentPlan.executionStrategy == TransitExecutionStrategy.IN_APP_GOOGLE_ROUTES
        ) {
            null
        } else {
            currentPlan.transitAnchorTime
        },
    )
    return ActiveTourEditResult(plan = rebuiltPlan, progress = currentProgress)
}

private fun activeTransitSuffixAnchor(
    plan: TourPlan,
    progress: NavigationProgress,
    currentDestinationLegIndex: Int,
    preservedThroughLegIndex: Int,
    fallbackAnchorTime: String,
): String {
    val fallback = OffsetDateTime.parse(fallbackAnchorTime)
    val base = if (progress.state == NavigationState.DWELLING) {
        progress.dwellingUntilEpochMillis?.let { deadline ->
            Instant.ofEpochMilli(deadline).atOffset(fallback.offset).takeIf { it.isAfter(fallback) }
        } ?: fallback
    } else {
        fallback
    }
    val firstUnaccountedLeg = when (progress.state) {
        NavigationState.DWELLING, NavigationState.NEXT_STOP -> currentDestinationLegIndex + 1
        else -> progress.legIndex.coerceAtLeast(0)
    }
    if (firstUnaccountedLeg > preservedThroughLegIndex) return formatTransitDepartureTime(base)
    val accountedLegs = plan.legs.subList(
        firstUnaccountedLeg.coerceAtLeast(0),
        (preservedThroughLegIndex + 1).coerceAtMost(plan.legs.size),
    )
    val travelNanos = (accountedLegs.sumOf(TourLeg::durationSeconds) * NANOS_PER_SECOND).roundToLong()
    val dwellMinutes = accountedLegs.count { it.destinationPointId != null } * plan.dwellMinutes.toLong()
    return formatTransitDepartureTime(base.plusNanos(travelNanos).plusMinutes(dwellMinutes))
}

private fun validateFixedEndPoint(
    currentPlan: TourPlan,
    orderedFuturePoints: List<PilgrimagePoint>,
    lockedPointIds: Set<String>,
) {
    if (currentPlan.endPolicy != EndPolicy.FIXED) return
    val fixedEndPoint = currentPlan.orderedPoints.lastOrNull()
        ?: throw ActiveTourEditException("The fixed end point is missing")
    if (fixedEndPoint.id in lockedPointIds) {
        if (orderedFuturePoints.isNotEmpty()) {
            throw ActiveTourEditException("No point can be inserted after the fixed end point")
        }
    } else if (orderedFuturePoints.lastOrNull()?.id != fixedEndPoint.id) {
        throw ActiveTourEditException("The fixed end point must remain last")
    }
}

private fun findPreservedDestinationLegIndex(
    plan: TourPlan,
    currentDestinationLegIndex: Int,
    unchangedFuturePoints: List<PilgrimagePoint>,
): Int {
    var cursor = currentDestinationLegIndex
    unchangedFuturePoints.forEach { point ->
        val nextIndex = (cursor + 1..plan.legs.lastIndex)
            .firstOrNull { plan.legs[it].destinationPointId == point.id }
            ?: throw ActiveTourEditException("An unchanged future point is missing from the route")
        cursor = nextIndex
    }
    return cursor
}

private fun editWhileReturningToStart(
    currentPlan: TourPlan,
    currentProgress: NavigationProgress,
    orderedFuturePoints: List<PilgrimagePoint>,
    planner: TourPlanner,
): ActiveTourEditResult {
    if (currentPlan.endPolicy != EndPolicy.RETURN_TO_START) {
        throw ActiveTourEditException("The active leg has no pilgrimage target")
    }
    if (orderedFuturePoints.isNotEmpty()) {
        throw ActiveTourEditException("No future points can be added during the return leg")
    }
    validatedActiveExecutionStrategy(currentPlan, currentPlan.selectedPoints, planner)
    return ActiveTourEditResult(plan = currentPlan, progress = currentProgress)
}

private fun validatedActiveExecutionStrategy(
    currentPlan: TourPlan,
    selectedPoints: List<PilgrimagePoint>,
    planner: TourPlanner,
): TransitExecutionStrategy {
    if (currentPlan.mode != TravelMode.TRANSIT) return currentPlan.executionStrategy
    return planner.transitExecutionStrategy(selectedPoints).also { executionStrategy ->
        if (executionStrategy != currentPlan.executionStrategy) {
            throw ActiveTourEditException("An active transit tour cannot change execution strategy")
        }
    }
}

private fun validateLockedAndFuturePoints(
    currentPlan: TourPlan,
    orderedFuturePoints: List<PilgrimagePoint>,
    lockedPointIds: Set<String>,
    originalFuturePoints: List<PilgrimagePoint>,
) {
    if (orderedFuturePoints.any { it.id in lockedPointIds }) {
        throw ActiveTourEditException("Completed points and the current target are locked")
    }
    val originalFutureById = originalFuturePoints.associateBy(PilgrimagePoint::id)
    orderedFuturePoints.forEach { submitted ->
        val original = originalFutureById[submitted.id] ?: return@forEach
        if (submitted != original) {
            throw ActiveTourEditException("Existing future points can only be inserted, deleted, or reordered")
        }
    }
    val knownPointIds = currentPlan.selectedPoints.mapTo(mutableSetOf(), PilgrimagePoint::id)
    if (
        orderedFuturePoints.any { submitted ->
            submitted.id in knownPointIds && submitted.id !in originalFutureById
        }
    ) {
        throw ActiveTourEditException("A locked point cannot be reinserted as a future point")
    }
}

private fun requireUniquePointIds(points: List<PilgrimagePoint>) {
    if (points.map(PilgrimagePoint::id).toSet().size != points.size) {
        throw ActiveTourEditException("Future point IDs must be unique")
    }
}

private val TERMINAL_OR_INACTIVE_STATES = setOf(
    NavigationState.PLANNED,
    NavigationState.COMPLETED,
    NavigationState.ENDED,
)

private const val NANOS_PER_SECOND = 1_000_000_000.0
