package cn.anitabi.navigator.core.navigation

import cn.anitabi.navigator.core.routing.TourRequestBatcher
import org.junit.Assert.assertEquals
import org.junit.Test

class RoadNavigationBatchCoordinatorTest {
    @Test
    fun `production batches reserve no more than the daily per-user allowance`() {
        val coordinator = RoadNavigationBatchCoordinator(destinationCount = 61, initialLegIndex = 0)

        assertEquals(RoadNavigationAction.Load(0..19), coordinator.actionFor(0))
        coordinator.markLoaded(0..19)
        assertEquals(RoadNavigationAction.None, coordinator.actionFor(0))
        assertEquals(RoadNavigationAction.Continue, coordinator.actionFor(1))
        assertEquals(RoadNavigationAction.Load(20..39), coordinator.actionFor(20))
    }

    @Test
    fun `resume can load a batch from a saved middle leg`() {
        val coordinator = RoadNavigationBatchCoordinator(destinationCount = 61, initialLegIndex = 25)

        assertEquals(RoadNavigationAction.Load(25..44), coordinator.actionFor(25))
    }

    @Test
    fun `return destination is completed after the final leg`() {
        val coordinator = RoadNavigationBatchCoordinator(destinationCount = 2, initialLegIndex = 0)
        coordinator.markLoaded(0..1)

        assertEquals(RoadNavigationAction.Continue, coordinator.actionFor(1))
        assertEquals(RoadNavigationAction.Complete, coordinator.actionFor(2))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `batch size cannot exceed the Navigation SDK ceiling`() {
        RoadNavigationBatchCoordinator(
            destinationCount = 26,
            initialLegIndex = 0,
            batchSize = TourRequestBatcher.MAX_NAVIGATION_DESTINATIONS + 1,
        )
    }
}
