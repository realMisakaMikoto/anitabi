package cn.anitabi.navigator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.Modifier
import cn.anitabi.navigator.ui.search.SearchRoute
import cn.anitabi.navigator.ui.search.SearchViewModel
import cn.anitabi.navigator.ui.planner.PlannerViewModel
import cn.anitabi.navigator.navigation.NavigationViewModel
import cn.anitabi.navigator.ui.theme.AnitabiTheme
import cn.anitabi.navigator.ui.theme.Paper

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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Paper)
                        .navigationBarsPadding(),
                ) {
                    SearchRoute(
                        viewModel = searchViewModel,
                        plannerViewModel = plannerViewModel,
                        navigationViewModel = navigationViewModel,
                    )
                }
            }
        }
    }
}
