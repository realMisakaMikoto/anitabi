package cn.anitabi.navigator.ui.search

import cn.anitabi.navigator.core.model.Anime
import cn.anitabi.navigator.core.model.EndPolicy
import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.NavigationState
import cn.anitabi.navigator.core.model.PilgrimagePoint
import cn.anitabi.navigator.core.model.RouteObjective
import cn.anitabi.navigator.core.model.StoredTourV2
import cn.anitabi.navigator.core.model.TravelMode
import cn.anitabi.navigator.data.repository.PilgrimageData
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchSelectionRestoreTest {
    @Test
    fun `cached works restore v0_2_0 multi anime and point selection without network`() {
        val animeA = Anime(101, "Anime A")
        val animeB = Anime(202, "Anime B")
        val cached = listOf(
            PilgrimageData(animeA, listOf(point("a", 1.0)), 1),
            PilgrimageData(animeB, listOf(point("b", 2.0)), 1),
        )
        val stored = StoredTourV2(
            id = "tour",
            displayAnime = Anime(0, "Anime A + Anime B"),
            selectedAnimes = listOf(animeA, animeB),
            selectedPoints = listOf(
                point("101::a", 1.0),
                point("202::b", 2.0),
            ),
            manualOrderPointIds = listOf("101::a", "202::b"),
            start = GeoPoint(0.0, 0.0),
            mode = TravelMode.WALK,
            objective = RouteObjective.FASTEST,
            endPolicy = EndPolicy.OPEN,
            navigationState = NavigationState.PLANNED,
        )

        val restored = restoreSearchSelection(stored, cached)!!

        assertEquals(setOf(101L, 202L), restored.animeData.keys)
        assertEquals(setOf("101::a", "202::b"), restored.selectedPointIds)
    }

    private fun point(id: String, latitude: Double) = PilgrimagePoint(
        id = id,
        name = id,
        coordinate = GeoPoint(latitude, 0.0),
    )
}
