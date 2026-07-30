package cn.anitabi.navigator.core.navigation

import cn.anitabi.navigator.core.routing.TourRequestBatcher

class RoadNavigationBatchCoordinator(
    private val destinationCount: Int,
    initialLegIndex: Int,
    private val batchSize: Int = QUOTA_AWARE_BATCH_SIZE,
) {
    private var activeBatch: IntRange? = null
    private var activeLegIndex = initialLegIndex

    init {
        require(destinationCount >= 0) { "Destination count cannot be negative" }
        require(initialLegIndex in 0..destinationCount) { "Initial leg index is out of range" }
        require(batchSize in 1..TourRequestBatcher.MAX_NAVIGATION_DESTINATIONS) {
            "Navigation batch size exceeds the SDK limit"
        }
    }

    fun actionFor(legIndex: Int): RoadNavigationAction {
        require(legIndex in 0..destinationCount) { "Leg index is out of range" }
        if (legIndex == destinationCount) return RoadNavigationAction.Complete

        val batch = activeBatch
        if (batch == null || legIndex !in batch || legIndex > activeLegIndex + 1) {
            return RoadNavigationAction.Load(batchStartingAt(legIndex))
        }
        if (legIndex == activeLegIndex + 1) {
            activeLegIndex = legIndex
            return RoadNavigationAction.Continue
        }
        return RoadNavigationAction.None
    }

    fun markLoaded(batch: IntRange) {
        require(!batch.isEmpty() && batch.first >= 0 && batch.last < destinationCount) {
            "Loaded navigation batch is out of range"
        }
        require(batch.count() <= TourRequestBatcher.MAX_NAVIGATION_DESTINATIONS) {
            "Loaded navigation batch exceeds the SDK limit"
        }
        activeBatch = batch
        activeLegIndex = batch.first
    }

    fun currentLegIndex(): Int = activeLegIndex

    private fun batchStartingAt(legIndex: Int): IntRange {
        val last = minOf(destinationCount - 1, legIndex + batchSize - 1)
        return legIndex..last
    }

    companion object {
        const val QUOTA_AWARE_BATCH_SIZE = 20
    }
}

sealed interface RoadNavigationAction {
    data class Load(val legIndexes: IntRange) : RoadNavigationAction
    data object Continue : RoadNavigationAction
    data object Complete : RoadNavigationAction
    data object None : RoadNavigationAction
}
