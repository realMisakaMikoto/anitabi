package cn.anitabi.navigator.ui.search

import cn.anitabi.navigator.core.model.PilgrimagePoint

data class GeoBounds(
    val north: Double,
    val east: Double,
    val south: Double,
    val west: Double,
) {
    fun contains(point: PilgrimagePoint): Boolean {
        val longitudeInside = if (west <= east) {
            point.coordinate.longitude in west..east
        } else {
            point.coordinate.longitude >= west || point.coordinate.longitude <= east
        }
        return point.coordinate.latitude in south..north && longitudeInside
    }
}
