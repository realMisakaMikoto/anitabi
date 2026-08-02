package cn.anitabi.navigator.data.repository

import cn.anitabi.navigator.core.model.Anime
import cn.anitabi.navigator.core.model.EndPolicy
import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.NavigationState
import cn.anitabi.navigator.core.model.PilgrimagePoint
import cn.anitabi.navigator.core.model.RouteObjective
import cn.anitabi.navigator.core.model.StoredTourV2
import cn.anitabi.navigator.core.model.TransitExecutionStrategy
import cn.anitabi.navigator.core.model.TravelMode
import cn.anitabi.navigator.core.region.JapanRegion
import cn.anitabi.navigator.data.local.TourPlanDao
import cn.anitabi.navigator.data.local.TourPlanEntity
import cn.anitabi.navigator.data.network.ApiHttpClient
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TourRepositoryJapanTransitRecoveryTest {
    @Test
    fun `v0_2_2 transit tours without strategy are reclassified from stored coordinates`() = runBlocking {
        val dao = RecoveryTourPlanDao()
        val japanPoints = listOf(
            point("tokyo", 35.6762, 139.6503),
            point("sapporo", 43.0618, 141.3545),
        )
        val nonJapanPoints = listOf(
            point("seoul", 37.5665, 126.9780),
            point("busan", 35.1796, 129.0756),
        )
        dao.seed(legacyEntity("legacy-japan", japanPoints))
        dao.seed(legacyEntity("legacy-non-japan", nonJapanPoints))
        val classifiedCoordinates = mutableListOf<GeoPoint>()
        val repository = TourRepository(
            dao = dao,
            json = ApiHttpClient.defaultJson,
            now = { 123L },
            classifyRegion = { coordinate ->
                classifiedCoordinates += coordinate
                if (coordinate.longitude >= 130.0) JapanRegion.JAPAN else JapanRegion.NON_JAPAN
            },
        )

        val japan = requireNotNull(repository.get("legacy-japan"))
        val nonJapan = requireNotNull(repository.get("legacy-non-japan"))

        assertNull(japan.storedTour.executionStrategy)
        assertNull(nonJapan.storedTour.executionStrategy)
        assertEquals(TransitExecutionStrategy.EXTERNAL_GOOGLE_MAPS_JAPAN, japan.plan.executionStrategy)
        assertEquals(TransitExecutionStrategy.IN_APP_GOOGLE_ROUTES, nonJapan.plan.executionStrategy)
        assertTrue(japan.routeNeedsRefresh)
        assertTrue(nonJapan.routeNeedsRefresh)
        assertEquals(1, japan.progress?.legIndex)
        assertEquals(0, nonJapan.progress?.legIndex)
        assertEquals((japanPoints + nonJapanPoints).map(PilgrimagePoint::coordinate), classifiedCoordinates)
    }

    @Test
    fun `v0_2_2 Japanese return tour without active leg restores return segment`() = runBlocking {
        val dao = RecoveryTourPlanDao()
        val points = listOf(
            point("first", 35.6762, 139.6503),
            point("second", 35.6895, 139.6917),
        )
        val stored = StoredTourV2(
            id = "legacy-japan-return",
            displayAnime = Anime(1, "Legacy return"),
            selectedAnimes = listOf(Anime(1, "Legacy return")),
            selectedPoints = points,
            manualOrderPointIds = points.map(PilgrimagePoint::id),
            start = points.first().coordinate,
            startPointId = points.first().id,
            mode = TravelMode.TRANSIT,
            objective = RouteObjective.FASTEST,
            endPolicy = EndPolicy.RETURN_TO_START,
            completedPointIds = points.map(PilgrimagePoint::id).toSet(),
            activePointId = null,
            activeLegIndex = null,
            navigationState = NavigationState.NAVIGATING,
            executionStrategy = null,
        )
        val encoded = ApiHttpClient.defaultJson.encodeToString(StoredTourV2.serializer(), stored)
        assertFalse(encoded.contains("executionStrategy"))
        assertFalse(encoded.contains("activeLegIndex"))
        dao.seed(storedEntity(stored, encoded))
        val repository = TourRepository(
            dao = dao,
            json = ApiHttpClient.defaultJson,
            classifyRegion = { JapanRegion.JAPAN },
        )

        val restored = requireNotNull(repository.get(stored.id))

        assertEquals(TransitExecutionStrategy.EXTERNAL_GOOGLE_MAPS_JAPAN, restored.plan.executionStrategy)
        assertEquals(points.size, restored.progress?.legIndex)
    }

    @Test
    fun `repository restores active leg and paused state after process death`() = runBlocking {
        val dao = RecoveryTourPlanDao()
        val points = listOf(
            point("first", 35.6762, 139.6503),
            point("second", 35.6895, 139.6917),
            point("third", 35.7101, 139.8107),
        )
        val stored = StoredTourV2(
            id = "paused-japan",
            displayAnime = Anime(1, "Paused"),
            selectedAnimes = listOf(Anime(1, "Paused")),
            selectedPoints = points,
            manualOrderPointIds = points.map(PilgrimagePoint::id),
            start = points.first().coordinate,
            startPointId = points.first().id,
            mode = TravelMode.TRANSIT,
            objective = RouteObjective.FASTEST,
            endPolicy = EndPolicy.OPEN,
            completedPointIds = setOf(points.first().id),
            activePointId = points.last().id,
            activeLegIndex = 1,
            navigationState = NavigationState.NAVIGATING,
            dwellingUntilEpochMillis = 20_000L,
            executionStrategy = TransitExecutionStrategy.EXTERNAL_GOOGLE_MAPS_JAPAN,
            isPaused = true,
            pausedAtEpochMillis = 15_000L,
        )
        dao.seed(storedEntity(stored))
        val repository = TourRepository(
            dao = dao,
            json = ApiHttpClient.defaultJson,
            classifyRegion = { error("Persisted strategy must not be reclassified") },
        )

        val restored = requireNotNull(repository.get(stored.id))

        assertTrue(restored.routeNeedsRefresh)
        assertEquals(TransitExecutionStrategy.EXTERNAL_GOOGLE_MAPS_JAPAN, restored.plan.executionStrategy)
        assertEquals(1, restored.progress?.legIndex)
        assertEquals(NavigationState.NAVIGATING, restored.progress?.state)
        assertEquals(setOf("first"), restored.progress?.completedPointIds)
        assertEquals(20_000L, restored.progress?.dwellingUntilEpochMillis)
        assertTrue(restored.progress?.isPaused == true)
        assertEquals(15_000L, restored.progress?.pausedAtEpochMillis)
    }

    private fun legacyEntity(id: String, points: List<PilgrimagePoint>): TourPlanEntity {
        val stored = StoredTourV2(
            id = id,
            displayAnime = Anime(1, "Legacy"),
            selectedAnimes = listOf(Anime(1, "Legacy")),
            selectedPoints = points,
            manualOrderPointIds = points.map(PilgrimagePoint::id),
            start = points.first().coordinate,
            startPointId = points.first().id,
            mode = TravelMode.TRANSIT,
            objective = RouteObjective.FASTEST,
            endPolicy = EndPolicy.OPEN,
            completedPointIds = setOf(points.first().id),
            activePointId = points.last().id,
            navigationState = NavigationState.NAVIGATING,
            executionStrategy = null,
        )
        val encoded = ApiHttpClient.defaultJson.encodeToString(StoredTourV2.serializer(), stored)
        assertFalse(encoded.contains("executionStrategy"))
        assertFalse(encoded.contains("activeLegIndex"))
        assertFalse(encoded.contains("isPaused"))
        return storedEntity(stored, encoded)
    }

    private fun storedEntity(
        stored: StoredTourV2,
        encoded: String = ApiHttpClient.defaultJson.encodeToString(StoredTourV2.serializer(), stored),
    ) = TourPlanEntity(
        id = stored.id,
        storedTourJson = encoded,
        legacyPlanJson = null,
        legacyProgressJson = null,
        migrationError = null,
        routeNeedsRefresh = true,
        updatedAtEpochMillis = 123L,
    )

    private fun point(id: String, latitude: Double, longitude: Double) = PilgrimagePoint(
        id = id,
        name = id,
        coordinate = GeoPoint(latitude, longitude),
    )
}

private class RecoveryTourPlanDao : TourPlanDao {
    private val entities = linkedMapOf<String, TourPlanEntity>()

    fun seed(entity: TourPlanEntity) {
        entities[entity.id] = entity
    }

    override suspend fun get(id: String): TourPlanEntity? = entities[id]

    override suspend fun getMostRecent(): TourPlanEntity? =
        entities.values.maxByOrNull(TourPlanEntity::updatedAtEpochMillis)

    override suspend fun getIdsMostRecentFirst(): List<String> = entities.values
        .sortedWith(compareByDescending<TourPlanEntity> { it.updatedAtEpochMillis }.thenByDescending { it.id })
        .map(TourPlanEntity::id)

    override suspend fun upsert(entity: TourPlanEntity) {
        entities[entity.id] = entity
    }

    override suspend fun finishLegacyMigration(
        id: String,
        storedTourJson: String,
        updatedAtEpochMillis: Long,
    ) = error("Not used")

    override suspend fun recordMigrationError(id: String, message: String) = error("Not used")

    override suspend fun getMostRecentMigrationError(): String? = null
}
