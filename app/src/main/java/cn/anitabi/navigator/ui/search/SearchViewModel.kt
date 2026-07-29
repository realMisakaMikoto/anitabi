package cn.anitabi.navigator.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.anitabi.navigator.core.model.Anime
import cn.anitabi.navigator.core.model.PilgrimagePoint
import cn.anitabi.navigator.data.network.ApiException
import cn.anitabi.navigator.data.network.bangumi.BangumiApi
import cn.anitabi.navigator.data.repository.PilgrimageData
import cn.anitabi.navigator.data.repository.PilgrimageRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel(
    private val bangumiApi: BangumiApi,
    private val pilgrimageRepository: PilgrimageRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = mutableState.asStateFlow()

    fun updateQuery(query: String) {
        mutableState.update { it.copy(query = query, errorMessage = null) }
    }

    fun search() {
        val keyword = state.value.query.trim()
        if (keyword.isEmpty()) {
            mutableState.update { it.copy(errorMessage = "请输入动漫名称") }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { bangumiApi.searchAnime(keyword) }
                .onSuccess { results ->
                    mutableState.update {
                        it.copy(searchResults = results, isLoading = false, selectedAnime = null, pilgrimageData = null)
                    }
                }
                .onFailure(::handleFailure)
        }
    }

    fun openAnime(anime: Anime) {
        viewModelScope.launch {
            mutableState.update {
                it.copy(isLoading = true, errorMessage = null, selectedAnime = anime, selectedPointIds = emptySet())
            }
            runCatching { pilgrimageRepository.load(anime.subjectId) }
                .onSuccess { data ->
                    mutableState.update { it.copy(pilgrimageData = data, isLoading = false, showList = false) }
                }
                .onFailure(::handleFailure)
        }
    }

    fun backToResults() {
        mutableState.update {
            it.copy(selectedAnime = null, pilgrimageData = null, selectedPointIds = emptySet(), errorMessage = null)
        }
    }

    fun togglePoint(pointId: String) {
        mutableState.update { current ->
            val selected = current.selectedPointIds
            when {
                pointId in selected -> current.copy(selectedPointIds = selected - pointId, errorMessage = null)
                selected.size >= MAX_ROAD_POINTS -> current.copy(errorMessage = "道路路线最多选择 $MAX_ROAD_POINTS 个巡礼点")
                else -> current.copy(selectedPointIds = selected + pointId, errorMessage = null)
            }
        }
    }

    fun updateVisibleBounds(bounds: GeoBounds) {
        mutableState.update { it.copy(visibleBounds = bounds) }
    }

    fun selectVisiblePoints() {
        mutableState.update { current ->
            val visible = current.pilgrimageData?.points.orEmpty()
                .filter { point -> current.visibleBounds?.contains(point) == true }
                .map(PilgrimagePoint::id)
            val available = visible.filterNot(current.selectedPointIds::contains)
            val room = MAX_ROAD_POINTS - current.selectedPointIds.size
            val added = available.take(room)
            current.copy(
                selectedPointIds = current.selectedPointIds + added,
                errorMessage = if (added.size < available.size) "已选满 $MAX_ROAD_POINTS 个点，其余未加入" else null,
            )
        }
    }

    fun clearSelection() {
        mutableState.update { it.copy(selectedPointIds = emptySet(), errorMessage = null) }
    }

    fun setShowList(showList: Boolean) {
        mutableState.update { it.copy(showList = showList) }
    }

    fun openPlanner() {
        mutableState.update { current ->
            if (current.selectedPointIds.size < 2) {
                current.copy(errorMessage = "至少选择 2 个巡礼点才能规划路线")
            } else {
                current.copy(plannerOpen = true, errorMessage = null)
            }
        }
    }

    fun closePlanner() {
        mutableState.update { it.copy(plannerOpen = false, errorMessage = null) }
    }

    fun openNavigation() {
        mutableState.update { it.copy(navigationOpen = true, hiddenNavigationTourId = null) }
    }

    fun closeNavigation(tourId: String?) {
        mutableState.update {
            it.copy(navigationOpen = false, hiddenNavigationTourId = tourId)
        }
    }

    fun openAbout() {
        mutableState.update { it.copy(aboutOpen = true) }
    }

    fun closeAbout() {
        mutableState.update { it.copy(aboutOpen = false) }
    }

    private fun handleFailure(throwable: Throwable) {
        if (throwable is CancellationException) throw throwable
        val message = when (throwable) {
            is ApiException.NotFound -> "Anitabi 暂无这部作品的巡礼数据"
            is ApiException.RateLimited -> "请求过于频繁，请稍后再试"
            is ApiException.Forbidden -> "当前公网 IP 被公共服务拒绝，请更换网络后重试"
            is ApiException.Network -> "网络不可用，可查看已缓存作品"
            is ApiException.Server -> "公共服务暂时不可用，请稍后再试"
            else -> throwable.message ?: "加载失败，请稍后再试"
        }
        mutableState.update { it.copy(isLoading = false, errorMessage = message) }
    }

    class Factory(
        private val bangumiApi: BangumiApi,
        private val pilgrimageRepository: PilgrimageRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SearchViewModel(bangumiApi, pilgrimageRepository) as T
        }
    }

    companion object {
        const val MAX_ROAD_POINTS = 12
    }
}

data class SearchUiState(
    val query: String = "",
    val searchResults: List<Anime> = emptyList(),
    val selectedAnime: Anime? = null,
    val pilgrimageData: PilgrimageData? = null,
    val selectedPointIds: Set<String> = emptySet(),
    val visibleBounds: GeoBounds? = null,
    val isLoading: Boolean = false,
    val showList: Boolean = false,
    val plannerOpen: Boolean = false,
    val navigationOpen: Boolean = false,
    val hiddenNavigationTourId: String? = null,
    val aboutOpen: Boolean = false,
    val errorMessage: String? = null,
)
