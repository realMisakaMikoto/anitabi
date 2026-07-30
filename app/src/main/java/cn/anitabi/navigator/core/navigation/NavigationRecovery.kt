package cn.anitabi.navigator.core.navigation

import cn.anitabi.navigator.core.model.NavigationProgress
import cn.anitabi.navigator.core.model.NavigationState

fun NavigationProgress.afterRouteRefresh(hasRemainingLegs: Boolean): NavigationProgress = copy(
    legIndex = when (state) {
        NavigationState.DWELLING, NavigationState.NEXT_STOP -> -1
        else -> 0
    },
    stepIndex = 0,
    state = if (hasRemainingLegs) state else NavigationState.COMPLETED,
    offRouteSinceEpochMillis = null,
)
