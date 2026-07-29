package cn.anitabi.navigator.core.routing

import cn.anitabi.navigator.core.model.EndPolicy
import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.PilgrimagePoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class TourOptimizer {
    fun optimizeRoad(
        matrix: List<List<Double?>>,
        endPolicy: EndPolicy,
        fixedEndStopIndex: Int? = null,
    ): List<Int> {
        require(matrix.size in 2..13) { "Matrix must contain a start and 1 to 12 stops" }
        require(matrix.all { it.size == matrix.size }) { "Matrix must be square" }
        val stopCount = matrix.size - 1
        if (endPolicy == EndPolicy.FIXED) {
            require(fixedEndStopIndex in 0 until stopCount) { "A valid fixed end stop is required" }
        }

        val stateCount = 1 shl stopCount
        val costs = Array(stateCount) { DoubleArray(stopCount) { Double.POSITIVE_INFINITY } }
        val parents = Array(stateCount) { IntArray(stopCount) { NO_PARENT } }
        val allVisited = stateCount - 1

        for (stop in 0 until stopCount) {
            if (endPolicy == EndPolicy.FIXED && stop == fixedEndStopIndex && stopCount > 1) continue
            costs[1 shl stop][stop] = matrix.cost(START_INDEX, stop + 1)
        }

        for (mask in 1..allVisited) {
            for (last in 0 until stopCount) {
                val currentCost = costs[mask][last]
                if (!currentCost.isFinite()) continue
                for (next in 0 until stopCount) {
                    val nextBit = 1 shl next
                    if (mask and nextBit != 0) continue
                    val nextMask = mask or nextBit
                    if (endPolicy == EndPolicy.FIXED && next == fixedEndStopIndex && nextMask != allVisited) continue
                    val candidate = currentCost + matrix.cost(last + 1, next + 1)
                    if (candidate < costs[nextMask][next]) {
                        costs[nextMask][next] = candidate
                        parents[nextMask][next] = last
                    }
                }
            }
        }

        val finalStop = when (endPolicy) {
            EndPolicy.FIXED -> fixedEndStopIndex!!
            EndPolicy.OPEN -> (0 until stopCount).minBy { costs[allVisited][it] }
            EndPolicy.RETURN_TO_START -> (0 until stopCount).minBy {
                costs[allVisited][it] + matrix.cost(it + 1, START_INDEX)
            }
        }
        val finalCost = costs[allVisited][finalStop] + if (endPolicy == EndPolicy.RETURN_TO_START) {
            matrix.cost(finalStop + 1, START_INDEX)
        } else {
            0.0
        }
        if (!finalCost.isFinite()) throw NoRouteException("No route can visit every selected stop")

        val reversed = ArrayList<Int>(stopCount)
        var mask = allVisited
        var current = finalStop
        while (current != NO_PARENT) {
            reversed += current
            val parent = parents[mask][current]
            mask = mask xor (1 shl current)
            current = parent
        }
        return reversed.asReversed()
    }

    fun recommendTransitOrder(
        start: GeoPoint,
        points: List<PilgrimagePoint>,
    ): List<PilgrimagePoint> {
        require(points.size <= 8) { "Transit supports at most 8 pilgrimage points" }
        val remaining = points.toMutableList()
        val ordered = ArrayList<PilgrimagePoint>(points.size)
        var current = start
        while (remaining.isNotEmpty()) {
            val next = remaining.minBy { haversineMeters(current, it.coordinate) }
            ordered += next
            remaining.remove(next)
            current = next.coordinate
        }
        return ordered
    }

    companion object {
        private const val START_INDEX = 0
        private const val NO_PARENT = -1

        fun haversineMeters(from: GeoPoint, to: GeoPoint): Double {
            val earthRadius = 6_371_000.0
            val latitudeDelta = Math.toRadians(to.latitude - from.latitude)
            val longitudeDelta = Math.toRadians(to.longitude - from.longitude)
            val fromLatitude = Math.toRadians(from.latitude)
            val toLatitude = Math.toRadians(to.latitude)
            val a = sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
                cos(fromLatitude) * cos(toLatitude) *
                sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
            return earthRadius * 2 * atan2(sqrt(a), sqrt(1 - a))
        }
    }
}

private fun List<List<Double?>>.cost(from: Int, to: Int): Double {
    val value = this[from][to]
    return value?.takeIf { it.isFinite() && it >= 0.0 } ?: Double.POSITIVE_INFINITY
}

class NoRouteException(message: String) : Exception(message)
