package cn.anitabi.navigator.data.network.ors

import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.TravelMode
import cn.anitabi.navigator.data.network.ApiException
import cn.anitabi.navigator.data.network.ApiHttpClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class OrsApi(
    private val httpClient: ApiHttpClient,
    private val apiKey: () -> String?,
    private val json: Json = ApiHttpClient.defaultJson,
    private val baseUrl: String = BASE_URL,
) {
    suspend fun matrix(mode: TravelMode, points: List<GeoPoint>): OrsMatrixResponse {
        require(mode != TravelMode.TRANSIT) { "ORS does not provide transit routing" }
        require(points.size in 2..13) { "ORS matrix requires 2 to 13 coordinates" }
        return post(
            path = "matrix/${mode.orsProfile()}",
            body = json.encodeToString(
                OrsMatrixRequest.serializer(),
                OrsMatrixRequest(
                    locations = points.map(GeoPoint::toGeoJsonPosition),
                    metrics = listOf("distance", "duration"),
                    units = "m",
                ),
            ),
            deserializer = OrsMatrixResponse.serializer(),
        )
    }

    suspend fun directions(mode: TravelMode, points: List<GeoPoint>): OrsDirectionsResponse {
        require(mode != TravelMode.TRANSIT) { "ORS does not provide transit routing" }
        require(points.size >= 2) { "ORS directions requires at least 2 coordinates" }
        return post(
            path = "directions/${mode.orsProfile()}/geojson",
            body = json.encodeToString(
                OrsDirectionsRequest.serializer(),
                OrsDirectionsRequest(
                    coordinates = points.map(GeoPoint::toGeoJsonPosition),
                    instructions = true,
                    language = "zh-cn",
                ),
            ),
            deserializer = OrsDirectionsResponse.serializer(),
        )
    }

    private suspend fun <T> post(
        path: String,
        body: String,
        deserializer: kotlinx.serialization.DeserializationStrategy<T>,
    ): T {
        val key = apiKey()?.takeIf(String::isNotBlank) ?: throw ApiException.MissingOrsKey()
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/$path")
            .header("Authorization", key)
            .post(body.toRequestBody(JSON_MEDIA_TYPE))
            .build()
        return httpClient.execute(request, deserializer)
    }

    companion object {
        private const val BASE_URL = "https://api.heigit.org/openrouteservice/v2"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

private fun TravelMode.orsProfile(): String = when (this) {
    TravelMode.DRIVE -> "driving-car"
    TravelMode.BIKE -> "cycling-regular"
    TravelMode.WALK -> "foot-walking"
    TravelMode.TRANSIT -> error("Transit has no ORS profile")
}

@Serializable
data class OrsMatrixRequest(
    val locations: List<List<Double>>,
    val metrics: List<String>,
    val units: String,
)

@Serializable
data class OrsMatrixResponse(
    val durations: List<List<Double?>> = emptyList(),
    val distances: List<List<Double?>> = emptyList(),
)

@Serializable
data class OrsDirectionsRequest(
    val coordinates: List<List<Double>>,
    val instructions: Boolean,
    val language: String,
) 

@Serializable
data class OrsDirectionsResponse(
    val features: List<OrsFeatureDto> = emptyList(),
)

@Serializable
data class OrsFeatureDto(
    val geometry: OrsGeometryDto,
    val properties: OrsPropertiesDto,
)

@Serializable
data class OrsGeometryDto(
    val coordinates: List<List<Double>> = emptyList(),
)

@Serializable
data class OrsPropertiesDto(
    val segments: List<OrsSegmentDto> = emptyList(),
)

@Serializable
data class OrsSegmentDto(
    val distance: Double = 0.0,
    val duration: Double = 0.0,
    val steps: List<OrsStepDto> = emptyList(),
)

@Serializable
data class OrsStepDto(
    val distance: Double = 0.0,
    val duration: Double = 0.0,
    val instruction: String = "",
    @SerialName("way_points") val wayPoints: List<Int> = emptyList(),
)
