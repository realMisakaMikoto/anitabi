package cn.anitabi.navigator.core.routing

import cn.anitabi.navigator.core.model.GeoPoint
import kotlin.math.pow

object PolylineDecoder {
    fun decode(encoded: String, precision: Int = 6): List<GeoPoint> {
        if (encoded.isEmpty()) return emptyList()
        val factor = 10.0.pow(precision)
        val points = mutableListOf<GeoPoint>()
        var index = 0
        var latitude = 0
        var longitude = 0

        while (index < encoded.length) {
            val latitudeChunk = decodeChunk(encoded, index)
            index = latitudeChunk.nextIndex
            latitude += latitudeChunk.value

            val longitudeChunk = decodeChunk(encoded, index)
            index = longitudeChunk.nextIndex
            longitude += longitudeChunk.value

            points += GeoPoint(latitude / factor, longitude / factor)
        }
        return points
    }

    private fun decodeChunk(encoded: String, startIndex: Int): Chunk {
        var index = startIndex
        var result = 0
        var shift = 0
        var byte: Int
        do {
            require(index < encoded.length) { "Malformed encoded polyline" }
            byte = encoded[index++].code - 63
            result = result or ((byte and 0x1f) shl shift)
            shift += 5
        } while (byte >= 0x20)
        val value = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        return Chunk(value, index)
    }

    private data class Chunk(val value: Int, val nextIndex: Int)
}
