package cn.anitabi.navigator.core.navigation

import cn.anitabi.navigator.core.model.NavigationProgress
import cn.anitabi.navigator.core.model.NavigationState
import cn.anitabi.navigator.core.model.TourPlan
import cn.anitabi.navigator.core.model.TravelMode

object TransitRefreshPolicy {
    fun shouldRefresh(
        plan: TourPlan,
        progress: NavigationProgress,
        targetPointId: String?,
    ): Boolean {
        if (plan.mode != TravelMode.TRANSIT) return false
        val currentLeg = plan.legs.getOrNull(progress.legIndex)
        val completedPilgrimageStop = progress.state == NavigationState.NEXT_STOP && targetPointId != null
        val cancelledLeg = progress.state == NavigationState.NAVIGATING && currentLeg?.transit?.cancelled == true
        return completedPilgrimageStop || cancelledLeg
    }
}
