package cn.anitabi.navigator.data.repository

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
}
