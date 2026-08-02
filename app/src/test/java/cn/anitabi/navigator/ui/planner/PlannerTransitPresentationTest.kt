package cn.anitabi.navigator.ui.planner

import cn.anitabi.navigator.core.model.TransitExecutionStrategy
import cn.anitabi.navigator.core.model.TravelMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlannerTransitPresentationTest {
    @Test
    fun `Japan external transit shows only the Google Maps handoff presentation`() {
        val presentation = plannerTransitPresentation(
            mode = TravelMode.TRANSIT,
            executionStrategy = TransitExecutionStrategy.EXTERNAL_GOOGLE_MAPS_JAPAN,
        )

        assertTrue(presentation.showExternalProviderMessage)
        assertEquals(
            "路线、班次和换乘由 Google 地图提供",
            EXTERNAL_JAPAN_TRANSIT_PROVIDER_MESSAGE,
        )
        assertFalse(presentation.showTransitSchedule)
        assertFalse(presentation.showTransitFilters)
        assertFalse(presentation.showRouteEstimateSummary)
        assertFalse(presentation.showTransitJourneyDetails)
    }

    @Test
    fun `in-app transit keeps schedule filters estimates and route details`() {
        val presentation = plannerTransitPresentation(
            mode = TravelMode.TRANSIT,
            executionStrategy = TransitExecutionStrategy.IN_APP_GOOGLE_ROUTES,
        )

        assertFalse(presentation.showExternalProviderMessage)
        assertTrue(presentation.showTransitSchedule)
        assertTrue(presentation.showTransitFilters)
        assertTrue(presentation.showRouteEstimateSummary)
        assertTrue(presentation.showTransitJourneyDetails)
    }

    @Test
    fun `road modes keep their route estimate summary without transit controls`() {
        val presentation = plannerTransitPresentation(
            mode = TravelMode.WALK,
            executionStrategy = null,
        )

        assertFalse(presentation.showExternalProviderMessage)
        assertFalse(presentation.showTransitSchedule)
        assertFalse(presentation.showTransitFilters)
        assertTrue(presentation.showRouteEstimateSummary)
        assertFalse(presentation.showTransitJourneyDetails)
    }
}
