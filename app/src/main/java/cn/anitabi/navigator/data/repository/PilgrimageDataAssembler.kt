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

fun mergePilgrimageData(selections: Collection<PilgrimageData>): PilgrimageData? {
    if (selections.isEmpty()) return null
    val multipleAnime = selections.size > 1
    val mergedAnime = if (multipleAnime) {
        Anime(
            subjectId = 0,
            name = selections.joinToString(" + ") { it.anime.name },
            nameCn = "${selections.size} 部作品联合巡礼",
            imageUrl = selections.first().anime.imageUrl,
        )
    } else {
        selections.single().anime
    }
    val points = selections.flatMap { data ->
        val title = data.anime.nameCn ?: data.anime.name
        data.points.map { point ->
            point.copy(
                id = scopedPilgrimagePointId(data.anime.subjectId, point.id),
                name = if (multipleAnime) "《$title》· ${point.name}" else point.name,
            )
        }
    }
    return PilgrimageData(
        anime = mergedAnime,
        points = points,
        expectedPointCount = selections.sumOf(PilgrimageData::expectedPointCount),
        warnings = selections.flatMap(PilgrimageData::warnings).toSet(),
    )
}

internal fun scopedPilgrimagePointId(subjectId: Long, pointId: String): String = "$subjectId::$pointId"

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
