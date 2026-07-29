package cn.anitabi.navigator.data.network.bangumi

import cn.anitabi.navigator.core.model.Anime
import cn.anitabi.navigator.data.network.ApiHttpClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class BangumiApi(
    private val httpClient: ApiHttpClient,
    private val json: Json = ApiHttpClient.defaultJson,
) {
    suspend fun searchAnime(keyword: String, limit: Int = 20): List<Anime> {
        require(keyword.isNotBlank()) { "Search keyword cannot be blank" }
        require(limit in 1..50) { "Search limit must be between 1 and 50" }

        val payload = BangumiSearchRequest(
            keyword = keyword.trim(),
            filter = BangumiSearchFilter(type = listOf(ANIME_SUBJECT_TYPE)),
        )
        val request = Request.Builder()
            .url("$SEARCH_URL?limit=$limit&offset=0")
            .post(
                json.encodeToString(BangumiSearchRequest.serializer(), payload)
                    .toRequestBody(JSON_MEDIA_TYPE),
            )
            .build()

        return httpClient.execute(request, BangumiSearchResponse.serializer()).data.map { it.toAnime() }
    }

    companion object {
        private const val SEARCH_URL = "https://api.bgm.tv/v0/search/subjects"
        private const val ANIME_SUBJECT_TYPE = 2
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

@Serializable
data class BangumiSearchRequest(
    val keyword: String,
    val sort: String = "match",
    val filter: BangumiSearchFilter,
)

@Serializable
data class BangumiSearchFilter(val type: List<Int>)

@Serializable
data class BangumiSearchResponse(
    val total: Int = 0,
    val data: List<BangumiSubjectDto> = emptyList(),
)

@Serializable
data class BangumiSubjectDto(
    val id: Long,
    val name: String,
    @SerialName("name_cn") val nameCn: String = "",
    val images: BangumiImagesDto? = null,
) {
    fun toAnime(): Anime = Anime(
        subjectId = id,
        name = name,
        nameCn = nameCn.takeIf(String::isNotBlank),
        imageUrl = images?.common ?: images?.medium ?: images?.grid,
    )
}

@Serializable
data class BangumiImagesDto(
    val large: String? = null,
    val common: String? = null,
    val medium: String? = null,
    val small: String? = null,
    val grid: String? = null,
)
