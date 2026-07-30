package cn.anitabi.navigator.core.routing

object TourRequestBatcher {
    const val MAX_MATRIX_LOCATIONS = 10
    const val MAX_ROUTE_LOCATIONS = 12
    const val MAX_NAVIGATION_DESTINATIONS = 25

    fun <T> matrixWindows(start: T, orderedStops: List<T>): List<List<T>> {
        if (orderedStops.isEmpty()) return emptyList()
        val windows = mutableListOf<List<T>>()
        var anchor = start
        orderedStops.chunked(MAX_MATRIX_LOCATIONS - 1).forEach { stops ->
            windows += listOf(anchor) + stops
            anchor = stops.last()
        }
        return windows
    }

    fun <T> routeBatches(locations: List<T>): List<List<T>> {
        require(locations.size >= 2) { "A route requires at least two locations" }
        return locations.windowed(
            size = MAX_ROUTE_LOCATIONS,
            step = MAX_ROUTE_LOCATIONS - 1,
            partialWindows = true,
        ).filter { it.size >= 2 }
    }

    fun <T> navigationBatches(destinations: List<T>): List<List<T>> =
        destinations.chunked(MAX_NAVIGATION_DESTINATIONS)

    fun affectedMatrixWindowIndexes(
        fromStopIndex: Int,
        toStopIndex: Int,
        stopCount: Int,
    ): Set<Int> {
        require(fromStopIndex in 0 until stopCount && toStopIndex in 0 until stopCount) {
            "Moved stop indexes must be in range"
        }
        val stopsPerWindow = MAX_MATRIX_LOCATIONS - 1
        return buildSet {
            add(fromStopIndex / stopsPerWindow)
            add(toStopIndex / stopsPerWindow)
        }
    }
}
