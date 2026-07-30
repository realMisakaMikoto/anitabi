package cn.anitabi.navigator.core.routing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TourRequestBatcherTest {
    @Test
    fun `matrix windows never exceed ten locations and preserve every stop`() {
        val windows = TourRequestBatcher.matrixWindows(start = 0, orderedStops = (1..28).toList())

        assertTrue(windows.all { it.size in 2..10 })
        assertEquals((1..28).toList(), windows.flatMap { it.drop(1) })
        assertEquals(listOf(0, 9, 18, 27), windows.map { it.first() })
    }

    @Test
    fun `route batches overlap at boundaries and never exceed twelve locations`() {
        val batches = TourRequestBatcher.routeBatches((0..35).toList())

        assertTrue(batches.all { it.size in 2..12 })
        assertEquals((0..35).toList(), buildList {
            batches.forEachIndexed { index, batch -> addAll(if (index == 0) batch else batch.drop(1)) }
        })
        assertTrue(batches.zipWithNext().all { (left, right) -> left.last() == right.first() })
    }

    @Test
    fun `navigation batches never exceed twenty five destinations`() {
        val batches = TourRequestBatcher.navigationBatches((1..61).toList())

        assertEquals(listOf(25, 25, 11), batches.map(List<Int>::size))
        assertEquals((1..61).toList(), batches.flatten())
    }

    @Test
    fun `dragging only marks source and destination matrix windows`() {
        assertEquals(setOf(0, 2), TourRequestBatcher.affectedMatrixWindowIndexes(4, 22, 30))
        assertEquals(setOf(1), TourRequestBatcher.affectedMatrixWindowIndexes(10, 17, 30))
    }
}
