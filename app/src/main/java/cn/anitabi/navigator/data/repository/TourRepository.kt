package cn.anitabi.navigator.data.repository

import cn.anitabi.navigator.core.model.NavigationProgress
import cn.anitabi.navigator.core.model.TourPlan
import cn.anitabi.navigator.data.local.TourPlanDao
import cn.anitabi.navigator.data.local.TourPlanEntity
import kotlinx.serialization.json.Json

class TourRepository(
    private val dao: TourPlanDao,
    private val json: Json,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun save(plan: TourPlan, progress: NavigationProgress? = null) {
        dao.upsert(
            TourPlanEntity(
                id = plan.id,
                planJson = json.encodeToString(TourPlan.serializer(), plan),
                progressJson = progress?.let {
                    json.encodeToString(NavigationProgress.serializer(), it)
                },
                updatedAtEpochMillis = now(),
            ),
        )
    }

    suspend fun get(id: String): SavedTour? = dao.get(id)?.toSavedTour()

    suspend fun getMostRecent(): SavedTour? = dao.getMostRecent()?.toSavedTour()

    private fun TourPlanEntity.toSavedTour(): SavedTour = SavedTour(
        plan = json.decodeFromString(TourPlan.serializer(), planJson),
        progress = progressJson?.let {
            json.decodeFromString(NavigationProgress.serializer(), it)
        },
    )
}

data class SavedTour(
    val plan: TourPlan,
    val progress: NavigationProgress?,
)
