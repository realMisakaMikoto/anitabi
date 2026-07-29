package cn.anitabi.navigator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import cn.anitabi.navigator.ui.search.SearchRoute
import cn.anitabi.navigator.ui.search.SearchViewModel
import cn.anitabi.navigator.ui.planner.PlannerViewModel
import cn.anitabi.navigator.navigation.NavigationViewModel
import cn.anitabi.navigator.ui.theme.AnitabiTheme

class MainActivity : ComponentActivity() {
    private val container by lazy { (application as AnitabiApplication).container }
    private val searchViewModel by viewModels<SearchViewModel> {
        SearchViewModel.Factory(container.bangumiApi, container.pilgrimageRepository)
    }
    private val plannerViewModel by viewModels<PlannerViewModel> {
        PlannerViewModel.Factory(
            planner = container.tourPlanner,
            repository = container.tourRepository,
            keyStore = container.orsKeyStore,
            locationProvider = container.locationProvider,
        )
    }
    private val navigationViewModel by viewModels<NavigationViewModel> {
        NavigationViewModel.Factory(application, container.tourRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AnitabiTheme {
                SearchRoute(
                    viewModel = searchViewModel,
                    plannerViewModel = plannerViewModel,
                    navigationViewModel = navigationViewModel,
                )
            }
        }
    }
}
