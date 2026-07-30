package cn.anitabi.navigator.core.navigation

import cn.anitabi.navigator.core.model.Anime
import cn.anitabi.navigator.core.model.EndPolicy
import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.NavigationProgress
import cn.anitabi.navigator.core.model.NavigationState
import cn.anitabi.navigator.core.model.PilgrimagePoint
import cn.anitabi.navigator.core.model.RouteObjective
import cn.anitabi.navigator.core.model.TourLeg
import cn.anitabi.navigator.core.model.TourPlan
import cn.anitabi.navigator.core.model.TransitLegDetails
import cn.anitabi.navigator.core.model.TravelMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransitRefreshPolicyTest {
    @Test
    fun `completed pilgrimage stop refreshes remaining transit itinerary`() {
        val plan = transitPlan(cancelled = false)
        val progress = NavigationProgress(
            tourId = plan.id,
            state = NavigationState.NEXT_STOP,
            completedPointIds = setOf("stop"),
        )

        assertTrue(TransitRefreshPolicy.shouldRefresh(plan, progress, targetPointId = "stop"))
    }

    @Test
    fun `internal transfer and road legs do not refresh at next stop state`() {
        val transit = transitPlan(cancelled = false)
        val progress = NavigationProgress(tourId = transit.id, state = NavigationState.NEXT_STOP)

        assertFalse(TransitRefreshPolicy.shouldRefresh(transit, progress, targetPointId = null))
        assertFalse(
            TransitRefreshPolicy.shouldRefresh(
                transit.copy(mode = TravelMode.WALK),
                progress,
                targetPointId = "stop",
            ),
        )
    }

    @Test
    fun `cancelled current transit leg refreshes immediately`() {
        val plan = transitPlan(cancelled = true)
        val progress = NavigationProgress(tourId = plan.id, state = NavigationState.NAVIGATING)

        assertTrue(TransitRefreshPolicy.shouldRefresh(plan, progress, targetPointId = null))
    }

    @Test
    fun `missed next departure after an internal transfer refreshes automatically`() {
        val departure = java.time.OffsetDateTime.parse("2026-07-29T09:10:00+09:00").toInstant().toEpochMilli()
        val plan = twoLegTransitPlan(nextDeparture = "2026-07-29T09:10:00+09:00")
        val progress = NavigationProgress(
            tourId = plan.id,
            legIndex = 0,
            state = NavigationState.NEXT_STOP,
        )

        assertTrue(
            TransitRefreshPolicy.shouldRefresh(
                plan,
                progress,
                targetPointId = null,
                nowEpochMillis = departure + TransitRefreshPolicy.MISSED_CONNECTION_GRACE_MILLIS + 1,
            ),
        )
    }

    @Test
    fun `connection inside grace period does not refresh`() {
        val departure = java.time.OffsetDateTime.parse("2026-07-29T09:10:00+09:00").toInstant().toEpochMilli()
        val plan = twoLegTransitPlan(nextDeparture = "2026-07-29T09:10:00+09:00")
        val progress = NavigationProgress(
            tourId = plan.id,
            legIndex = 0,
            state = NavigationState.NEXT_STOP,
        )

        assertFalse(
            TransitRefreshPolicy.shouldRefresh(
                plan,
                progress,
                targetPointId = null,
                nowEpochMillis = departure + TransitRefreshPolicy.MISSED_CONNECTION_GRACE_MILLIS,
            ),
        )
    }

    private fun transitPlan(cancelled: Boolean): TourPlan {
        val from = GeoPoint(35.0, 139.0)
        val to = GeoPoint(35.1, 139.1)
        val point = PilgrimagePoint("stop", "Stop", to)
        return TourPlan(
            id = "tour",
            anime = Anime(1, "Test"),
            selectedPoints = listOf(point),
            orderedPoints = listOf(point),
            legs = listOf(
                TourLeg(
                    from = from,
                    to = to,
                    mode = TravelMode.TRANSIT,
                    geometry = listOf(from, to),
                    steps = emptyList(),
                    distanceMeters = 1_000.0,
                    durationSeconds = 600.0,
                    source = "Google Routes API",
                    transit = TransitLegDetails(vehicleMode = "TRAIN", cancelled = cancelled),
                    destinationPointId = "stop",
                ),
            ),
            mode = TravelMode.TRANSIT,
            objective = RouteObjective.FASTEST,
            endPolicy = EndPolicy.OPEN,
            estimatedDurationSeconds = 600.0,
            attribution = emptyList(),
        )
    }

    private fun twoLegTransitPlan(nextDeparture: String): TourPlan {
        val plan = transitPlan(cancelled = false)
        val transfer = GeoPoint(35.05, 139.05)
        val first = plan.legs.single().copy(
            to = transfer,
            geometry = listOf(plan.legs.single().from, transfer),
            transit = plan.legs.single().transit?.copy(arrivalTime = "2026-07-29T09:05:00+09:00"),
            destinationPointId = null,
        )
        val second = plan.legs.single().copy(
            from = transfer,
            geometry = listOf(transfer, plan.legs.single().to),
            transit = plan.legs.single().transit?.copy(departureTime = nextDeparture),
        )
        return plan.copy(legs = listOf(first, second))
    }
}
