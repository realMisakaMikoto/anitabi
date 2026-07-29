package cn.anitabi.navigator.data.network.transit

import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.data.network.ApiHttpClient
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request

class TransitousApi(
    private val httpClient: ApiHttpClient,
    private val planUrl: String = PLAN_URL,
) {
    suspend fun plan(
        from: GeoPoint,
        to: GeoPoint,
        departureTime: String,
    ): TransitPlanDto {
        val url = planUrl.toHttpUrl().newBuilder()
            .addQueryParameter("fromPlace", from.toTransitPlace())
            .addQueryParameter("toPlace", to.toTransitPlace())
            .addQueryParameter("time", departureTime)
            .addQueryParameter("transitModes", "TRANSIT")
            .addQueryParameter("directModes", "")
            .addQueryParameter("detailedLegs", "true")
            .build()
        return httpClient.execute(
            Request.Builder().url(url).get().build(),
            TransitPlanDto.serializer(),
        )
    }

    companion object {
        internal const val PLAN_URL = "https://api.transitous.org/api/v6/plan"
    }
}

private fun GeoPoint.toTransitPlace(): String = "$latitude,$longitude"

@Serializable
data class TransitPlanDto(
    val itineraries: List<TransitItineraryDto> = emptyList(),
    val direct: List<TransitItineraryDto> = emptyList(),
    val previousPageCursor: String? = null,
    val nextPageCursor: String? = null,
)

@Serializable
data class TransitItineraryDto(
    val id: String,
    val duration: Long,
    val startTime: String,
    val endTime: String,
    val transfers: Int,
    val legs: List<TransitLegDto> = emptyList(),
)

@Serializable
data class TransitLegDto(
    val mode: String,
    val from: TransitPlaceDto,
    val to: TransitPlaceDto,
    val duration: Long,
    val startTime: String,
    val endTime: String,
    val distance: Double? = null,
    val headsign: String? = null,
    val routeShortName: String? = null,
    val routeLongName: String? = null,
    val agencyName: String? = null,
    val intermediateStops: List<TransitPlaceDto> = emptyList(),
    val legGeometry: TransitPolylineDto,
    val realTime: Boolean = false,
    val cancelled: Boolean = false,
)

@Serializable
data class TransitPlaceDto(
    val name: String,
    val lat: Double,
    val lon: Double,
    val stopId: String? = null,
    val arrival: String? = null,
    val departure: String? = null,
    val track: String? = null,
    val scheduledTrack: String? = null,
)

@Serializable
data class TransitPolylineDto(
    val points: String = "",
    val length: Int = 0,
)
