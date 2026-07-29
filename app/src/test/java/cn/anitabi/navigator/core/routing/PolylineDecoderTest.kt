package cn.anitabi.navigator.core.routing

import org.junit.Assert.assertEquals
import org.junit.Test

class PolylineDecoderTest {
    @Test
    fun `decodes standard Google polyline with requested precision`() {
        val points = PolylineDecoder.decode("_p~iF~ps|U_ulLnnqC_mqNvxq`@", precision = 5)

        assertEquals(3, points.size)
        assertEquals(38.5, points[0].latitude, 0.00001)
        assertEquals(-120.2, points[0].longitude, 0.00001)
        assertEquals(43.252, points[2].latitude, 0.00001)
        assertEquals(-126.453, points[2].longitude, 0.00001)
    }
}
