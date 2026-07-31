package cn.anitabi.navigator.data.repository

import cn.anitabi.navigator.core.model.Anime
import cn.anitabi.navigator.core.model.EndPolicy
import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.NavigationProgress
import cn.anitabi.navigator.core.model.NavigationState
import cn.anitabi.navigator.core.model.PilgrimagePoint
import cn.anitabi.navigator.core.model.RouteObjective
import cn.anitabi.navigator.core.model.StoredTourV2
import cn.anitabi.navigator.core.model.TourLeg
import cn.anitabi.navigator.core.model.TourPlan
import cn.anitabi.navigator.core.model.TravelMode
import cn.anitabi.navigator.data.local.TourPlanDao
import cn.anitabi.navigator.data.local.TourPlanEntity
import cn.anitabi.navigator.data.network.ApiHttpClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TourRepositoryTest {
    @Test
    fun `resolved tour preserves exact progress in the same process`() = runBlocking {
        val repository = TourRepository(FakeTourPlanDao(), ApiHttpClient.defaultJson, now = { 123L })
        val resolved = fixturePlan()
        val exactProgress = NavigationProgress(
            tourId = resolved.id,
            legIndex = 3,
            stepIndex = 7,
            completedPointIds = setOf("first"),
            state = NavigationState.NAVIGATING,
        )

        repository.save(resolved, exactProgress)

        val restored = requireNotNull(repository.get(resolved.id))
        assertFalse(restored.routeNeedsRefresh)
        assertEquals(exactProgress, restored.progress)
    }

    @Test
    fun `saving unresolved tour evicts resolved route and exact progress`() = runBlocking {
        val dao = FakeTourPlanDao()
        val repository = TourRepository(dao, ApiHttpClient.defaultJson, now = { 123L })
        val resolved = fixturePlan()
        repository.save(
            resolved,
            NavigationProgress(
                tourId = resolved.id,
                legIndex = 3,
                stepIndex = 7,
                state = NavigationState.NAVIGATING,
            ),
        )
        assertFalse(requireNotNull(repository.get(resolved.id)).routeNeedsRefresh)

        val unresolved = resolved.copy(legs = emptyList(), estimatedDurationSeconds = 0.0)
        repository.saveUnresolved(
            unresolved,
            NavigationProgress(
                tourId = resolved.id,
                completedPointIds = setOf("first"),
                state = NavigationState.PLANNED,
            ),
        )

        val restored = requireNotNull(repository.get(resolved.id))
        assertTrue(restored.routeNeedsRefresh)
        assertTrue(restored.plan.legs.isEmpty())
        assertEquals(setOf("first"), restored.progress?.completedPointIds)
        assertEquals(NavigationState.PLANNED, restored.progress?.state)
        assertEquals(0, restored.progress?.legIndex)
        assertEquals(0, restored.progress?.stepIndex)
    }

    @Test
    fun `cancelled save before dao commit does not publish resolved caches`() = runBlocking {
        val upsertGate = CompletableDeferred<Unit>()
        val dao = FakeTourPlanDao(upsertGate)
        val repository = TourRepository(dao, ApiHttpClient.defaultJson, now = { 123L })
        val resolved = fixturePlan()
        val saveJob = launch {
            repository.save(
                resolved,
                NavigationProgress(
                    tourId = resolved.id,
                    legIndex = 3,
                    stepIndex = 7,
                    state = NavigationState.NAVIGATING,
                ),
            )
        }
        dao.upsertStarted.await()

        saveJob.cancelAndJoin()

        assertNull(dao.get(resolved.id))
        val unresolved = resolved.copy(legs = emptyList(), estimatedDurationSeconds = 0.0)
        dao.seed(unresolvedEntity(unresolved))
        val restored = requireNotNull(repository.get(resolved.id))
        assertTrue(restored.routeNeedsRefresh)
        assertTrue(restored.plan.legs.isEmpty())
        assertEquals(0, restored.progress?.legIndex)
        assertEquals(0, restored.progress?.stepIndex)
    }

    private fun unresolvedEntity(plan: TourPlan): TourPlanEntity {
        val stored = StoredTourV2.from(
            plan,
            NavigationProgress(tourId = plan.id, state = NavigationState.PLANNED),
        )
        return TourPlanEntity(
            id = plan.id,
            storedTourJson = ApiHttpClient.defaultJson.encodeToString(StoredTourV2.serializer(), stored),
            legacyPlanJson = null,
            legacyProgressJson = null,
            migrationError = null,
            routeNeedsRefresh = true,
            updatedAtEpochMillis = 123L,
        )
    }

    private fun fixturePlan(): TourPlan {
        val first = PilgrimagePoint("first", "First", GeoPoint(1.0, 1.0))
        val second = PilgrimagePoint("second", "Second", GeoPoint(2.0, 2.0))
        return TourPlan(
            id = "tour",
            anime = Anime(1, "Test"),
            selectedPoints = listOf(first, second),
            orderedPoints = listOf(first, second),
            legs = listOf(
                TourLeg(
                    from = first.coordinate,
                    to = second.coordinate,
                    mode = TravelMode.WALK,
                    geometry = listOf(first.coordinate, second.coordinate),
                    steps = emptyList(),
                    distanceMeters = 100.0,
                    durationSeconds = 60.0,
                    source = "test",
                    destinationPointId = second.id,
                ),
            ),
            mode = TravelMode.WALK,
            objective = RouteObjective.FASTEST,
            endPolicy = EndPolicy.OPEN,
            estimatedDurationSeconds = 60.0,
            attribution = listOf("test"),
            initialStart = first.coordinate,
        )
    }
}

private class FakeTourPlanDao(
    private val upsertGate: CompletableDeferred<Unit>? = null,
) : TourPlanDao {
    private val entities = linkedMapOf<String, TourPlanEntity>()
    val upsertStarted = CompletableDeferred<Unit>()

    override suspend fun get(id: String): TourPlanEntity? = entities[id]

    override suspend fun getMostRecent(): TourPlanEntity? =
        entities.values.maxByOrNull(TourPlanEntity::updatedAtEpochMillis)

    override suspend fun upsert(entity: TourPlanEntity) {
        upsertStarted.complete(Unit)
        upsertGate?.await()
        entities[entity.id] = entity
    }

    fun seed(entity: TourPlanEntity) {
        entities[entity.id] = entity
    }

    override suspend fun finishLegacyMigration(id: String, storedTourJson: String, updatedAtEpochMillis: Long) {
        error("Not used")
    }

    override suspend fun recordMigrationError(id: String, message: String) {
        error("Not used")
    }

    override suspend fun getMostRecentMigrationError(): String? = null
}
