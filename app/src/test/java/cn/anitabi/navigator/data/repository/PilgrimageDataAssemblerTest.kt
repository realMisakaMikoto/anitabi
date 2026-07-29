package cn.anitabi.navigator.data.repository

import cn.anitabi.navigator.core.model.Anime
import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.PilgrimagePoint
import cn.anitabi.navigator.data.network.anitabi.AnitabiLiteDto
import cn.anitabi.navigator.data.network.anitabi.AnitabiPointDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PilgrimageDataAssemblerTest {
    @Test
    fun `details shorter than advertised produce a partial data warning`() {
        val result = assemblePilgrimageData(
            lite = AnitabiLiteDto(id = 1, pointsLength = 3),
            detailDtos = listOf(AnitabiPointDto(id = "a", geo = listOf(35.0, 139.0))),
        )

        assertEquals(1, result.points.size)
        assertTrue(PilgrimageWarning.PARTIAL_DATA in result.warnings)
    }

    @Test
    fun `invalid details are skipped and reported independently`() {
        val result = assemblePilgrimageData(
            lite = AnitabiLiteDto(id = 1, pointsLength = 2),
            detailDtos = listOf(
                AnitabiPointDto(id = "valid", geo = listOf(35.0, 139.0)),
                AnitabiPointDto(id = "invalid", geo = listOf(200.0)),
            ),
        )

        assertEquals(listOf("valid"), result.points.map { it.id })
        assertTrue(PilgrimageWarning.INVALID_COORDINATES_SKIPPED in result.warnings)
        assertTrue(PilgrimageWarning.PARTIAL_DATA in result.warnings)
    }

    @Test
    fun `multiple anime merge all points with collision safe ids and visible titles`() {
        val first = pilgrimageData(subjectId = 1, title = "甲", pointId = "same", pointName = "车站")
        val second = pilgrimageData(subjectId = 2, title = "乙", pointId = "same", pointName = "公园")

        val merged = mergePilgrimageData(listOf(first, second))!!

        assertEquals(0, merged.anime.subjectId)
        assertEquals("2 部作品联合巡礼", merged.anime.nameCn)
        assertEquals(listOf("1::same", "2::same"), merged.points.map(PilgrimagePoint::id))
        assertEquals(listOf("《甲》· 车站", "《乙》· 公园"), merged.points.map(PilgrimagePoint::name))
        assertEquals(2, merged.expectedPointCount)
    }

    @Test
    fun `single anime keeps its identity while still namespacing point ids`() {
        val data = pilgrimageData(subjectId = 7, title = "单部作品", pointId = "p", pointName = "地点")

        val merged = mergePilgrimageData(listOf(data))!!

        assertEquals(data.anime, merged.anime)
        assertEquals("7::p", merged.points.single().id)
        assertEquals("地点", merged.points.single().name)
    }

    private fun pilgrimageData(
        subjectId: Long,
        title: String,
        pointId: String,
        pointName: String,
    ) = PilgrimageData(
        anime = Anime(subjectId = subjectId, name = title, nameCn = title),
        points = listOf(
            PilgrimagePoint(
                id = pointId,
                name = pointName,
                coordinate = GeoPoint(35.0 + subjectId / 100, 139.0),
            ),
        ),
        expectedPointCount = 1,
    )
}
