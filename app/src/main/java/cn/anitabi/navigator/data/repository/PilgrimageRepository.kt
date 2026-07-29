package cn.anitabi.navigator.data.repository

import cn.anitabi.navigator.core.model.Anime
import cn.anitabi.navigator.core.model.PilgrimagePoint
import cn.anitabi.navigator.data.local.PilgrimageCacheDao
import cn.anitabi.navigator.data.local.PilgrimageCacheEntity
import cn.anitabi.navigator.data.network.anitabi.AnitabiApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class PilgrimageRepository(
    private val api: AnitabiApi,
    private val cacheDao: PilgrimageCacheDao,
    private val json: Json,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun load(subjectId: Long, refresh: Boolean = false): PilgrimageData {
        if (!refresh) {
            cacheDao.get(subjectId)?.let {
                return json.decodeFromString(PilgrimageData.serializer(), it.payloadJson)
            }
        }

        val (lite, detailDtos) = coroutineScope {
            val liteRequest = async { api.getLite(subjectId) }
            val detailsRequest = async { api.getPointDetails(subjectId) }
            liteRequest.await() to detailsRequest.await()
        }
        val points = detailDtos.mapNotNull { it.toPilgrimagePointOrNull() }
        val warnings = buildSet {
            if (points.size < detailDtos.size) add(PilgrimageWarning.INVALID_COORDINATES_SKIPPED)
            if (points.size < lite.pointsLength) add(PilgrimageWarning.PARTIAL_DATA)
        }
        val result = PilgrimageData(
            anime = lite.toAnime(),
            points = points,
            expectedPointCount = lite.pointsLength,
            warnings = warnings,
        )
        cacheDao.upsert(
            PilgrimageCacheEntity(
                subjectId = subjectId,
                payloadJson = json.encodeToString(PilgrimageData.serializer(), result),
                updatedAtEpochMillis = now(),
            ),
        )
        return result
    }
}

@Serializable
data class PilgrimageData(
    val anime: Anime,
    val points: List<PilgrimagePoint>,
    val expectedPointCount: Int,
    val warnings: Set<PilgrimageWarning> = emptySet(),
)

@Serializable
enum class PilgrimageWarning {
    PARTIAL_DATA,
    INVALID_COORDINATES_SKIPPED,
}
