package cn.anitabi.navigator.core.routing

import cn.anitabi.navigator.core.model.EndPolicy
import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.PilgrimagePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TourOptimizerTest {
    private val optimizer = TourOptimizer()

    @Test
    fun `open fixed and return policies work for 2 8 and 12 stops`() {
        listOf(2, 8, 12).forEach { stopCount ->
            val matrix = lineMatrix(stopCount + 1)

            assertEquals(
                (0 until stopCount).toList(),
                optimizer.optimizeRoad(matrix, EndPolicy.OPEN),
            )
            assertEquals(
                (0 until stopCount).toList(),
                optimizer.optimizeRoad(matrix, EndPolicy.FIXED, fixedEndStopIndex = stopCount - 1),
            )
            val returnOrder = optimizer.optimizeRoad(matrix, EndPolicy.RETURN_TO_START)
            assertEquals((0 until stopCount).toSet(), returnOrder.toSet())
            assertEquals(2.0 * stopCount, routeCost(matrix, returnOrder, returnsToStart = true), 0.0001)
        }
    }

    @Test
    fun `fixed end is always placed last`() {
        val order = optimizer.optimizeRoad(
            matrix = lineMatrix(5),
            endPolicy = EndPolicy.FIXED,
            fixedEndStopIndex = 0,
        )

        assertEquals(0, order.last())
        assertEquals(setOf(0, 1, 2, 3), order.toSet())
    }

    @Test
    fun `unreachable matrix fails explicitly`() {
        val matrix = listOf(
            listOf(0.0, 1.0, null),
            listOf(1.0, 0.0, null),
            listOf(null, null, 0.0),
        )

        assertThrows(NoRouteException::class.java) {
            optimizer.optimizeRoad(matrix, EndPolicy.OPEN)
        }
    }

    @Test
    fun `transit recommendation uses nearest unvisited point`() {
        val points = listOf(
            point("far", 35.3),
            point("near", 35.1),
            point("middle", 35.2),
        )

        val ordered = optimizer.recommendTransitOrder(GeoPoint(35.0, 139.0), points)

        assertEquals(listOf("near", "middle", "far"), ordered.map(PilgrimagePoint::id))
    }

    private fun lineMatrix(size: Int): List<List<Double?>> = List(size) { from ->
        List(size) { to -> kotlin.math.abs(from - to).toDouble() }
    }

    private fun routeCost(
        matrix: List<List<Double?>>,
        stopOrder: List<Int>,
        returnsToStart: Boolean,
    ): Double {
        val matrixOrder = listOf(0) + stopOrder.map { it + 1 } + if (returnsToStart) listOf(0) else emptyList()
        return matrixOrder.zipWithNext().sumOf { (from, to) -> matrix[from][to]!! }
    }

    private fun point(id: String, latitude: Double) = PilgrimagePoint(
        id = id,
        name = id,
        coordinate = GeoPoint(latitude, 139.0),
    )
}
