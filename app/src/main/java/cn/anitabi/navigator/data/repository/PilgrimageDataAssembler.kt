package cn.anitabi.navigator.data.repository

import cn.anitabi.navigator.core.model.Anime
import cn.anitabi.navigator.core.model.PilgrimagePoint
import cn.anitabi.navigator.data.network.anitabi.AnitabiLiteDto
import cn.anitabi.navigator.data.network.anitabi.AnitabiPointDto
import kotlinx.serialization.Serializable

fun assemblePilgrimageData(
    lite: AnitabiLiteDto,
    detailDtos: List<AnitabiPointDto>,
): PilgrimageData {
    val points = detailDtos.mapNotNull(AnitabiPointDto::toPilgrimagePointOrNull)
    val warnings = buildSet {
        if (points.size < detailDtos.size) add(PilgrimageWarning.INVALID_COORDINATES_SKIPPED)
        if (points.size < lite.pointsLength) add(PilgrimageWarning.PARTIAL_DATA)
    }
    return PilgrimageData(
        anime = lite.toAnime(),
        points = points,
        expectedPointCount = lite.pointsLength,
        warnings = warnings,
    )
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
