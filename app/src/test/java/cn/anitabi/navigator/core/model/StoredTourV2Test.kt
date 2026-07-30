package cn.anitabi.navigator.core.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoredTourV2Test {
    private val json = Json { encodeDefaults = true }

    @Test
    fun `stored tour keeps user state but excludes resolved route content`() {
        val plan = legacyPlan()
        val progress = NavigationProgress(
            tourId = plan.id,
            legIndex = 1,
            stepIndex = 2,
            completedPointIds = setOf("101::a"),
            state = NavigationState.NAVIGATING,
            lastRerouteEpochMillis = 1234L,
        )

        val stored = StoredTourV2.from(plan, progress)
        val encoded = json.encodeToString(StoredTourV2.serializer(), stored)

        assertEquals(listOf(101L, 202L), stored.selectedAnimes.map(Anime::subjectId))
        assertEquals(listOf("101::a", "202::b"), stored.manualOrderPointIds)
        assertEquals(setOf("101::a"), stored.completedPointIds)
        assertEquals(NavigationState.NAVIGATING, stored.navigationState)
        assertTrue(stored.toUnresolvedPlan().legs.isEmpty())
        assertFalse(encoded.contains("geometry"))
        assertFalse(encoded.contains("steps"))
        assertFalse(encoded.contains("estimatedDurationSeconds"))
        assertFalse(encoded.contains("Google polyline"))
    }

    @Test
    fun `stored tour conversion is idempotent`() {
        val first = StoredTourV2.from(legacyPlan(), null)
        val decoded = json.decodeFromString(StoredTourV2.serializer(), json.encodeToString(first))

        assertEquals(first, decoded)
        assertEquals(first.manualOrderPointIds, decoded.toUnresolvedPlan().orderedPoints.map(PilgrimagePoint::id))
    }

    private fun legacyPlan(): TourPlan {
        val first = PilgrimagePoint("101::a", "《作品甲》· A", GeoPoint(35.0, 139.0))
        val second = PilgrimagePoint("202::b", "《作品乙》· B", GeoPoint(35.1, 139.1))
        return TourPlan(
            id = "legacy-tour",
            anime = Anime(0, "作品甲 + 作品乙", "2 部作品联合巡礼"),
            selectedPoints = listOf(first, second),
            orderedPoints = listOf(first, second),
            legs = listOf(
                TourLeg(
                    from = first.coordinate,
                    to = second.coordinate,
                    mode = TravelMode.WALK,
                    geometry = listOf(first.coordinate, GeoPoint(35.05, 139.05), second.coordinate),
                    steps = listOf(RouteStep("Google polyline", 10.0, 20.0)),
                    distanceMeters = 10.0,
                    durationSeconds = 20.0,
                    source = "legacy resolved route",
                    destinationPointId = second.id,
                ),
            ),
            mode = TravelMode.WALK,
            objective = RouteObjective.FASTEST,
            endPolicy = EndPolicy.OPEN,
            estimatedDurationSeconds = 20.0,
            attribution = listOf("legacy attribution"),
            initialStart = first.coordinate,
        )
    }
}
