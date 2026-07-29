package cn.anitabi.navigator.navigation

import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.NavigationProgress
import cn.anitabi.navigator.core.model.TourPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class NavigationRuntimeState(
    val plan: TourPlan? = null,
    val progress: NavigationProgress? = null,
    val currentLocation: GeoPoint? = null,
    val instruction: String = "准备开始巡礼",
    val remainingDistanceMeters: Double = 0.0,
    val isRunning: Boolean = false,
    val isRerouting: Boolean = false,
    val errorMessage: String? = null,
)

object NavigationRuntime {
    private val mutableState = MutableStateFlow(NavigationRuntimeState())
    val state: StateFlow<NavigationRuntimeState> = mutableState.asStateFlow()

    fun set(value: NavigationRuntimeState) {
        mutableState.value = value
    }

    fun update(transform: (NavigationRuntimeState) -> NavigationRuntimeState) {
        mutableState.update(transform)
    }
}
