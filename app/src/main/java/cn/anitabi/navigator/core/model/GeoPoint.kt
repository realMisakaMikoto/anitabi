package cn.anitabi.navigator.core.model

import kotlinx.serialization.Serializable

@Serializable
data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
    }

    fun toGeoJsonPosition(): List<Double> = listOf(longitude, latitude)

    companion object {
        fun fromAnitabiGeo(geo: List<Double>): GeoPoint {
            require(geo.size >= 2) { "Anitabi geo must contain latitude and longitude" }
            return GeoPoint(latitude = geo[0], longitude = geo[1])
        }
    }
}
