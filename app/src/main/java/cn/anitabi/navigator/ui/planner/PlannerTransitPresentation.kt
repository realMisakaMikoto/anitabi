package cn.anitabi.navigator.ui.planner

import cn.anitabi.navigator.core.model.TransitExecutionStrategy
import cn.anitabi.navigator.core.model.TravelMode

internal const val EXTERNAL_JAPAN_TRANSIT_PROVIDER_MESSAGE =
    "路线、班次和换乘由 Google 地图提供"

internal data class PlannerTransitPresentation(
    val showExternalProviderMessage: Boolean,
    val showTransitSchedule: Boolean,
    val showTransitFilters: Boolean,
    val showRouteEstimateSummary: Boolean,
    val showTransitJourneyDetails: Boolean,
)

internal fun plannerTransitPresentation(
    mode: TravelMode,
    executionStrategy: TransitExecutionStrategy?,
): PlannerTransitPresentation {
    val externalJapanTransit = mode == TravelMode.TRANSIT &&
        executionStrategy == TransitExecutionStrategy.EXTERNAL_GOOGLE_MAPS_JAPAN
    val inAppTransit = mode == TravelMode.TRANSIT && !externalJapanTransit

    return PlannerTransitPresentation(
        showExternalProviderMessage = externalJapanTransit,
        showTransitSchedule = inAppTransit,
        showTransitFilters = inAppTransit,
        showRouteEstimateSummary = !externalJapanTransit,
        showTransitJourneyDetails = inAppTransit,
    )
}
