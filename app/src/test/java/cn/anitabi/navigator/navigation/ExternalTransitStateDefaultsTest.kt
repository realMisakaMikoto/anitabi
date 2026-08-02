package cn.anitabi.navigator.navigation

import cn.anitabi.navigator.core.model.Anime
import cn.anitabi.navigator.core.model.EndPolicy
import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.PilgrimagePoint
import cn.anitabi.navigator.core.model.RouteObjective
import cn.anitabi.navigator.core.model.StoredTourV2
import cn.anitabi.navigator.core.model.TravelMode
import cn.anitabi.navigator.core.navigation.JapanExternalTransitRuntimeState
import cn.anitabi.navigator.ui.navigation.externalTransitNeedsResume
import kotlinx.serialization.json.Json
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class ExternalTransitStateDefaultsTest {
    private val encodeDefaultsJson = Json { encodeDefaults = true }

    @Test
    fun `current target distance is transient and defaults to unknown`() {
        val point = PilgrimagePoint(
            id = "point",
            name = "Point",
            coordinate = GeoPoint(35.0, 139.0),
        )
        val anime = Anime(subjectId = 1L, name = "Anime")
        val stored = StoredTourV2(
            id = "tour",
            displayAnime = anime,
            selectedAnimes = listOf(anime),
            selectedPoints = listOf(point),
            manualOrderPointIds = listOf(point.id),
            start = point.coordinate,
            mode = TravelMode.TRANSIT,
            objective = RouteObjective.FASTEST,
            endPolicy = EndPolicy.OPEN,
        )
        val encoded = encodeDefaultsJson.encodeToString(StoredTourV2.serializer(), stored)

        assertFalse(encoded.contains("currentTargetDistanceMeters"))
        assertFalse(encoded.contains("targetDistanceMeters"))
        assertNull(NavigationRuntimeState().currentTargetDistanceMeters)
        assertNull(JapanExternalTransitRuntimeState().targetDistanceMeters)
    }

    @Test
    fun `cold recovered dwelling and paused states expose explicit resume`() {
        assertFalse(externalTransitNeedsResume(isRunning = true, isPaused = false))
        org.junit.Assert.assertTrue(externalTransitNeedsResume(isRunning = false, isPaused = false))
        org.junit.Assert.assertTrue(externalTransitNeedsResume(isRunning = true, isPaused = true))
    }
}
