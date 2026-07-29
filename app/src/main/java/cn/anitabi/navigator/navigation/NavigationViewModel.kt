package cn.anitabi.navigator.navigation

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.anitabi.navigator.core.model.NavigationState
import cn.anitabi.navigator.core.model.TourPlan
import cn.anitabi.navigator.data.repository.TourRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NavigationViewModel(
    application: Application,
    private val repository: TourRepository,
) : AndroidViewModel(application) {
    val state: StateFlow<NavigationRuntimeState> = NavigationRuntime.state

    init {
        viewModelScope.launch {
            val saved = repository.getMostRecent() ?: return@launch
            val progress = saved.progress ?: return@launch
            if (progress.state !in setOf(NavigationState.PLANNED, NavigationState.COMPLETED) &&
                !NavigationRuntime.state.value.isRunning &&
                AndroidLocationProvider.hasLocationPermission(getApplication())
            ) {
                start(saved.plan)
            }
        }
    }

    fun start(plan: TourPlan) {
        val intent = Intent(getApplication(), NavigationService::class.java)
            .setAction(NavigationService.ACTION_START)
            .putExtra(NavigationService.EXTRA_TOUR_ID, plan.id)
        ContextCompat.startForegroundService(getApplication(), intent)
    }

    fun stop() {
        getApplication<Application>().startService(
            Intent(getApplication(), NavigationService::class.java).setAction(NavigationService.ACTION_STOP),
        )
    }

    fun markArrived() {
        getApplication<Application>().startService(
            Intent(getApplication(), NavigationService::class.java).setAction(NavigationService.ACTION_MANUAL_ARRIVAL),
        )
    }

    fun refreshTransit() {
        getApplication<Application>().startService(
            Intent(getApplication(), NavigationService::class.java).setAction(
                NavigationService.ACTION_REFRESH_TRANSIT,
            ),
        )
    }

    class Factory(
        private val application: Application,
        private val repository: TourRepository,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            NavigationViewModel(application, repository) as T
    }
}
