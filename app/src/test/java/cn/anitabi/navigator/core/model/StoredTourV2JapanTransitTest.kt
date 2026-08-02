package cn.anitabi.navigator.core.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StoredTourV2JapanTransitTest {
    private val json = Json { explicitNulls = false }

    @Test
    fun `active leg and paused state survive storage while legacy fields retain defaults`() {
        val plan = externalTransitPlan()
        val progress = NavigationProgress(
            tourId = plan.id,
            legIndex = 1,
            completedPointIds = setOf("first"),
            state = NavigationState.NAVIGATING,
            dwellingUntilEpochMillis = 20_000L,
            isPaused = true,
            pausedAtEpochMillis = 15_000L,
        )

        val stored = StoredTourV2.from(plan, progress)
        val encoded = json.encodeToString(StoredTourV2.serializer(), stored)
        val decoded = json.decodeFromString(StoredTourV2.serializer(), encoded)
        val restored = decoded.toNavigationProgress()

        assertEquals(1, stored.activeLegIndex)
        assertEquals("second", stored.activePointId)
        assertTrue(stored.isPaused)
        assertEquals(15_000L, stored.pausedAtEpochMillis)
        assertEquals(1, restored.legIndex)
        assertEquals(NavigationState.NAVIGATING, restored.state)
        assertEquals(setOf("first"), restored.completedPointIds)
        assertEquals(20_000L, restored.dwellingUntilEpochMillis)
        assertTrue(restored.isPaused)
        assertEquals(15_000L, restored.pausedAtEpochMillis)
        assertEquals(
            TransitExecutionStrategy.EXTERNAL_GOOGLE_MAPS_JAPAN,
            decoded.toUnresolvedPlan().executionStrategy,
        )

        val legacyFields = json.parseToJsonElement(encoded).jsonObject.toMutableMap().apply {
            remove("activeLegIndex")
            remove("isPaused")
            remove("pausedAtEpochMillis")
        }
        val legacyDecoded = json.decodeFromString(
            StoredTourV2.serializer(),
            JsonObject(legacyFields).toString(),
        )
        val legacyProgress = legacyDecoded.toNavigationProgress()

        assertEquals(1, legacyProgress.legIndex)
        assertFalse(legacyProgress.isPaused)
        assertNull(legacyProgress.pausedAtEpochMillis)
    }

    @Test
    fun `legacy external selected start maps active point against complete ordered points`() {
        val points = listOf(
            point("first", 35.6762, 139.6503),
            point("second", 35.6895, 139.6917),
            point("third", 35.7101, 139.8107),
        )
        val stored = legacyStoredTour(
            points = points,
            activePointId = points[1].id,
        )

        assertEquals(
            1,
            stored.toNavigationProgress(
                TransitExecutionStrategy.EXTERNAL_GOOGLE_MAPS_JAPAN,
            ).legIndex,
        )
        assertEquals(
            0,
            stored.toNavigationProgress(
                TransitExecutionStrategy.IN_APP_GOOGLE_ROUTES,
            ).legIndex,
        )
    }

    @Test
    fun `legacy external completed return tour restores return to start leg`() {
        val points = listOf(
            point("first", 35.6762, 139.6503),
            point("second", 35.6895, 139.6917),
            point("third", 35.7101, 139.8107),
        )
        val stored = legacyStoredTour(
            points = points,
            activePointId = null,
            completedPointIds = points.map(PilgrimagePoint::id).toSet(),
            endPolicy = EndPolicy.RETURN_TO_START,
        )

        assertEquals(
            points.size,
            stored.toNavigationProgress(
                TransitExecutionStrategy.EXTERNAL_GOOGLE_MAPS_JAPAN,
            ).legIndex,
        )
    }

    private fun legacyStoredTour(
        points: List<PilgrimagePoint>,
        activePointId: String?,
        completedPointIds: Set<String> = emptySet(),
        endPolicy: EndPolicy = EndPolicy.OPEN,
    ) = StoredTourV2(
        id = "legacy-external",
        displayAnime = Anime(1, "Japan"),
        selectedAnimes = listOf(Anime(1, "Japan")),
        selectedPoints = points,
        manualOrderPointIds = points.map(PilgrimagePoint::id),
        start = points.first().coordinate,
        startPointId = points.first().id,
        mode = TravelMode.TRANSIT,
        objective = RouteObjective.FASTEST,
        endPolicy = endPolicy,
        completedPointIds = completedPointIds,
        activePointId = activePointId,
        activeLegIndex = null,
        navigationState = NavigationState.NAVIGATING,
        executionStrategy = null,
    )

    private fun externalTransitPlan(): TourPlan {
        val first = point("first", 35.6762, 139.6503)
        val second = point("second", 35.6895, 139.6917)
        val third = point("third", 35.7101, 139.8107)
        return TourPlan(
            id = "external-japan-tour",
            anime = Anime(1, "Japan"),
            selectedPoints = listOf(first, second, third),
            orderedPoints = listOf(first, second, third),
            legs = listOf(
                leg(first, first),
                leg(first, second),
                leg(second, third),
            ),
            mode = TravelMode.TRANSIT,
            objective = RouteObjective.FASTEST,
            endPolicy = EndPolicy.OPEN,
            estimatedDurationSeconds = 0.0,
            attribution = listOf("Google Maps"),
            transitTimeMode = TransitTimeMode.NOW,
            initialStart = first.coordinate,
            executionStrategy = TransitExecutionStrategy.EXTERNAL_GOOGLE_MAPS_JAPAN,
        )
    }

    private fun leg(from: PilgrimagePoint, to: PilgrimagePoint) = TourLeg(
        from = from.coordinate,
        to = to.coordinate,
        mode = TravelMode.TRANSIT,
        geometry = emptyList(),
        steps = emptyList(),
        distanceMeters = 100.0,
        durationSeconds = 0.0,
        source = "Google Maps",
        destinationPointId = to.id,
    )

    private fun point(id: String, latitude: Double, longitude: Double) = PilgrimagePoint(
        id = id,
        name = id,
        coordinate = GeoPoint(latitude, longitude),
    )
}
