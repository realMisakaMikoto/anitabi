package cn.anitabi.navigator

import android.content.Context
import cn.anitabi.navigator.data.local.AnitabiDatabase
import cn.anitabi.navigator.data.auth.FirebaseAnonymousTokenProvider
import cn.anitabi.navigator.data.network.ApiHttpClient
import cn.anitabi.navigator.data.network.UserAgentInterceptor
import cn.anitabi.navigator.data.network.anitabi.AnitabiApi
import cn.anitabi.navigator.data.network.backend.BackendApi
import cn.anitabi.navigator.data.network.bangumi.BangumiApi
import cn.anitabi.navigator.data.repository.PilgrimageRepository
import cn.anitabi.navigator.data.repository.TourRepository
import cn.anitabi.navigator.core.routing.BackendRoadRoutingProvider
import cn.anitabi.navigator.core.routing.BackendTransitJourneyProvider
import cn.anitabi.navigator.core.routing.TourPlanner
import cn.anitabi.navigator.security.AppSettingsStore
import cn.anitabi.navigator.navigation.AndroidLocationProvider

class AppContainer(context: Context) {
    private val json = ApiHttpClient.defaultJson
    private val database = AnitabiDatabase.create(context)
    val appSettingsStore = AppSettingsStore(context)
    val locationProvider = AndroidLocationProvider(context)
    private val httpClient = ApiHttpClient(
        userAgentInterceptor = createAppUserAgentInterceptor(),
        json = json,
    )

    val bangumiApi = BangumiApi(httpClient, json)
    val pilgrimageRepository = PilgrimageRepository(
        api = AnitabiApi(httpClient),
        cacheDao = database.pilgrimageCacheDao(),
        json = json,
    )
    val tourRepository = TourRepository(database.tourPlanDao(), json)
    private val backendApi = BackendApi(
        httpClient = httpClient,
        tokenProvider = FirebaseAnonymousTokenProvider(),
        json = json,
    )
    val tourPlanner = TourPlanner(
        roadProvider = BackendRoadRoutingProvider(backendApi),
        transitProvider = BackendTransitJourneyProvider(backendApi),
    )

    companion object {
        const val PROJECT_CONTACT = "https://github.com/realMisakaMikoto"
    }
}

internal fun createAppUserAgentInterceptor(): UserAgentInterceptor = UserAgentInterceptor(
    appName = "AnitabiNavigator",
    appVersion = BuildConfig.VERSION_NAME,
    contact = AppContainer.PROJECT_CONTACT,
)
