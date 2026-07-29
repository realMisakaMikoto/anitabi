package cn.anitabi.navigator.core.navigation

import cn.anitabi.navigator.core.model.NavigationProgress
import cn.anitabi.navigator.core.model.NavigationState
import cn.anitabi.navigator.core.model.TourPlan
import cn.anitabi.navigator.core.model.TravelMode
import java.time.OffsetDateTime

object TransitRefreshPolicy {
    const val MISSED_CONNECTION_GRACE_MILLIS = 2 * 60_000L

    fun shouldRefresh(
        plan: TourPlan,
        progress: NavigationProgress,
        targetPointId: String?,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): Boolean {
        if (plan.mode != TravelMode.TRANSIT) return false
        val currentLeg = plan.legs.getOrNull(progress.legIndex)
        val completedPilgrimageStop = progress.state == NavigationState.NEXT_STOP && targetPointId != null
        val cancelledLeg = progress.state == NavigationState.NAVIGATING && currentLeg?.transit?.cancelled == true
        val nextTransit = plan.legs.getOrNull(progress.legIndex + 1)?.transit
        val missedConnection = progress.state == NavigationState.NEXT_STOP &&
            targetPointId == null &&
            nextTransit?.vehicleMode != "WALK" &&
            nextTransit?.departureTime
                ?.let(::parseEpochMillisOrNull)
                ?.let { departure -> nowEpochMillis - departure > MISSED_CONNECTION_GRACE_MILLIS } == true
        return completedPilgrimageStop || cancelledLeg || missedConnection
    }

    private fun parseEpochMillisOrNull(value: String): Long? = runCatching {
        OffsetDateTime.parse(value).toInstant().toEpochMilli()
    }.getOrNull()
}
