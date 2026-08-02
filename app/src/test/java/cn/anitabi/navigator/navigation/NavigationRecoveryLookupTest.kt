package cn.anitabi.navigator.navigation

import cn.anitabi.navigator.core.model.Anime
import cn.anitabi.navigator.core.model.EndPolicy
import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.NavigationState
import cn.anitabi.navigator.core.model.PilgrimagePoint
import cn.anitabi.navigator.core.model.RouteObjective
import cn.anitabi.navigator.core.model.StoredTourV2
import cn.anitabi.navigator.core.model.TourPlan
import cn.anitabi.navigator.core.model.TransitExecutionStrategy
import cn.anitabi.navigator.core.model.TravelMode
import cn.anitabi.navigator.core.region.JapanRegion
import cn.anitabi.navigator.data.local.TourPlanDao
import cn.anitabi.navigator.data.local.TourPlanEntity
import cn.anitabi.navigator.data.network.ApiHttpClient
import cn.anitabi.navigator.data.repository.TourRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NavigationRecoveryLookupTest {
    @Test
    fun `empty active store falls back to most recent recoverable v0_2_2 tour`() = runBlocking {
        val stored = v022StoredTour(NavigationState.NAVIGATING)
        val encoded = ApiHttpClient.defaultJson.encodeToString(StoredTourV2.serializer(), stored)
        val dao = RecoveryLookupDao(entity(stored, updatedAtEpochMillis = 123L))
        val repository = TourRepository(dao, ApiHttpClient.defaultJson)

        val recovered = loadNavigationRecoveryCandidate(
            storedActiveTourId = null,
            repository = repository,
        )

        assertFalse(encoded.contains("executionStrategy"))
        assertFalse(encoded.contains("activeLegIndex"))
        assertNotNull(recovered)
        assertEquals(stored.id, recovered?.plan?.id)
        assertEquals(NavigationState.NAVIGATING, recovered?.progress?.state)
        assertEquals(1, dao.candidateListReads)
        assertEquals(1, dao.idReads)
    }

    @Test
    fun `scan skips newer inactive tour and restores older active tour`() = runBlocking {
        val planned = v022StoredTour(NavigationState.PLANNED, id = "new-planned")
        val active = v022StoredTour(NavigationState.DWELLING, id = "older-active").copy(
            activeLegIndex = 3,
            isPaused = true,
            pausedAtEpochMillis = 456L,
        )
        val dao = RecoveryLookupDao(
            entity(planned, updatedAtEpochMillis = 200L),
            entity(active, updatedAtEpochMillis = 100L),
        )

        val recovered = loadNavigationRecoveryCandidate(
            storedActiveTourId = null,
            repository = TourRepository(dao, ApiHttpClient.defaultJson),
        )

        assertEquals(active.id, recovered?.plan?.id)
        assertEquals(NavigationState.DWELLING, recovered?.progress?.state)
        assertEquals(3, recovered?.progress?.legIndex)
        assertEquals(true, recovered?.progress?.isPaused)
        assertEquals(2, dao.idReads)
    }

    @Test
    fun `stale active store falls back but an existing terminal store never revives an older tour`() = runBlocking {
        val terminal = v022StoredTour(NavigationState.COMPLETED, id = "terminal")
        val active = v022StoredTour(NavigationState.NAVIGATING, id = "active")
        val dao = RecoveryLookupDao(
            entity(terminal, updatedAtEpochMillis = 200L),
            entity(active, updatedAtEpochMillis = 100L),
        )
        val repository = TourRepository(dao, ApiHttpClient.defaultJson)

        assertEquals(
            active.id,
            loadNavigationRecoveryCandidate("missing", repository)?.plan?.id,
        )
        assertEquals(
            terminal.id,
            loadNavigationRecoveryCandidate(terminal.id, repository)?.plan?.id,
        )
        assertEquals(
            NavigationBootRestoreAction.CLEAR_STALE_POINTER,
            navigationBootRestoreAction(loadNavigationRecoveryCandidate(terminal.id, repository)),
        )
        assertEquals(
            NavigationBootRestoreAction.IGNORE_NON_EXTERNAL,
            navigationBootRestoreAction(loadNavigationRecoveryCandidate("missing", repository)),
        )
    }

    @Test
    fun `boot restore control is limited to recoverable external japan transit`() = runBlocking {
        val stored = v022StoredTour(
            state = NavigationState.NAVIGATING,
            id = "legacy-transit",
            mode = TravelMode.TRANSIT,
        )
        val dao = RecoveryLookupDao(entity(stored, updatedAtEpochMillis = 100L))
        val external = loadNavigationRecoveryCandidate(
            storedActiveTourId = null,
            repository = TourRepository(
                dao = dao,
                json = ApiHttpClient.defaultJson,
                classifyRegion = { JapanRegion.JAPAN },
            ),
        )
        val inApp = loadNavigationRecoveryCandidate(
            storedActiveTourId = null,
            repository = TourRepository(
                dao = dao,
                json = ApiHttpClient.defaultJson,
                classifyRegion = { JapanRegion.NON_JAPAN },
            ),
        )

        assertEquals(
            NavigationBootRestoreAction.SHOW_EXTERNAL_JAPAN_CONTROL,
            navigationBootRestoreAction(external),
        )
        assertEquals(
            NavigationBootRestoreAction.IGNORE_NON_EXTERNAL,
            navigationBootRestoreAction(inApp),
        )
        assertEquals(
            NavigationBootRestoreAction.CLEAR_STALE_POINTER,
            navigationBootRestoreAction(external?.copy(progress = null)),
        )
        assertEquals(
            NavigationBootRestoreAction.CLEAR_STALE_POINTER,
            navigationBootRestoreAction(null),
        )
    }

    @Test
    fun `empty active store ignores legacy tours when none are active`() = runBlocking {
        val dao = RecoveryLookupDao(
            entity(v022StoredTour(NavigationState.PLANNED, "planned"), 300L),
            entity(v022StoredTour(NavigationState.COMPLETED, "completed"), 200L),
            entity(v022StoredTour(NavigationState.ENDED, "ended"), 100L),
        )

        assertNull(
            loadNavigationRecoveryCandidate(
                storedActiveTourId = null,
                repository = TourRepository(dao, ApiHttpClient.defaultJson),
            ),
        )
    }

    @Test
    fun `inactive legacy transit never loads region data while restoring an older road tour`() = runBlocking {
        val inactiveTransit = v022StoredTour(
            state = NavigationState.PLANNED,
            id = "new-transit",
            mode = TravelMode.TRANSIT,
        )
        val activeRoad = v022StoredTour(NavigationState.NAVIGATING, id = "older-road")
        val dao = RecoveryLookupDao(
            entity(inactiveTransit, 200L),
            entity(activeRoad, 100L),
        )
        val repository = TourRepository(
            dao = dao,
            json = ApiHttpClient.defaultJson,
            classifyRegion = { error("Inactive transit must not classify regions") },
        )

        val recovered = loadNavigationRecoveryCandidate(null, repository)

        assertEquals(activeRoad.id, recovered?.plan?.id)
    }

    @Test
    fun `cold recovery never overwrites a running or newer runtime snapshot`() {
        val recovered = runtimeState("tour", NavigationState.DWELLING, isRunning = false)
        val running = runtimeState("tour", NavigationState.NEXT_STOP, isRunning = true)
        val newerIdle = runtimeState("tour", NavigationState.NEXT_STOP, isRunning = false)

        assertEquals(running, navigationRuntimeAfterColdRecovery(running, recovered))
        assertEquals(newerIdle, navigationRuntimeAfterColdRecovery(newerIdle, recovered))
        assertEquals(recovered, navigationRuntimeAfterColdRecovery(NavigationRuntimeState(), recovered))
    }

    private fun v022StoredTour(
        state: NavigationState,
        id: String = "legacy-v0.2.2",
        mode: TravelMode = TravelMode.WALK,
    ): StoredTourV2 {
        val point = PilgrimagePoint("point", "Point", GeoPoint(37.5665, 126.9780))
        val anime = Anime(1L, "Legacy")
        return StoredTourV2(
            id = id,
            displayAnime = anime,
            selectedAnimes = listOf(anime),
            selectedPoints = listOf(point),
            manualOrderPointIds = listOf(point.id),
            start = point.coordinate,
            startPointId = point.id,
            mode = mode,
            objective = RouteObjective.FASTEST,
            endPolicy = EndPolicy.OPEN,
            activePointId = point.id,
            navigationState = state,
        )
    }

    private fun entity(stored: StoredTourV2, updatedAtEpochMillis: Long): TourPlanEntity = TourPlanEntity(
        id = stored.id,
        storedTourJson = ApiHttpClient.defaultJson.encodeToString(StoredTourV2.serializer(), stored),
        legacyPlanJson = null,
        legacyProgressJson = null,
        migrationError = null,
        routeNeedsRefresh = true,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )

    private fun runtimeState(
        id: String,
        state: NavigationState,
        isRunning: Boolean,
    ): NavigationRuntimeState {
        val stored = v022StoredTour(state, id)
        val strategy = TransitExecutionStrategy.IN_APP_GOOGLE_ROUTES
        val plan: TourPlan = stored.toUnresolvedPlan(strategy)
        return NavigationRuntimeState(
            plan = plan,
            progress = stored.toNavigationProgress(strategy),
            isRunning = isRunning,
        )
    }
}

private class RecoveryLookupDao(
    vararg entities: TourPlanEntity,
) : TourPlanDao {
    private val entities = entities.associateBy(TourPlanEntity::id)
    var idReads = 0
        private set
    var candidateListReads = 0
        private set

    override suspend fun get(id: String): TourPlanEntity? {
        idReads += 1
        return entities[id]
    }

    override suspend fun getMostRecent(): TourPlanEntity? = entities.values
        .maxWithOrNull(compareBy<TourPlanEntity> { it.updatedAtEpochMillis }.thenBy { it.id })

    override suspend fun getIdsMostRecentFirst(): List<String> {
        candidateListReads += 1
        return entities.values
            .sortedWith(compareByDescending<TourPlanEntity> { it.updatedAtEpochMillis }.thenByDescending { it.id })
            .map(TourPlanEntity::id)
    }

    override suspend fun upsert(entity: TourPlanEntity) = error("Not used")

    override suspend fun finishLegacyMigration(
        id: String,
        storedTourJson: String,
        updatedAtEpochMillis: Long,
    ) = error("Not used")

    override suspend fun recordMigrationError(id: String, message: String) = error("Not used")

    override suspend fun getMostRecentMigrationError(): String? = null
}
