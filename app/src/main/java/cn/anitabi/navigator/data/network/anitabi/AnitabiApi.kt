package cn.anitabi.navigator.data.network.anitabi

import cn.anitabi.navigator.core.model.Anime
import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.PilgrimagePoint
import cn.anitabi.navigator.data.network.ApiHttpClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request

class AnitabiApi(private val httpClient: ApiHttpClient) {
    suspend fun getLite(subjectId: Long): AnitabiLiteDto = httpClient.execute(
        Request.Builder().url("$BASE_URL/bangumi/$subjectId/lite").get().build(),
        AnitabiLiteDto.serializer(),
    )

    suspend fun getPointDetails(subjectId: Long): List<AnitabiPointDto> = httpClient.execute(
        Request.Builder().url("$BASE_URL/bangumi/$subjectId/points/detail").get().build(),
        AnitabiPointListSerializer,
    )

    companion object {
        private const val BASE_URL = "https://api.anitabi.cn"
    }
}

private object AnitabiPointListSerializer : kotlinx.serialization.KSerializer<List<AnitabiPointDto>> by
    kotlinx.serialization.builtins.ListSerializer(AnitabiPointDto.serializer())

@Serializable
data class AnitabiLiteDto(
    val id: Long,
    val cn: String? = null,
    val title: String? = null,
    val city: String? = null,
    val cover: String? = null,
    val geo: List<Double> = emptyList(),
    val zoom: Double? = null,
    val modified: Long? = null,
    val pointsLength: Int = 0,
) {
    fun toAnime(): Anime = Anime(
        subjectId = id,
        name = title?.takeIf(String::isNotBlank) ?: cn?.takeIf(String::isNotBlank) ?: "Bangumi $id",
        nameCn = cn?.takeIf(String::isNotBlank),
        imageUrl = cover.toAllowedAnitabiImageUrlOrNull(),
    )
}

@Serializable
data class AnitabiPointDto(
    val id: String,
    val name: String? = null,
    val cn: String? = null,
    val image: String? = null,
    val geo: List<Double> = emptyList(),
    val origin: String? = null,
    @SerialName("originURL") val originUrl: String? = null,
) {
    fun toPilgrimagePointOrNull(): PilgrimagePoint? {
        val coordinate = runCatching { GeoPoint.fromAnitabiGeo(geo) }.getOrNull() ?: return null
        return PilgrimagePoint(
            id = id,
            name = cn?.takeIf(String::isNotBlank)
                ?: name?.takeIf(String::isNotBlank)
                ?: "未命名地点",
            coordinate = coordinate,
            imageUrl = image.toAllowedAnitabiImageUrlOrNull(),
            origin = origin?.takeIf(String::isNotBlank),
            originUrl = originUrl?.takeIf(String::isSafeWebUrl),
        )
    }
}

private fun String.isSafeWebUrl(): Boolean =
    startsWith("https://", ignoreCase = true) || startsWith("http://", ignoreCase = true)

private fun String?.toAllowedAnitabiImageUrlOrNull(): String? {
    val url = this?.toHttpUrlOrNull() ?: return null
    return takeIf { url.scheme == "https" && url.host == ANITABI_IMAGE_HOST }
}

private const val ANITABI_IMAGE_HOST = "image.anitabi.cn"
