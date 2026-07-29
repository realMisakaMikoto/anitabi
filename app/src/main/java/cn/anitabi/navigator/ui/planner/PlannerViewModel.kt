package cn.anitabi.navigator.ui.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.anitabi.navigator.core.model.Anime
import cn.anitabi.navigator.core.model.EndPolicy
import cn.anitabi.navigator.core.model.PilgrimagePoint
import cn.anitabi.navigator.core.model.RouteObjective
import cn.anitabi.navigator.core.model.TourPlan
import cn.anitabi.navigator.core.model.TravelMode
import cn.anitabi.navigator.core.routing.NoRouteException
import cn.anitabi.navigator.core.routing.NoTransitDataException
import cn.anitabi.navigator.core.routing.RoadPlanRequest
import cn.anitabi.navigator.core.routing.TourPlanner
import cn.anitabi.navigator.core.routing.TransitPlanRequest
import cn.anitabi.navigator.data.network.ApiException
import cn.anitabi.navigator.data.repository.TourRepository
import cn.anitabi.navigator.security.OrsKeyStore
import cn.anitabi.navigator.navigation.CurrentLocationProvider
import cn.anitabi.navigator.navigation.LocationUnavailableException
import cn.anitabi.navigator.navigation.MissingLocationPermissionException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlannerViewModel(
    private val planner: TourPlanner,
    private val repository: TourRepository,
    private val keyStore: OrsKeyStore,
    private val locationProvider: CurrentLocationProvider,
) : ViewModel() {
    private val mutableState = MutableStateFlow(PlannerUiState())
    val state: StateFlow<PlannerUiState> = mutableState.asStateFlow()

    fun configure(anime: Anime, points: List<PilgrimagePoint>) {
        require(points.size >= 2)
        val now = LocalDateTime.now().withSecond(0).withNano(0)
        mutableState.value = PlannerUiState(
            anime = anime,
            selectedPoints = points,
            startPointId = points.first().id,
            fixedEndPointId = points.last().id,
            departureDate = now.toLocalDate().toString(),
            departureTime = now.toLocalTime().toString(),
            hasStoredOrsKey = keyStore.hasKey(),
        )
    }

    fun setMode(mode: TravelMode) {
        mutableState.update { current ->
            when {
                mode == TravelMode.TRANSIT && current.selectedPoints.size > TourPlanner.MAX_TRANSIT_POINTS -> current.copy(
                    errorMessage = "公交最多选择 ${TourPlanner.MAX_TRANSIT_POINTS} 个巡礼点",
                )
                else -> current.copy(mode = mode, errorMessage = null, plan = null)
            }
        }
    }

    fun setObjective(objective: RouteObjective) {
        mutableState.update { it.copy(objective = objective, plan = null) }
    }

    fun setEndPolicy(policy: EndPolicy) {
        mutableState.update { it.copy(endPolicy = policy, plan = null) }
    }

    fun setStartPoint(pointId: String) {
        mutableState.update { current ->
            val fixedEnd = if (current.fixedEndPointId == pointId) {
                current.selectedPoints.lastOrNull { it.id != pointId }?.id
            } else {
                current.fixedEndPointId
            }
            current.copy(
                startPointId = pointId,
                fixedEndPointId = fixedEnd,
                useCurrentLocation = false,
                plan = null,
            )
        }
    }

    fun setUseCurrentLocation() {
        mutableState.update { it.copy(useCurrentLocation = true, startPointId = null, plan = null, errorMessage = null) }
    }

    fun locationPermissionDenied() {
        mutableState.update { it.copy(errorMessage = "需要定位权限才能从当前位置出发") }
    }

    fun navigationPermissionDenied(message: String) {
        mutableState.update { it.copy(errorMessage = message) }
    }

    fun setFixedEndPoint(pointId: String) {
        mutableState.update { it.copy(fixedEndPointId = pointId, plan = null) }
    }

    fun setDepartureDate(value: String) {
        mutableState.update { it.copy(departureDate = value, plan = null) }
    }

    fun setDepartureTime(value: String) {
        mutableState.update { it.copy(departureTime = value, plan = null) }
    }

    fun setDwellMinutes(value: String) {
        mutableState.update { it.copy(dwellMinutesInput = value.filter(Char::isDigit).take(3), plan = null) }
    }

    fun setOrsKey(value: String) {
        mutableState.update { it.copy(orsKeyInput = value.trim(), errorMessage = null) }
    }

    fun generate() {
        val current = state.value
        val anime = current.anime ?: return
        viewModelScope.launch {
            mutableState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val startPoint = current.startPointId?.let { id -> current.selectedPoints.single { it.id == id } }
                val startCoordinate = if (current.useCurrentLocation) {
                    locationProvider.currentLocation()
                } else {
                    requireNotNull(startPoint).coordinate
                }
                if (current.mode != TravelMode.TRANSIT && current.orsKeyInput.isNotBlank()) {
                    keyStore.save(current.orsKeyInput)
                }
                val plan = if (current.mode == TravelMode.TRANSIT) {
                    val departure = LocalDateTime.of(
                        LocalDate.parse(current.departureDate),
                        LocalTime.parse(current.departureTime),
                    ).atZone(ZoneId.systemDefault()).toOffsetDateTime().toString()
                    planner.planTransit(
                        TransitPlanRequest(
                            anime = anime,
                            selectedPoints = current.selectedPoints,
                            start = startCoordinate,
                            startPointId = startPoint?.id,
                            endPolicy = current.endPolicy,
                            fixedEndPointId = current.fixedEndPointId,
                            departureTime = departure,
                            dwellMinutes = current.dwellMinutesInput.toIntOrNull() ?: 15,
                        ),
                    )
                } else {
                    planner.planRoad(
                        RoadPlanRequest(
                            anime = anime,
                            selectedPoints = current.selectedPoints,
                            start = startCoordinate,
                            startPointId = startPoint?.id,
                            mode = current.mode,
                            objective = current.objective,
                            endPolicy = current.endPolicy,
                            fixedEndPointId = current.fixedEndPointId,
                        ),
                    )
                }
                repository.save(plan)
                mutableState.update {
                    it.copy(
                        plan = plan,
                        draftOrder = plan.orderedPoints,
                        isLoading = false,
                        hasStoredOrsKey = keyStore.hasKey(),
                        orsKeyInput = "",
                    )
                }
            } catch (exception: Exception) {
                handleFailure(exception)
            }
        }
    }

    fun moveDraft(fromIndex: Int, toIndex: Int) {
        mutableState.update { current ->
            val order = current.draftOrder.toMutableList()
            if (!current.canMove(fromIndex, toIndex)) return@update current
            order.add(toIndex, order.removeAt(fromIndex))
            current.copy(draftOrder = order, orderChanged = true)
        }
    }

    fun applyManualOrder() {
        val current = state.value
        val plan = current.plan ?: return
        viewModelScope.launch {
            mutableState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { planner.rebuild(plan, current.draftOrder) }
                .onSuccess { updated ->
                    repository.save(updated)
                    mutableState.update {
                        it.copy(plan = updated, draftOrder = updated.orderedPoints, orderChanged = false, isLoading = false)
                    }
                }
                .onFailure(::handleFailure)
        }
    }

    fun clearPlan() {
        mutableState.update { it.copy(plan = null, draftOrder = emptyList(), orderChanged = false, errorMessage = null) }
    }

    private fun PlannerUiState.canMove(fromIndex: Int, toIndex: Int): Boolean {
        if (fromIndex !in draftOrder.indices || toIndex !in draftOrder.indices) return false
        val startLocked = draftOrder.firstOrNull()?.id == startPointId
        if (startLocked && (fromIndex == 0 || toIndex == 0)) return false
        if (endPolicy == EndPolicy.FIXED && (fromIndex == draftOrder.lastIndex || toIndex == draftOrder.lastIndex)) {
            return false
        }
        return true
    }

    private fun handleFailure(throwable: Throwable) {
        if (throwable is CancellationException) throw throwable
        val isTransit = state.value.mode == TravelMode.TRANSIT
        val message = when (throwable) {
            is ApiException.MissingOrsKey -> "请先填写自己的免费 ORS Key"
            is ApiException.InvalidCredentials -> "ORS Key 无效或已失效，请重新填写"
            is ApiException.Forbidden -> if (isTransit) {
                "Transitous 拒绝了当前网络的访问，请停止重试并更换网络"
            } else {
                "ORS Key 无效或已失效，请重新填写"
            }
            is ApiException.RateLimited -> if (isTransit) {
                "Transitous 请求过于频繁，请稍后再试"
            } else {
                "今日 ORS 配额已用尽，请明天再试"
            }
            is ApiException.NotFound -> if (isTransit) {
                "Transitous 未找到可用行程"
            } else {
                "ORS 未找到可用路线"
            }
            is ApiException.Server -> if (isTransit) {
                "Transitous 服务暂时不可用，请稍后再试"
            } else {
                "ORS 服务暂时不可用，请稍后再试"
            }
            is ApiException.InvalidResponse -> if (isTransit) {
                "Transitous 返回了无法识别的数据"
            } else {
                "ORS 返回了无法识别的数据"
            }
            is ApiException.Http -> if (isTransit) {
                "Transitous 请求失败，请稍后再试"
            } else {
                "ORS 请求失败，请稍后再试"
            }
            is ApiException.Network -> "网络连接失败，请检查网络后重试"
            is NoTransitDataException -> "本区域暂无开放公交数据"
            is NoRouteException -> "所选地点之间存在不可达路段"
            is MissingLocationPermissionException -> "需要定位权限才能从当前位置出发"
            is LocationUnavailableException -> "暂时无法取得当前位置，请检查系统定位开关"
            is java.time.format.DateTimeParseException -> "日期或时间格式不正确"
            else -> throwable.message ?: "路线生成失败"
        }
        mutableState.update { it.copy(isLoading = false, errorMessage = message) }
    }

    class Factory(
        private val planner: TourPlanner,
        private val repository: TourRepository,
        private val keyStore: OrsKeyStore,
        private val locationProvider: CurrentLocationProvider,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PlannerViewModel(planner, repository, keyStore, locationProvider) as T
    }
}

data class PlannerUiState(
    val anime: Anime? = null,
    val selectedPoints: List<PilgrimagePoint> = emptyList(),
    val mode: TravelMode = TravelMode.WALK,
    val objective: RouteObjective = RouteObjective.FASTEST,
    val endPolicy: EndPolicy = EndPolicy.OPEN,
    val startPointId: String? = null,
    val useCurrentLocation: Boolean = false,
    val fixedEndPointId: String? = null,
    val departureDate: String = "",
    val departureTime: String = "",
    val dwellMinutesInput: String = "15",
    val orsKeyInput: String = "",
    val hasStoredOrsKey: Boolean = false,
    val plan: TourPlan? = null,
    val draftOrder: List<PilgrimagePoint> = emptyList(),
    val orderChanged: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
