package cn.anitabi.navigator.data.network.backend

import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.RouteObjective
import cn.anitabi.navigator.core.model.TransitRoutingPreference
import cn.anitabi.navigator.core.model.TransitTravelMode
import cn.anitabi.navigator.core.model.TravelMode
import cn.anitabi.navigator.data.auth.IdTokenProvider
import cn.anitabi.navigator.data.network.ApiException
import cn.anitabi.navigator.data.network.ApiHttpClient
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class BackendApi(
    private val httpClient: ApiHttpClient,
    private val tokenProvider: IdTokenProvider,
    private val json: Json = ApiHttpClient.defaultJson,
    private val baseUrl: String = BASE_URL,
) {
    suspend fun matrix(
        mode: TravelMode,
        coordinates: List<GeoPoint>,
        objective: RouteObjective,
        departureTime: String? = null,
    ): BackendMatrixResponse {
        require(mode != TravelMode.TRANSIT) { "Transit has no matrix request" }
        require(coordinates.size in 2..10) { "Matrix requires 2 to 10 coordinates" }
        return post(
            path = "v1/matrix",
            body = BackendMatrixRequest(
                mode = mode.backendName(),
                coordinates = coordinates.map(GeoPoint::toBackendCoordinate),
                departureTime = departureTime,
                objective = objective.name,
            ),
            serializer = BackendMatrixRequest.serializer(),
            deserializer = BackendMatrixResponse.serializer(),
        )
    }

    suspend fun route(
        mode: TravelMode,
        locations: List<GeoPoint>,
        departureTime: String? = null,
        arrivalTime: String? = null,
        transitRoutingPreference: TransitRoutingPreference = TransitRoutingPreference.RECOMMENDED,
        transitTravelModes: Set<TransitTravelMode> = emptySet(),
    ): BackendRouteResponse {
        require(locations.size in 2..12) { "Route requires 2 to 12 locations" }
        require(mode != TravelMode.TRANSIT || locations.size == 2) {
            "Transit routes require exactly two locations"
        }
        require(departureTime == null || arrivalTime == null) {
            "A route cannot specify both departure and arrival time"
        }
        require(
            mode == TravelMode.TRANSIT ||
                (departureTime == null && arrivalTime == null &&
                    transitRoutingPreference == TransitRoutingPreference.RECOMMENDED &&
                    transitTravelModes.isEmpty()),
        ) {
            "Transit time and routing preferences require transit mode"
        }
        return post(
            path = "v1/route",
            body = BackendRouteRequest(
                mode = mode.backendName(),
                locations = locations.map(GeoPoint::toBackendCoordinate),
                departureTime = departureTime,
                arrivalTime = arrivalTime,
                transitRoutingPreference = transitRoutingPreference
                    .takeUnless { it == TransitRoutingPreference.RECOMMENDED }
                    ?.name,
                transitTravelModes = transitTravelModes
                    .sortedBy(TransitTravelMode::ordinal)
                    .map(TransitTravelMode::name)
                    .takeIf(List<String>::isNotEmpty),
            ),
            serializer = BackendRouteRequest.serializer(),
            deserializer = BackendRouteResponse.serializer(),
        )
    }

    suspend fun reserveNavigation(destinationCount: Int): BackendNavigationReservation {
        require(destinationCount in 1..25) { "Navigation reservation requires 1 to 25 destinations" }
        return post(
            path = "v1/navigation/reserve",
            body = BackendNavigationReservationRequest(destinationCount),
            serializer = BackendNavigationReservationRequest.serializer(),
            deserializer = BackendNavigationReservation.serializer(),
        )
    }

    private suspend fun <RequestBody, ResponseBody> post(
        path: String,
        body: RequestBody,
        serializer: SerializationStrategy<RequestBody>,
        deserializer: DeserializationStrategy<ResponseBody>,
    ): ResponseBody {
        val token = tokenProvider.idToken()
        val request = Request.Builder()
            .url("${baseUrl.trimEnd('/')}/$path")
            .header("Authorization", "Bearer $token")
            .post(json.encodeToString(serializer, body).toRequestBody(JSON_MEDIA_TYPE))
            .build()
        return httpClient.execute(
            request = request,
            deserializer = deserializer,
            errorMapper = ::mapBackendError,
        )
    }

    private fun mapBackendError(status: Int, body: String): ApiException {
        val code = runCatching {
            json.decodeFromString(BackendErrorEnvelope.serializer(), body).error.code
        }.getOrNull()
        return when (code) {
            "UNAUTHENTICATED" -> ApiException.Unauthenticated()
            "INVALID_ARGUMENT" -> ApiException.InvalidArgument()
            "NO_ROUTE" -> ApiException.NoRoute()
            "QUOTA_EXHAUSTED" -> ApiException.QuotaExhausted()
            "RATE_LIMITED" -> ApiException.RateLimited()
            "UPSTREAM_UNAVAILABLE" -> ApiException.UpstreamUnavailable()
            "BACKEND_UNAVAILABLE" -> ApiException.BackendUnavailable()
            else -> when (status) {
                400 -> ApiException.InvalidArgument()
                401 -> ApiException.Unauthenticated()
                404 -> ApiException.UpstreamUnavailable()
                429 -> ApiException.RateLimited()
                503 -> ApiException.BackendUnavailable()
                else -> ApiException.Http(status)
            }
        }
    }

    companion object {
        internal const val BASE_URL = "https://api.anitabi.afunnypersonlol0.site"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

private fun TravelMode.backendName(): String = when (this) {
    TravelMode.DRIVE -> "DRIVE"
    TravelMode.BIKE -> "BICYCLE"
    TravelMode.WALK -> "WALK"
    TravelMode.TRANSIT -> "TRANSIT"
}

private fun GeoPoint.toBackendCoordinate() = BackendCoordinate(latitude, longitude)

@Serializable
data class BackendCoordinate(
    val latitude: Double,
    val longitude: Double,
)

@Serializable
data class BackendMatrixRequest(
    val mode: String,
    val coordinates: List<BackendCoordinate>,
    val departureTime: String? = null,
    val objective: String,
)

@Serializable
data class BackendRouteRequest(
    val mode: String,
    val locations: List<BackendCoordinate>,
    val departureTime: String? = null,
    val arrivalTime: String? = null,
    val transitRoutingPreference: String? = null,
    val transitTravelModes: List<String>? = null,
)

@Serializable
data class BackendNavigationReservationRequest(val destinationCount: Int)

@Serializable
data class BackendMatrixResponse(
    val elements: List<BackendMatrixElement> = emptyList(),
)

@Serializable
data class BackendMatrixElement(
    val originIndex: Int,
    val destinationIndex: Int,
    val status: String,
    val distanceMeters: Double? = null,
    val durationSeconds: Double? = null,
)

@Serializable
data class BackendRouteResponse(
    val distanceMeters: Double,
    val durationSeconds: Double,
    val encodedPolyline: String? = null,
    val legs: List<BackendRouteLeg> = emptyList(),
)

@Serializable
data class BackendRouteLeg(
    val distanceMeters: Double,
    val durationSeconds: Double,
    val encodedPolyline: String? = null,
    val steps: List<BackendRouteStep> = emptyList(),
)

@Serializable
data class BackendRouteStep(
    val travelMode: String,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val encodedPolyline: String? = null,
    val instruction: String? = null,
    val maneuver: String? = null,
    val transit: BackendTransitDetails? = null,
)

@Serializable
data class BackendTransitDetails(
    val departureStop: String? = null,
    val arrivalStop: String? = null,
    val departureTime: String? = null,
    val arrivalTime: String? = null,
    val departureTimeZone: String? = null,
    val arrivalTimeZone: String? = null,
    val lineName: String? = null,
    val lineShortName: String? = null,
    val headsign: String? = null,
    val vehicleName: String? = null,
    val vehicleType: String? = null,
    val stopCount: Int? = null,
)

@Serializable
data class BackendNavigationReservation(
    val reservedDestinations: Int,
    val remainingToday: Int,
)

@Serializable
private data class BackendErrorEnvelope(val error: BackendError)

@Serializable
private data class BackendError(val code: String)
