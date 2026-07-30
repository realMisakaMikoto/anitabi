package cn.anitabi.navigator.data.repository

import cn.anitabi.navigator.core.model.NavigationProgress
import cn.anitabi.navigator.core.model.StoredTourV2
import cn.anitabi.navigator.core.model.TourPlan
import cn.anitabi.navigator.data.local.TourPlanDao
import cn.anitabi.navigator.data.local.TourPlanEntity
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

class TourRepository(
    private val dao: TourPlanDao,
    private val json: Json,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val resolvedRoutes = ConcurrentHashMap<String, TourPlan>()

    suspend fun save(plan: TourPlan, progress: NavigationProgress? = null) {
        val stored = StoredTourV2.from(plan, progress)
        resolvedRoutes[plan.id] = plan
        dao.upsert(
            TourPlanEntity(
                id = plan.id,
                storedTourJson = json.encodeToString(StoredTourV2.serializer(), stored),
                legacyPlanJson = null,
                legacyProgressJson = null,
                migrationError = null,
                routeNeedsRefresh = true,
                updatedAtEpochMillis = now(),
            ),
        )
    }

    suspend fun get(id: String): SavedTour? = dao.get(id)?.toSavedTour()

    suspend fun getMostRecent(): SavedTour? = dao.getMostRecent()?.toSavedTour()

    suspend fun getMostRecentRecoveryError(): String? = dao.getMostRecentMigrationError()

    private suspend fun TourPlanEntity.toSavedTour(): SavedTour? {
        val stored = storedTourJson?.let { json.decodeFromString(StoredTourV2.serializer(), it) }
            ?: migrateLegacyTour()
            ?: return null
        val resolved = resolvedRoutes[id]
        return SavedTour(
            storedTour = stored,
            plan = resolved ?: stored.toUnresolvedPlan(),
            progress = stored.toNavigationProgress(),
            routeNeedsRefresh = resolved == null,
        )
    }

    private suspend fun TourPlanEntity.migrateLegacyTour(): StoredTourV2? {
        val legacyPlan = legacyPlanJson ?: return null
        return runCatching {
            val plan = json.decodeFromString(TourPlan.serializer(), legacyPlan)
            val progress = legacyProgressJson?.let {
                json.decodeFromString(NavigationProgress.serializer(), it)
            }
            StoredTourV2.from(plan, progress)
        }.onSuccess { stored ->
            dao.finishLegacyMigration(
                id = id,
                storedTourJson = json.encodeToString(StoredTourV2.serializer(), stored),
                updatedAtEpochMillis = now(),
            )
        }.onFailure {
            dao.recordMigrationError(id, RECOVERY_ERROR_MESSAGE)
        }.getOrNull()
    }

    companion object {
        const val RECOVERY_ERROR_MESSAGE = "无法恢复 v0.2.0 行程；原记录已保留，请勿清除应用数据"
    }
}

data class SavedTour(
    val storedTour: StoredTourV2,
    val plan: TourPlan,
    val progress: NavigationProgress?,
    val routeNeedsRefresh: Boolean,
)
